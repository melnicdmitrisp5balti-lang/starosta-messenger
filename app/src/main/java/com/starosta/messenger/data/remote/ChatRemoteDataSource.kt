package com.starosta.messenger.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.starosta.messenger.data.model.Chat
import com.starosta.messenger.data.model.Message
import com.starosta.messenger.data.model.TypingState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val chatsRef = firestore.collection(FirebaseCollections.CHATS)

    // ---- Chat operations ----

    suspend fun createChat(chat: Chat): String {
        val docRef = if (chat.id.isNotEmpty()) {
            chatsRef.document(chat.id)
        } else {
            chatsRef.document()
        }
        val chatWithId = chat.copy(id = docRef.id)
        docRef.set(chatWithId).await()

        // Add to each participant's userChats
        chatWithId.participantIds.forEach { uid ->
            firestore.collection(FirebaseCollections.USER_CHATS)
                .document(uid)
                .collection(FirebaseCollections.ITEMS)
                .document(chatWithId.id)
                .set(mapOf("chatId" to chatWithId.id, "pinnedAt" to 0L))
                .await()
        }
        return docRef.id
    }

    suspend fun getChatById(chatId: String): Chat? {
        return chatsRef.document(chatId).get().await().toObject(Chat::class.java)
    }

    fun observeUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val subscription = chatsRef
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents?.mapNotNull { it.toObject(Chat::class.java) } ?: emptyList()
                trySend(chats)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun pinChat(chatId: String, userId: String) {
        val current = chatsRef.document(chatId).get().await().toObject(Chat::class.java) ?: return
        val pinned = if (userId in current.pinnedBy) {
            current.pinnedBy - userId
        } else {
            current.pinnedBy + userId
        }
        chatsRef.document(chatId).update("pinnedBy", pinned).await()
    }

    suspend fun updateLastMessage(chatId: String, text: String, senderId: String) {
        chatsRef.document(chatId).update(
            mapOf(
                "lastMessageText" to text,
                "lastMessageAt" to System.currentTimeMillis(),
                "lastMessageSenderId" to senderId
            )
        ).await()
    }

    // ---- Message operations ----

    suspend fun sendMessage(message: Message): String {
        val msgRef = if (message.id.isNotEmpty()) {
            chatsRef.document(message.chatId).collection(FirebaseCollections.MESSAGES).document(message.id)
        } else {
            chatsRef.document(message.chatId).collection(FirebaseCollections.MESSAGES).document()
        }
        val msgWithId = message.copy(id = msgRef.id)
        msgRef.set(msgWithId).await()
        updateLastMessage(message.chatId, message.text.ifEmpty { "[${message.type}]" }, message.senderId)
        return msgRef.id
    }

    fun observeMessages(chatId: String, limit: Long = 50): Flow<List<Message>> = callbackFlow {
        val subscription = chatsRef
            .document(chatId)
            .collection(FirebaseCollections.MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) }
                    ?.filter { it.deletedAt == null } ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun loadMoreMessages(chatId: String, beforeTimestamp: Long, limit: Long = 30): List<Message> {
        return chatsRef.document(chatId)
            .collection(FirebaseCollections.MESSAGES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .whereLessThan("createdAt", beforeTimestamp)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Message::class.java) }
            .filter { it.deletedAt == null }
            .reversed()
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        chatsRef.document(chatId)
            .collection(FirebaseCollections.MESSAGES)
            .document(messageId)
            .update(mapOf("text" to newText, "edited" to true))
            .await()
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        chatsRef.document(chatId)
            .collection(FirebaseCollections.MESSAGES)
            .document(messageId)
            .update(mapOf("deletedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun updateMessageStatus(chatId: String, messageId: String, status: String) {
        chatsRef.document(chatId)
            .collection(FirebaseCollections.MESSAGES)
            .document(messageId)
            .update(mapOf("status" to status))
            .await()
    }

    // ---- Typing indicator ----

    suspend fun setTyping(chatId: String, userId: String, isTyping: Boolean) {
        val docId = "${chatId}_${userId}"
        firestore.collection(FirebaseCollections.TYPING)
            .document(docId)
            .set(TypingState(chatId, userId, isTyping, System.currentTimeMillis()))
            .await()
    }

    fun observeTyping(chatId: String, currentUserId: String): Flow<List<String>> = callbackFlow {
        val subscription = firestore.collection(FirebaseCollections.TYPING)
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("isTyping", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val typingUsers = snapshot?.documents
                    ?.mapNotNull { it.toObject(TypingState::class.java) }
                    ?.filter { it.userId != currentUserId }
                    ?.map { it.userId } ?: emptyList()
                trySend(typingUsers)
            }
        awaitClose { subscription.remove() }
    }

    // ---- Users ----

    suspend fun getAllUsers(): List<com.starosta.messenger.data.model.User> {
        return firestore.collection(FirebaseCollections.USERS)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(com.starosta.messenger.data.model.User::class.java) }
    }

    fun observeUser(userId: String): Flow<com.starosta.messenger.data.model.User?> = callbackFlow {
        val subscription = firestore.collection(FirebaseCollections.USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(com.starosta.messenger.data.model.User::class.java))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun findExistingPrivateChat(userId1: String, userId2: String): Chat? {
        return chatsRef
            .whereEqualTo("type", "private")
            .whereArrayContains("participantIds", userId1)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Chat::class.java) }
            .firstOrNull { userId2 in it.participantIds }
    }
}

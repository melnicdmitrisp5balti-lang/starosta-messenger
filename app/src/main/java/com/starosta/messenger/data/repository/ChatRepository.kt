package com.starosta.messenger.data.repository

import android.net.Uri
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.model.Chat
import com.starosta.messenger.data.model.ChatType
import com.starosta.messenger.data.model.Message
import com.starosta.messenger.data.model.MessageType
import com.starosta.messenger.data.remote.ChatRemoteDataSource
import com.starosta.messenger.data.remote.StorageRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDataSource: ChatRemoteDataSource,
    private val storageDataSource: StorageRemoteDataSource
) {

    fun observeChats(userId: String): Flow<List<Chat>> =
        chatDataSource.observeUserChats(userId)

    suspend fun getOrCreatePrivateChat(currentUserId: String, otherUserId: String): Resource<String> {
        return try {
            // Check if chat already exists
            val existing = chatDataSource.findExistingPrivateChat(currentUserId, otherUserId)
            if (existing != null) {
                return Resource.Success(existing.id)
            }
            // Create new
            val chat = Chat(
                type = ChatType.PRIVATE,
                participantIds = listOf(currentUserId, otherUserId)
            )
            val chatId = chatDataSource.createChat(chat)
            Resource.Success(chatId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create chat")
        }
    }

    suspend fun createGroupChat(currentUserId: String, title: String, memberIds: List<String>): Resource<String> {
        return try {
            val participants = (memberIds + currentUserId).distinct()
            val chat = Chat(
                title = title,
                type = ChatType.GROUP,
                participantIds = participants
            )
            val chatId = chatDataSource.createChat(chat)
            Resource.Success(chatId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create group chat")
        }
    }

    suspend fun getChatById(chatId: String): Chat? = chatDataSource.getChatById(chatId)

    suspend fun togglePinChat(chatId: String, userId: String) {
        chatDataSource.pinChat(chatId, userId)
    }

    fun observeMessages(chatId: String): Flow<List<Message>> =
        chatDataSource.observeMessages(chatId)

    suspend fun loadMoreMessages(chatId: String, beforeTimestamp: Long): List<Message> =
        chatDataSource.loadMoreMessages(chatId, beforeTimestamp)

    suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        text: String,
        replyToMessageId: String? = null,
        replyToText: String? = null
    ): Resource<String> {
        return try {
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                text = text,
                type = MessageType.TEXT,
                replyToMessageId = replyToMessageId,
                replyToText = replyToText
            )
            val id = chatDataSource.sendMessage(message)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUri: Uri
    ): Resource<String> {
        return try {
            val uploadResult = storageDataSource.uploadChatMedia(chatId, imageUri, "images")
            if (uploadResult.isFailure) {
                return Resource.Error(uploadResult.exceptionOrNull()?.message ?: "Upload failed")
            }
            val fileUrl = uploadResult.getOrThrow()
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                text = "",
                type = MessageType.IMAGE,
                fileUrl = fileUrl
            )
            val id = chatDataSource.sendMessage(message)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send image")
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Resource<Unit> {
        return try {
            chatDataSource.editMessage(chatId, messageId, newText)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to edit message")
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Resource<Unit> {
        return try {
            chatDataSource.deleteMessage(chatId, messageId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete message")
        }
    }

    suspend fun markMessagesAsRead(chatId: String, messageIds: List<String>) {
        messageIds.forEach { id ->
            chatDataSource.updateMessageStatus(chatId, id, com.starosta.messenger.data.model.MessageStatus.READ)
        }
    }

    suspend fun setTyping(chatId: String, userId: String, isTyping: Boolean) {
        chatDataSource.setTyping(chatId, userId, isTyping)
    }

    fun observeTyping(chatId: String, currentUserId: String): Flow<List<String>> =
        chatDataSource.observeTyping(chatId, currentUserId)

    suspend fun getAllUsers(): List<com.starosta.messenger.data.model.User> =
        chatDataSource.getAllUsers()

    fun observeUser(userId: String) = chatDataSource.observeUser(userId)
}

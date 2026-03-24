package com.starosta.messenger.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.starosta.messenger.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isSignedIn(): Boolean = auth.currentUser != null

    suspend fun sendVerificationCode(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<String> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No UID returned"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateUser(user: User) {
        firestore.collection(FirebaseCollections.USERS)
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            firestore.collection(FirebaseCollections.USERS)
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        firestore.collection(FirebaseCollections.USERS)
            .document(userId)
            .update(mapOf("fcmToken" to token))
            .await()
    }

    suspend fun updateUserOnlineStatus(userId: String, online: Boolean) {
        val updates = if (online) {
            mapOf("online" to true)
        } else {
            mapOf("online" to false, "lastSeen" to System.currentTimeMillis())
        }
        firestore.collection(FirebaseCollections.USERS)
            .document(userId)
            .update(updates)
            .await()
    }

    fun signOut() {
        auth.signOut()
    }
}

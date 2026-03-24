package com.starosta.messenger.data.repository

import android.app.Activity
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.model.User
import com.starosta.messenger.data.remote.AuthRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authDataSource: AuthRemoteDataSource
) {

    fun isSignedIn(): Boolean = authDataSource.isSignedIn()

    fun getCurrentUserId(): String? = authDataSource.getCurrentUserId()

    suspend fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) = authDataSource.sendVerificationCode(phoneNumber, activity, callbacks)

    suspend fun verifyOtp(credential: PhoneAuthCredential): Resource<String> {
        return try {
            val result = authDataSource.signInWithCredential(credential)
            if (result.isSuccess) {
                val uid = result.getOrThrow()
                // Ensure user doc exists
                val existingUser = authDataSource.getUserById(uid)
                if (existingUser == null) {
                    authDataSource.createOrUpdateUser(
                        User(
                            id = uid,
                            phone = "",
                            name = "",
                            username = ""
                        )
                    )
                }
                Resource.Success(uid)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun updateProfile(user: User): Resource<Unit> {
        return try {
            authDataSource.createOrUpdateUser(user)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    suspend fun getUserById(userId: String): User? = authDataSource.getUserById(userId)

    suspend fun updateFcmToken(token: String) {
        val uid = authDataSource.getCurrentUserId() ?: return
        authDataSource.updateFcmToken(uid, token)
    }

    suspend fun updateOnlineStatus(online: Boolean) {
        val uid = authDataSource.getCurrentUserId() ?: return
        authDataSource.updateUserOnlineStatus(uid, online)
    }

    fun signOut() = authDataSource.signOut()
}

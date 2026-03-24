package com.starosta.messenger.data.repository

import android.net.Uri
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.model.User
import com.starosta.messenger.data.remote.AuthRemoteDataSource
import com.starosta.messenger.data.remote.StorageRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val authDataSource: AuthRemoteDataSource,
    private val storageDataSource: StorageRemoteDataSource
) {

    suspend fun getCurrentUser(): User? {
        val uid = authDataSource.getCurrentUserId() ?: return null
        return authDataSource.getUserById(uid)
    }

    suspend fun updateUser(user: User): Resource<Unit> {
        return try {
            authDataSource.createOrUpdateUser(user)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update user")
        }
    }

    suspend fun uploadAndUpdatePhoto(userId: String, uri: Uri): Resource<String> {
        return try {
            val result = storageDataSource.uploadProfilePhoto(userId, uri)
            if (result.isFailure) {
                return Resource.Error(result.exceptionOrNull()?.message ?: "Upload failed")
            }
            val url = result.getOrThrow()
            val currentUser = authDataSource.getUserById(userId)
            if (currentUser != null) {
                authDataSource.createOrUpdateUser(currentUser.copy(photoUrl = url))
            }
            Resource.Success(url)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload photo")
        }
    }
}

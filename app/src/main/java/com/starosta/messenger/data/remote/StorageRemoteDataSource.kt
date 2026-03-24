package com.starosta.messenger.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRemoteDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {

    suspend fun uploadImage(uri: Uri, path: String = "images"): Result<String> {
        return try {
            val ref = storage.reference.child("$path/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(userId: String, uri: Uri): Result<String> {
        return uploadImage(uri, "profiles/$userId")
    }

    suspend fun uploadChatMedia(chatId: String, uri: Uri, type: String = "image"): Result<String> {
        val path = "chats/$chatId/$type"
        return uploadImage(uri, path)
    }

    suspend fun deleteFile(url: String) {
        try {
            storage.getReferenceFromUrl(url).delete().await()
        } catch (e: Exception) {
            // Ignore deletion errors
        }
    }
}

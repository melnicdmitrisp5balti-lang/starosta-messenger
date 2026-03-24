package com.starosta.messenger.data.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = MessageType.TEXT,
    val fileUrl: String = "",
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val status: String = MessageStatus.SENT,
    val edited: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object MessageType {
    const val TEXT = "text"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val FILE = "file"
    const val VOICE = "voice"
}

object MessageStatus {
    const val SENT = "sent"
    const val DELIVERED = "delivered"
    const val READ = "read"
}

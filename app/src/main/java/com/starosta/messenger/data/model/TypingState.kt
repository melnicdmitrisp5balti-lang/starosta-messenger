package com.starosta.messenger.data.model

data class TypingState(
    val chatId: String = "",
    val userId: String = "",
    val isTyping: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

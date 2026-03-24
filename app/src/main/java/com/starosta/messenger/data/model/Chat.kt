package com.starosta.messenger.data.model

data class Chat(
    val id: String = "",
    val title: String = "",
    val type: String = ChatType.PRIVATE,
    val participantIds: List<String> = emptyList(),
    val pinnedBy: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String = "",
    val photoUrl: String = "",
    val unreadCount: Int = 0
)

object ChatType {
    const val PRIVATE = "private"
    const val GROUP = "group"
}

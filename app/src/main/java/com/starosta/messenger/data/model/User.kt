package com.starosta.messenger.data.model

data class User(
    val id: String = "",
    val phone: String = "",
    val name: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val statusText: String = "Hey there! I am using Starosta Messenger",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val fcmToken: String = ""
)

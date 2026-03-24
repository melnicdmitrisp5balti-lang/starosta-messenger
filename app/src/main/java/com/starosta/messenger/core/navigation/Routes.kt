package com.starosta.messenger.core.navigation

object Routes {
    const val PHONE_INPUT = "phone_input"
    const val OTP = "otp/{phoneNumber}"
    const val MAIN = "main"
    const val CHAT = "chat/{chatId}"
    const val NEW_CHAT = "new_chat"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"

    fun otpRoute(phoneNumber: String) = "otp/$phoneNumber"
    fun chatRoute(chatId: String) = "chat/$chatId"
}

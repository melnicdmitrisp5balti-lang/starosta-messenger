package com.starosta.messenger.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    fun formatChatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(cal, now) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            isYesterday(cal, now) -> "Yesterday"
            isSameWeek(cal, now) -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
        }
    }

    fun formatMessageTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatLastSeen(timestamp: Long): String {
        if (timestamp == 0L) return "last seen a long time ago"
        val date = Date(timestamp)
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(cal, now) -> "last seen today at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            isYesterday(cal, now) -> "last seen yesterday at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            else -> "last seen ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)}"
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(cal: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            time = now.time
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(cal, yesterday)
    }

    private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean =
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
}

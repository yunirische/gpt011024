package com.example.gpt011024

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatUnixTime(unixTime: String): String {
    return try {
        val timestamp = unixTime.toLong() * 1000 // Преобразование в миллисекунды
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    } catch (e: Exception) {
        "Invalid date"
    }
}

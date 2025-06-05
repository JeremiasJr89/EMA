package com.ema.musicschool.data

data class Message(
    val id: String,
    val senderUsername: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
package com.ema.musicschool.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderName: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
package com.ema.musicschool.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class StudyLog(
    val date: String = "",
    val totalTimeMillis: Long = 0L,
    val userId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)

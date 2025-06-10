package com.ema.musicschool.data

data class UserProfile(
    val fullName: String = "",
    val phoneNumber: String = "",
    val instrument: String = "",
    val age: Int = 0,
    val address: String = ""
)

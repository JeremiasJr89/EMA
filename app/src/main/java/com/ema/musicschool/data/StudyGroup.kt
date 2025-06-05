package com.ema.musicschool.data

data class StudyGroup(
    val id: String,
    val name: String,
    val description: String,
    val members: MutableList<String> = mutableListOf()
)

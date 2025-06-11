package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.Performance
import com.ema.musicschool.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class PerformanceViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _performances = MutableLiveData<MutableList<Performance>>(mutableListOf())
    val performances: LiveData<MutableList<Performance>> = _performances

    init {
        _performances.value?.add(
            Performance(
                UUID.randomUUID().toString(),
                "aluno1",
                "aluno1@email.com",
                "Minha primeira música no violão",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            )
        )
        _performances.value?.add(
            Performance(
                UUID.randomUUID().toString(),
                "outroAluno",
                "outro@email.com",
                "Solo de bateria insano!",
                "https://www.youtube.com/watch?v=oHg5SJYRHA0"
            )
        )
    }

    fun publishPerformance(title: String, videoLink: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email ?: "Desconhecido"
            val newPerformance = Performance(
                UUID.randomUUID().toString(),
                currentUser.uid,
                userEmail,
                title,
                videoLink
            )
            val currentList = _performances.value ?: mutableListOf()
            currentList.add(0, newPerformance)
            _performances.value = currentList
        }
    }
}
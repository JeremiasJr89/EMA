package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.Performance
import com.ema.musicschool.data.UserRepository
import java.util.UUID

class PerformanceViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application.applicationContext)
    private val _performances = MutableLiveData<MutableList<Performance>>(mutableListOf())
    val performances: LiveData<MutableList<Performance>> = _performances

    // Dados em memória para simular performances
    init {
        // Algumas performances de exemplo
        _performances.value?.add(
            Performance(
                UUID.randomUUID().toString(),
                "aluno1",
                "aluno1",
                "Minha primeira música no violão",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            )
        )
        _performances.value?.add(
            Performance(
                UUID.randomUUID().toString(),
                "outroAluno",
                "outroAluno",
                "Solo de bateria insano!",
                "https://www.youtube.com/watch?v=oHg5SJYRHA0"
            )
        )
    }

    fun publishPerformance(title: String, videoLink: String) {
        val currentUser = userRepository.getLoggedInUser()
        if (currentUser != null) {
            val newPerformance = Performance(
                UUID.randomUUID().toString(),
                currentUser,
                currentUser,
                title,
                videoLink
            )
            val currentList = _performances.value ?: mutableListOf()
            currentList.add(0, newPerformance) // Adiciona no topo
            _performances.value = currentList
        }
    }
}
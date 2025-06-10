package com.ema.musicschool.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _loggedInUsername = MutableLiveData<String?>()
    val loggedInUsername: LiveData<String?> = _loggedInUsername

    private val _studyTimeToday = MutableLiveData<Long>(0L) // Tempo TOTAL acumulado de estudo no dia
    val studyTimeToday: LiveData<Long> = _studyTimeToday

    private val _isStudying = MutableLiveData<Boolean>(false)
    val isStudying: LiveData<Boolean> = _isStudying

    private var studyStartTime: Long = 0L // Milissegundos quando a sessão ATUAL começou
    private var accumulatedStudyTimeBeforeCurrentSession: Long = 0L // Tempo acumulado de sessões ANTERIORES

    private val handler = Handler(Looper.getMainLooper())
    private val updateStudyTimeRunnable = object : Runnable {
        override fun run() {
            if (_isStudying.value == true) {
                val currentTime = System.currentTimeMillis()
                val elapsedCurrentSession = currentTime - studyStartTime

                _studyTimeToday.value = accumulatedStudyTimeBeforeCurrentSession + elapsedCurrentSession

                handler.postDelayed(this, 1000) // Agenda para rodar novamente em 1 segundo
            }
        }
    }

    init {
        // Inicialização do _loggedInUsername (será atualizado pelo AuthViewModel)
    }

    fun updateLoggedInUser(email: String) {
        _loggedInUsername.value = email
    }

    fun startStudySession() {
        if (!_isStudying.value!!) {
            studyStartTime = System.currentTimeMillis()
            accumulatedStudyTimeBeforeCurrentSession = _studyTimeToday.value ?: 0L
            _isStudying.value = true
            handler.post(updateStudyTimeRunnable)
        }
    }

    fun stopStudySession() {
        if (_isStudying.value!!) {
            handler.removeCallbacks(updateStudyTimeRunnable)

            val elapsedCurrentSession = System.currentTimeMillis() - studyStartTime
            _studyTimeToday.value = accumulatedStudyTimeBeforeCurrentSession + elapsedCurrentSession

            _isStudying.value = false
        }
    }

    fun resetStudyTimeForNewDay() {
        _studyTimeToday.value = 0L
        accumulatedStudyTimeBeforeCurrentSession = 0L
        studyStartTime = 0L
        _isStudying.value = false
        handler.removeCallbacks(updateStudyTimeRunnable)
    }

    /**
     * Formata o tempo em milissegundos para o formato HH:MM:SS.
     */
    fun formatTime(timeInMillis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getPointsForStudyTime(): String {
        val minutes = (_studyTimeToday.value ?: 0L) / (1000 * 60) // Ainda baseado em minutos para os pontos
        return when {
            minutes >= 60 -> "Ótimo! Você estudou ${formatTime(_studyTimeToday.value ?: 0L)}! Ganhou 100 pontos e a badge 'Mestre da Música'!"
            minutes >= 30 -> "Muito bom! Você estudou ${formatTime(_studyTimeToday.value ?: 0L)}! Ganhou 50 pontos e a badge 'Estudioso Dedicado'!"
            minutes > 0 -> "Você estudou ${formatTime(_studyTimeToday.value ?: 0L)}! Continue assim!"
            else -> "Ainda não estudou hoje. Que tal começar?"
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(updateStudyTimeRunnable)
    }
}
package com.ema.musicschool.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.UserRepository

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _loggedInUsername = MutableLiveData<String?>()
    val loggedInUsername: LiveData<String?> = _loggedInUsername

    private val _studyTimeToday = MutableLiveData<Long>(0L)
    val studyTimeToday: LiveData<Long> = _studyTimeToday

    private val _isStudying = MutableLiveData<Boolean>(false)
    val isStudying: LiveData<Boolean> = _isStudying

    private var studyStartTime: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val updateStudyTimeRunnable = object : Runnable {
        override fun run() {
            if (_isStudying.value == true) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - studyStartTime
                val totalStudyTime = (_studyTimeToday.value ?: 0L)
                _studyTimeToday.value = totalStudyTime + elapsed
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun updateLoggedInUser(email: String) {
        _loggedInUsername.value = email
    }

    fun startStudySession() {
        if (!_isStudying.value!!) {
            studyStartTime = System.currentTimeMillis()
            _isStudying.value = true
            handler.post(updateStudyTimeRunnable)
        }
    }

    fun stopStudySession() {
        if (_isStudying.value!!) {
            handler.removeCallbacks(updateStudyTimeRunnable)
            val elapsed = System.currentTimeMillis() - studyStartTime
            _studyTimeToday.value = (_studyTimeToday.value ?: 0L) + elapsed
            _isStudying.value = false
        }
    }

    fun getPointsForStudyTime(): String {
        val minutes = (_studyTimeToday.value ?: 0L) / (1000 * 60)
        return when {
            minutes >= 60 -> "Ótimo! Você estudou $minutes minutos! Ganhou 100 pontos e a badge 'Mestre da Música'!"
            minutes >= 30 -> "Muito bom! Você estudou $minutes minutos! Ganhou 50 pontos e a badge 'Estudioso Dedicado'!"
            minutes > 0 -> "Você estudou $minutes minutos! Continue assim!"
            else -> "Ainda não estudou hoje. Que tal começar?"
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(updateStudyTimeRunnable)
    }
}
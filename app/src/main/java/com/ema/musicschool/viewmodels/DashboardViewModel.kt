package com.ema.musicschool.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.StudyLog
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.lifecycle.map

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val studyLogViewModel = StudyLogViewModel(application)
    private val userProfileViewModel = UserProfileViewModel(application)

    private val _loggedInUsername = MutableLiveData<String?>()
    val loggedInUsername: LiveData<String?> = _loggedInUsername

    val userFullName: LiveData<String?> = userProfileViewModel.userProfile.map { userProfile ->
        userProfile?.fullName ?: "Aluno"
    }

    private val _currentSessionDisplayTime = MutableLiveData<Long>(0L)
    val currentSessionDisplayTime: LiveData<Long> = _currentSessionDisplayTime

    private val _totalStudyTimeTodayFromFirestore = MutableLiveData<Long>(0L)
    val totalStudyTimeTodayFromFirestore: LiveData<Long> = _totalStudyTimeTodayFromFirestore

    val pastStudyLogs: LiveData<List<StudyLog>> = studyLogViewModel.pastStudyLogs

    // NOVO: LiveData para indicar se há algum histórico de estudo
    private val _hasStudyHistory = MutableLiveData<Boolean>(false)
    val hasStudyHistory: LiveData<Boolean> = _hasStudyHistory

    private val _isStudying = MutableLiveData<Boolean>(false)
    val isStudying: LiveData<Boolean> = _isStudying

    private var studyStartTime: Long = 0L
    private var accumulatedStudyTimeBeforeCurrentSession: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val updateStudyTimeRunnable = object : Runnable {
        override fun run() {
            if (_isStudying.value == true) {
                val currentTime = System.currentTimeMillis()
                val elapsedInCurrentSession = currentTime - studyStartTime

                _currentSessionDisplayTime.value = elapsedInCurrentSession

                handler.postDelayed(this, 1000)
            }
        }
    }

    private val _studyStatusMessage = MutableLiveData<String>()
    val studyStatusMessage: LiveData<String> = _studyStatusMessage

    init {
        studyLogViewModel.currentDayStudyLog.observeForever { studyLog ->
            val loadedTime = studyLog?.totalTimeMillis ?: 0L
            _totalStudyTimeTodayFromFirestore.value = loadedTime
            accumulatedStudyTimeBeforeCurrentSession = loadedTime
            Log.d("DashboardViewModel", "Tempo total do dia carregado do StudyLogViewModel: ${formatTime(loadedTime)}")
            updateStudyStatusMessage()
        }
        _isStudying.observeForever {
            updateStudyStatusMessage()
        }
        // NOVO: Observar pastStudyLogs para atualizar _hasStudyHistory e a mensagem de status
        studyLogViewModel.pastStudyLogs.observeForever { logs ->
            _hasStudyHistory.value = logs.isNotEmpty()
            updateStudyStatusMessage() // Chama updateStudyStatusMessage novamente ao carregar o histórico
        }
        updateStudyStatusMessage()
    }

    private fun updateStudyStatusMessage() {
        val totalMinutes = (_totalStudyTimeTodayFromFirestore.value ?: 0L) / (1000 * 60)
        val isCurrentlyStudying = _isStudying.value ?: false
        val hasHistory = _hasStudyHistory.value ?: false // Obtém o estado do histórico

        _studyStatusMessage.value = when {
            isCurrentlyStudying -> "Você está estudando! Continue focado(a)!"
            totalMinutes >= 60 -> "Incrível! Você já estudou ${formatTime(_totalStudyTimeTodayFromFirestore.value ?: 0L)} hoje! Que tal mais um pouco?"
            totalMinutes >= 30 -> "Mandou bem! Você já estudou ${formatTime(_totalStudyTimeTodayFromFirestore.value ?: 0L)} hoje. Mantenha o ritmo!"
            totalMinutes > 0 -> "Ótimo começo! Você já estudou ${formatTime(_totalStudyTimeTodayFromFirestore.value ?: 0L)}. Vamos treinar um pouco mais!"
            // NOVA LÓGICA AQUI
            hasHistory -> "Incrível! Você já estudou ${formatTime(_totalStudyTimeTodayFromFirestore.value ?: 0L)} hoje! Vamos treinar um pouco mais!" // Se tempo hoje é 0, mas tem histórico geral
            else -> "Ainda não estudou hoje. Que tal começar?" // Tempo hoje 0, e sem histórico geral
        }
        Log.d("DashboardViewModel", "Mensagem de status atualizada para: ${_studyStatusMessage.value}")
    }

    fun updateLoggedInUser(email: String) {
        _loggedInUsername.value = email
        userProfileViewModel.loadUserProfile()
        studyLogViewModel.loadCurrentDayStudyLogFromFirestore()
        studyLogViewModel.loadPastStudyLogsFromFirestore()
    }

    fun startStudySession() {
        if (!_isStudying.value!!) {
            studyStartTime = System.currentTimeMillis()
            _currentSessionDisplayTime.value = 0L
            accumulatedStudyTimeBeforeCurrentSession = _totalStudyTimeTodayFromFirestore.value ?: 0L
            _isStudying.value = true
            handler.post(updateStudyTimeRunnable)
            Log.d("DashboardViewModel", "Sessão iniciada. Tempo base acumulado: ${formatTime(accumulatedStudyTimeBeforeCurrentSession)}")
        }
    }

    fun stopStudySession() {
        if (_isStudying.value!!) {
            handler.removeCallbacks(updateStudyTimeRunnable)

            val elapsedInCurrentSession = System.currentTimeMillis() - studyStartTime
            val finalTotalTimeForDay = accumulatedStudyTimeBeforeCurrentSession + elapsedInCurrentSession

            _isStudying.value = false

            _currentSessionDisplayTime.value = 0L

            studyLogViewModel.saveStudyLog(finalTotalTimeForDay)

            Log.d("DashboardViewModel", "Sessão parada. Tempo final para o dia salvo: ${formatTime(finalTotalTimeForDay)}. Cronômetro zerado.")
        }
    }

    fun formatTime(timeInMillis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getPointsForStudyTime(): String {
        return ""
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(updateStudyTimeRunnable)
        studyLogViewModel.currentDayStudyLog.removeObserver { }
        _isStudying.removeObserver { }
        studyLogViewModel.pastStudyLogs.removeObserver { } // REMOVER O NOVO OBSERVER FOREVER
    }
}
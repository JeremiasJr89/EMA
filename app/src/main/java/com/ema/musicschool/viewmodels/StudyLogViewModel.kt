package com.ema.musicschool.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.LocalDataSource
import com.ema.musicschool.data.StudyLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyLogViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val localDataSource = LocalDataSource(application.applicationContext)

    private val _currentDayStudyLog = MutableLiveData<StudyLog?>()
    val currentDayStudyLog: LiveData<StudyLog?> = _currentDayStudyLog

    private val _pastStudyLogs = MutableLiveData<List<StudyLog>>()
    val pastStudyLogs: LiveData<List<StudyLog>> = _pastStudyLogs // Esta é a lista que a UI observa

    private val _operationStatus = MutableLiveData<Boolean>()
    val operationStatus: LiveData<Boolean> = _operationStatus

    private val studyLogsCollection = firestore.collection("study_logs")

    init {
        Log.d("StudyLogViewModel", "INIT: Chamando loadLogsFromLocalCache()")
        loadLogsFromLocalCache() // Carrega do cache local primeiro
        Log.d("StudyLogViewModel", "INIT: Chamando loadCurrentDayStudyLogFromFirestore()")
        loadCurrentDayStudyLogFromFirestore() // Carrega do Firestore (dia atual)
        Log.d("StudyLogViewModel", "INIT: Chamando loadPastStudyLogsFromFirestore()")
        loadPastStudyLogsFromFirestore()      // Carrega do Firestore (histórico)
    }

    /**
     * Salva ou atualiza o tempo de estudo do usuário para o dia atual no Firestore.
     * @param totalTimeMillis O tempo total de estudo acumulado para o dia.
     */
    fun saveStudyLog(totalTimeMillis: Long) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Log.e("StudyLogViewModel", "Usuário não logado ao tentar salvar log de estudo.")
            _operationStatus.value = false
            return
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val documentRef = studyLogsCollection.document("${userId}_$todayDate")

        val studyLog = StudyLog(
            date = todayDate,
            totalTimeMillis = totalTimeMillis,
            userId = userId,
            timestamp = Date()
        )

        // 1. Salvar localmente primeiro
        localDataSource.saveCurrentDayStudyTime(totalTimeMillis)
        _currentDayStudyLog.value = studyLog // Atualizar o LiveData do dia atual localmente
        Log.d("StudyLogViewModel", "Log de estudo salvo localmente para $todayDate: ${totalTimeMillis}ms")

        // Para garantir que o histórico seja atualizado imediatamente após um novo salvamento
        // SEM ESPERAR O FIRESTORE, vamos adicionar o log salvo à lista local e depois sincronizar.
        val currentPastLogs = _pastStudyLogs.value?.toMutableList() ?: mutableListOf()
        val existingLogIndex = currentPastLogs.indexOfFirst { it.date == todayDate && it.userId == userId }
        if (existingLogIndex != -1) {
            currentPastLogs[existingLogIndex] = studyLog // Atualiza o log existente
        } else {
            // Se for um novo log para o dia, adiciona e reordena.
            currentPastLogs.add(studyLog)
            currentPastLogs.sortByDescending { it.date } // Mantenha a ordem por data
        }
        _pastStudyLogs.value = currentPastLogs.toList() // Atualiza o LiveData do histórico localmente
        localDataSource.savePastStudyLogs(currentPastLogs) // Salva a lista atualizada no cache

        // 2. Tentar salvar no Firestore
        documentRef.set(studyLog)
            .addOnSuccessListener {
                _operationStatus.value = true
                Log.d("StudyLogViewModel", "Log de estudo salvo/atualizado no Firestore para $todayDate")
                // Após salvar no Firestore, recarrega o histórico para garantir a sincronização e consistência
                loadPastStudyLogsFromFirestore() // Força um recarregamento após salvamento Firestore
            }
            .addOnFailureListener { e ->
                _operationStatus.value = false
                Log.e("StudyLogViewModel", "Erro ao salvar log de estudo no Firestore para $todayDate", e)
            }
    }

    /**
     * Carrega os logs de estudo do cache local e atualiza o LiveData.
     */
    private fun loadLogsFromLocalCache() {
        // Carrega tempo do dia atual
        val cachedCurrentDayTime = localDataSource.getCurrentDayStudyTime()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        _currentDayStudyLog.value = StudyLog(date = todayDate, totalTimeMillis = cachedCurrentDayTime, userId = firebaseAuth.currentUser?.uid ?: "")

        // Carrega logs passados
        val cachedPastLogs = localDataSource.getPastStudyLogs()
        _pastStudyLogs.value = cachedPastLogs // <--- ATUALIZA O LIVEDATA COM CACHE LOCAL
        Log.d("StudyLogViewModel", "Logs de estudo carregados do cache local: ${cachedPastLogs.size} registros.")
    }

    /**
     * Carrega o log de estudo do dia atual do Firestore e atualiza o cache e o LiveData.
     */
    fun loadCurrentDayStudyLogFromFirestore() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _currentDayStudyLog.value = StudyLog(date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time), totalTimeMillis = 0L, userId = "")
            return
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val documentRef = studyLogsCollection.document("${userId}_$todayDate")

        documentRef.get()
            .addOnSuccessListener { document ->
                val studyLog = if (document.exists()) {
                    document.toObject(StudyLog::class.java)
                } else {
                    StudyLog(date = todayDate, totalTimeMillis = 0L, userId = userId)
                }
                _currentDayStudyLog.value = studyLog
                localDataSource.saveCurrentDayStudyTime(studyLog?.totalTimeMillis ?: 0L)
                Log.d("StudyLogViewModel", "Log de estudo do dia atual carregado do Firestore: ${studyLog?.totalTimeMillis}ms")
            }
            .addOnFailureListener { e ->
                Log.e("StudyLogViewModel", "Erro ao carregar log de estudo do dia atual do Firestore", e)
            }
    }

    /**
     * Carrega os logs de estudo anteriores do Firestore e atualiza o cache e o LiveData.
     */
    fun loadPastStudyLogsFromFirestore() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _pastStudyLogs.value = emptyList() // Garante que a lista está vazia se não há usuário
            Log.d("StudyLogViewModel", "loadPastStudyLogsFromFirestore: Usuário não logado, retornando lista vazia.")
            return
        }

        val sevenDaysAgo = Calendar.getInstance()
        sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgoDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sevenDaysAgo.time)

        Log.d("StudyLogViewModel", "loadPastStudyLogsFromFirestore: User ID being queried: $userId")
        Log.d("StudyLogViewModel", "loadPastStudyLogsFromFirestore: Querying dates >= $sevenDaysAgoDateString")

        studyLogsCollection
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", sevenDaysAgoDateString)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val logs = querySnapshot.documents.mapNotNull { it.toObject(StudyLog::class.java) }
                _pastStudyLogs.value = logs // <--- ATUALIZA O LIVEDATA COM DADOS DO FIRESTORE
                localDataSource.savePastStudyLogs(logs) // Atualiza cache local
                Log.d("StudyLogViewModel", "Logs de estudo anteriores carregados do Firestore: ${logs.size} registros.")
                logs.forEach { Log.d("StudyLogViewModel", "Loaded Log (Converted): Date=${it.date}, Time=${it.totalTimeMillis}, User=${it.userId}") }
            }
            .addOnFailureListener { e ->
                Log.e("StudyLogViewModel", "ERRO ao carregar logs de estudo anteriores do Firestore", e)
                // Se falhar no Firestore, o LiveData _pastStudyLogs já tem o valor do cache (se houver)
            }
    }
}
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
    val pastStudyLogs: LiveData<List<StudyLog>> = _pastStudyLogs

    private val _operationStatus = MutableLiveData<Boolean>()
    val operationStatus: LiveData<Boolean> = _operationStatus

    private val studyLogsCollection = firestore.collection("study_logs")

    init {
        loadLogsFromLocalCache()
        loadCurrentDayStudyLogFromFirestore()
        loadPastStudyLogsFromFirestore()
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

        val todayDate =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val documentRef = studyLogsCollection.document("${userId}_$todayDate")

        val studyLog = StudyLog(
            date = todayDate,
            totalTimeMillis = totalTimeMillis,
            userId = userId,
            timestamp = Date() // Incluir timestamp para ordenação e sincronização
        )

        localDataSource.saveCurrentDayStudyTime(totalTimeMillis)
        _currentDayStudyLog.value = studyLog // Atualizar o LiveData local imediatamente
        Log.d(
            "StudyLogViewModel",
            "Log de estudo salvo localmente para $todayDate: ${totalTimeMillis}ms"
        )

        documentRef.set(studyLog)
            .addOnSuccessListener {
                _operationStatus.value = true
                Log.d(
                    "StudyLogViewModel",
                    "Log de estudo salvo/atualizado no Firestore para $todayDate"
                )
                loadPastStudyLogsFromFirestore()
            }
            .addOnFailureListener { e ->
                _operationStatus.value = false
                Log.e(
                    "StudyLogViewModel",
                    "Erro ao salvar log de estudo no Firestore para $todayDate",
                    e
                )
            }
    }

    /**
     * Carrega os logs de estudo do cache local.
     */
    private fun loadLogsFromLocalCache() {
        val cachedCurrentDayTime = localDataSource.getCurrentDayStudyTime()
        val todayDate =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        _currentDayStudyLog.value = StudyLog(
            date = todayDate,
            totalTimeMillis = cachedCurrentDayTime,
            userId = firebaseAuth.currentUser?.uid ?: ""
        )

        val cachedPastLogs = localDataSource.getPastStudyLogs()
        _pastStudyLogs.value = cachedPastLogs
        Log.d(
            "StudyLogViewModel",
            "Logs de estudo carregados do cache local: ${cachedPastLogs.size} registros."
        )
    }

    /**
     * Carrega o log de estudo do dia atual do Firestore e atualiza o cache.
     */
    fun loadCurrentDayStudyLogFromFirestore() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _currentDayStudyLog.value = StudyLog(
                date = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Calendar.getInstance().time), totalTimeMillis = 0L, userId = ""
            )
            return
        }

        val todayDate =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val documentRef = studyLogsCollection.document("${userId}_$todayDate")

        documentRef.get()
            .addOnSuccessListener { document ->
                val studyLog = if (document.exists()) {
                    document.toObject(StudyLog::class.java)
                } else {
                    StudyLog(date = todayDate, totalTimeMillis = 0L, userId = userId)
                }
                _currentDayStudyLog.value = studyLog // Atualiza LiveData
                localDataSource.saveCurrentDayStudyTime(
                    studyLog?.totalTimeMillis ?: 0L
                ) // Atualiza cache local
                Log.d(
                    "StudyLogViewModel",
                    "Log de estudo do dia atual carregado do Firestore: ${studyLog?.totalTimeMillis}ms"
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    "StudyLogViewModel",
                    "Erro ao carregar log de estudo do dia atual do Firestore",
                    e
                )
            }
    }

    /**
     * Carrega os logs de estudo anteriores do Firestore e atualiza o cache.
     */
    fun loadPastStudyLogsFromFirestore() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _pastStudyLogs.value = emptyList()
            Log.d(
                "StudyLogViewModel",
                "loadPastStudyLogsFromFirestore: Usuário não logado, retornando lista vazia."
            )
            return
        }

        val sevenDaysAgo = Calendar.getInstance()
        sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgoDateString =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sevenDaysAgo.time)

        Log.d("StudyLogViewModel", "loadPastStudyLogsFromFirestore: User ID being queried: $userId")
        Log.d(
            "StudyLogViewModel",
            "loadPastStudyLogsFromFirestore: Querying dates >= $sevenDaysAgoDateString"
        )

        studyLogsCollection
            .whereEqualTo("userId", userId) // <-- SUSPEITO PRINCIPAL: user ID
            .whereGreaterThanOrEqualTo("date", sevenDaysAgoDateString)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(
                    "StudyLogViewModel",
                    "Firestore query SUCESSO para logs passados."
                )
                Log.d(
                    "StudyLogViewModel",
                    "Número de documentos no querySnapshot: ${querySnapshot.documents.size}"
                )

                val logs = querySnapshot.documents.mapNotNull {
                    val log = it.toObject(StudyLog::class.java)
                    if (log == null) {
                        Log.e(
                            "StudyLogViewModel",
                            "Falha ao converter documento para StudyLog: ${it.id}"
                        )
                    }
                    log
                }
                _pastStudyLogs.value = logs
                localDataSource.savePastStudyLogs(logs)
                Log.d(
                    "StudyLogViewModel",
                    "Logs de estudo anteriores carregados do Firestore: ${logs.size} registros. (APÓS CONVERSÃO)"
                )
                logs.forEach {
                    Log.d(
                        "StudyLogViewModel",
                        "Log carregado (convertido): Data=${it.date}, Tempo=${it.totalTimeMillis}, Usuário=${it.userId}"
                    )
                }
            }
            .addOnFailureListener { e ->
                _pastStudyLogs.value = emptyList()
                Log.e(
                    "StudyLogViewModel",
                    "ERRO ao carregar logs de estudo anteriores do Firestore",
                    e
                )
            }
    }
}
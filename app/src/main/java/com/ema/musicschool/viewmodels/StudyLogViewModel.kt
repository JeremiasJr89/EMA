package com.ema.musicschool.viewmodels

import android.app.Application
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

    private val STUDY_LOGS_COLLECTION = "study_logs"
    private val FORMATO_DATA = "yyyy-MM-dd"
    private val USER_ID = "userId"
    private val DATE_FIELD = "date"


    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val localDataSource = LocalDataSource(application.applicationContext)

    private val _currentDayStudyLog = MutableLiveData<StudyLog?>()
    val currentDayStudyLog: LiveData<StudyLog?> = _currentDayStudyLog

    private val _pastStudyLogs = MutableLiveData<List<StudyLog>>()
    val pastStudyLogs: LiveData<List<StudyLog>> = _pastStudyLogs

    private val _operationStatus = MutableLiveData<Boolean>()
    val operationStatus: LiveData<Boolean> = _operationStatus

    private val studyLogsCollection = firestore.collection(STUDY_LOGS_COLLECTION)

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
            _operationStatus.value = false
            return
        }

        val todayDate =
            SimpleDateFormat(FORMATO_DATA, Locale.getDefault()).format(Calendar.getInstance().time)
        val documentRef = studyLogsCollection.document("${userId}_$todayDate")

        val studyLog = StudyLog(
            date = todayDate,
            totalTimeMillis = totalTimeMillis,
            userId = userId,
            timestamp = Date()
        )

        localDataSource.saveCurrentDayStudyTime(totalTimeMillis)
        _currentDayStudyLog.value = studyLog

        val currentPastLogs = _pastStudyLogs.value?.toMutableList() ?: mutableListOf()
        val existingLogIndex =
            currentPastLogs.indexOfFirst { it.date == todayDate && it.userId == userId }
        if (existingLogIndex != -1) {
            currentPastLogs[existingLogIndex] = studyLog
        } else {
            currentPastLogs.add(studyLog)
            currentPastLogs.sortByDescending { it.date }
        }
        _pastStudyLogs.value = currentPastLogs.toList()
        localDataSource.savePastStudyLogs(currentPastLogs)

        documentRef.set(studyLog)
            .addOnSuccessListener {
                _operationStatus.value = true

                loadPastStudyLogsFromFirestore()
            }
            .addOnFailureListener { e ->
                _operationStatus.value = false
            }
    }

    /**
     * Carrega os logs de estudo do cache local e atualiza o LiveData.
     */
    private fun loadLogsFromLocalCache() {
        val cachedCurrentDayTime = localDataSource.getCurrentDayStudyTime()
        val todayDate =
            SimpleDateFormat(FORMATO_DATA, Locale.getDefault()).format(Calendar.getInstance().time)
        _currentDayStudyLog.value = StudyLog(
            date = todayDate,
            totalTimeMillis = cachedCurrentDayTime,
            userId = firebaseAuth.currentUser?.uid ?: ""
        )

        val cachedPastLogs = localDataSource.getPastStudyLogs()
        _pastStudyLogs.value = cachedPastLogs
    }

    /**
     * Carrega o log de estudo do dia atual do Firestore e atualiza o cache e o LiveData.
     */
    fun loadCurrentDayStudyLogFromFirestore() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _currentDayStudyLog.value = StudyLog(
                date = SimpleDateFormat(
                    FORMATO_DATA,
                    Locale.getDefault()
                ).format(Calendar.getInstance().time), totalTimeMillis = 0L, userId = ""
            )
            return
        }

        val todayDate =
            SimpleDateFormat(FORMATO_DATA, Locale.getDefault()).format(Calendar.getInstance().time)
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
            }
            .addOnFailureListener { e ->
            }
    }

    /**
     * Carrega os logs de estudo anteriores do Firestore e atualiza o cache e o LiveData.
     */
    fun loadPastStudyLogsFromFirestore() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _pastStudyLogs.value = emptyList()
            return
        }

        val sevenDaysAgo = Calendar.getInstance()
        sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgoDateString =
            SimpleDateFormat(FORMATO_DATA, Locale.getDefault()).format(sevenDaysAgo.time)

        studyLogsCollection
            .whereEqualTo(USER_ID, userId)
            .whereGreaterThanOrEqualTo(DATE_FIELD, sevenDaysAgoDateString)
            .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val logs = querySnapshot.documents.mapNotNull { it.toObject(StudyLog::class.java) }
                _pastStudyLogs.value = logs
                localDataSource.savePastStudyLogs(logs)
            }
            .addOnFailureListener { e ->
            }
    }
}
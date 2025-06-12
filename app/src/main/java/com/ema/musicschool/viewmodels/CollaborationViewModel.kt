package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.Message
import com.ema.musicschool.data.StudyGroup
import com.ema.musicschool.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.UUID

class CollaborationViewModel(application: Application) : AndroidViewModel(application) {
    private val STUDY_GROUPS_COLLECTION = "study_groups"
    private val USER_PROFILE_COLLECTION = "user_profiles"
    private val DESCONHECIDO = "Desconhecido"
    private val NOME_DESCONHECIDO = "Nome Desconhecido"
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _studyGroups = MutableLiveData<MutableList<StudyGroup>>(mutableListOf())
    val studyGroups: LiveData<MutableList<StudyGroup>> = _studyGroups

    private val _currentGroupMessages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val currentGroupMessages: LiveData<MutableList<Message>> = _currentGroupMessages

    private val _currentGroupId = MutableLiveData<String?>(null)
    val currentGroupId: LiveData<String?> = _currentGroupId

    private val userProfileCache = mutableMapOf<String, UserProfile>()
    private val userProfilesCollection = firestore.collection(USER_PROFILE_COLLECTION)
    private val studyGroupsCollection = firestore.collection(STUDY_GROUPS_COLLECTION )

    init {
        loadStaticStudyGroups()
        _currentGroupId.observeForever { groupId ->
            if (groupId != null) {
                loadGroupMessages(groupId)
            } else {
                _currentGroupMessages.value = mutableListOf()
            }
        }
    }

    private fun loadStaticStudyGroups() {
        val group1 = StudyGroup("group1", "Violão Acústico", "Grupo para quem ama violão e bossa nova.")
        group1.members.add("aluno1@email.com")
        val group2 = StudyGroup("group2", "Teoria Musical Avançada", "Para os futuros maestros!")
        group2.members.add("outro@email.com")

        _studyGroups.value?.add(group1)
        _studyGroups.value?.add(group2)
    }

    fun joinGroup(groupId: String) {
        val currentUserEmail = firebaseAuth.currentUser?.email
        if (currentUserEmail != null) {
            _studyGroups.value?.find { it.id == groupId }?.apply {
                if (!members.contains(currentUserEmail)) {
                    members.add(currentUserEmail)
                    _studyGroups.value = _studyGroups.value
                }
            }
            _currentGroupId.value = groupId
        }
    }

    fun postMessage(groupId: String, content: String) {
        val currentUser = firebaseAuth.currentUser ?: return

        val senderId = currentUser.uid
        val senderEmail = currentUser.email ?: DESCONHECIDO

        getSenderName(senderId) { senderName ->
            val messageId = UUID.randomUUID().toString()
            val newMessage = Message(
                id = messageId,
                senderId = senderId,
                senderUsername = senderEmail,
                senderName = senderName,
                content = content,
                timestamp = Date()
            )

            firestore.collection(STUDY_GROUPS_COLLECTION )
                .document(groupId)
                .collection("messages")
                .document(messageId)
                .set(newMessage)
                .addOnSuccessListener {
                    val currentList = _currentGroupMessages.value ?: mutableListOf()
                    currentList.add(newMessage)
                    _currentGroupMessages.value = currentList
                }
                .addOnFailureListener { e ->
                }
        }
    }

    fun selectGroup(groupId: String?) {
        _currentGroupId.value = groupId
    }

    private fun loadGroupMessages(groupId: String) {
        val messagesCollection = firestore.collection(STUDY_GROUPS_COLLECTION )
            .document(groupId)
            .collection("messages")

        messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { querySnapshot ->
                val loadedMessages = mutableListOf<Message>()
                val messagesToProcess = querySnapshot.documents.mapNotNull { it.toObject(Message::class.java) }

                if (messagesToProcess.isEmpty()) {
                    _currentGroupMessages.value = mutableListOf() //
                    return@addOnSuccessListener
                }

                var messagesProcessedCount = 0
                val finalMessages = messagesToProcess.toMutableList()

                messagesToProcess.forEachIndexed { index, message ->
                    if (message.senderName.isEmpty() && message.senderId.isNotEmpty()) {
                        getSenderName(message.senderId) { name ->
                            finalMessages[index] = message.copy(senderName = name)
                            messagesProcessedCount++
                            if (messagesProcessedCount == messagesToProcess.size) {
                                _currentGroupMessages.value = finalMessages.sortedBy { it.timestamp }.toMutableList()
                            }
                        }
                    } else {
                        messagesProcessedCount++
                        if (messagesProcessedCount == messagesToProcess.size) {
                            _currentGroupMessages.value = finalMessages.sortedBy { it.timestamp }.toMutableList()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                _currentGroupMessages.value = mutableListOf()
            }
    }

    fun isUserInGroup(groupId: String): Boolean {
        val currentUserEmail = firebaseAuth.currentUser?.email
        return _studyGroups.value?.find { it.id == groupId }?.members?.contains(currentUserEmail) ?: false
    }

    private fun getSenderName(userId: String, callback: (String) -> Unit) {
        if (userProfileCache.containsKey(userId)) {
            callback(userProfileCache[userId]?.fullName ?: NOME_DESCONHECIDO)
            return
        }

        userProfilesCollection.document(userId).get()
            .addOnSuccessListener { document ->
                val userProfile = document.toObject(UserProfile::class.java)
                val name = userProfile?.fullName ?: NOME_DESCONHECIDO
                if (userProfile != null) {
                    userProfileCache[userId] = userProfile
                }
                callback(name)
            }
            .addOnFailureListener { e ->
                callback(NOME_DESCONHECIDO)
            }
    }

    fun getSenderNameForDisplay(userId: String, callback: (String) -> Unit) {
        getSenderName(userId, callback)
    }

    override fun onCleared() {
        super.onCleared()
        _currentGroupId.removeObserver { }
    }
}
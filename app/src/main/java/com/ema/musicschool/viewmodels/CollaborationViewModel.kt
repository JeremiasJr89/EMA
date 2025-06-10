package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.Message
import com.ema.musicschool.data.StudyGroup
import com.ema.musicschool.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class CollaborationViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _studyGroups = MutableLiveData<MutableList<StudyGroup>>(mutableListOf())
    val studyGroups: LiveData<MutableList<StudyGroup>> = _studyGroups

    private val _currentGroupMessages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val currentGroupMessages: LiveData<MutableList<Message>> = _currentGroupMessages

    private val _currentGroupId = MutableLiveData<String?>(null)
    val currentGroupId: LiveData<String?> = _currentGroupId


    init {
        val group1 =
            StudyGroup("group1", "Violão Acústico", "Grupo para quem ama violão e bossa nova.")
        group1.members.add("aluno1@email.com")
        val group2 = StudyGroup("group2", "Teoria Musical Avançada", "Para os futuros maestros!")
        group2.members.add("outro@email.com")

        _studyGroups.value?.add(group1)
        _studyGroups.value?.add(group2)

        _currentGroupMessages.value?.add(
            Message(
                UUID.randomUUID().toString(),
                "aluno1@email.com",
                "Olá a todos! Alguém tem dicas de acordes dissonantes?"
            )
        )
        _currentGroupMessages.value?.add(
            Message(
                UUID.randomUUID().toString(),
                "outro@email.com",
                "Dá uma olhada no livro de harmonia do Arnold Schoenberg!"
            )
        )
    }

    fun joinGroup(groupId: String) {
        val currentUser = firebaseAuth.currentUser?.email
        if (currentUser != null) {
            _studyGroups.value?.find { it.id == groupId }?.apply {
                if (!members.contains(currentUser)) {
                    members.add(currentUser)
                    _studyGroups.value = _studyGroups.value
                }
            }
            _currentGroupId.value = groupId
        }
    }

    fun postMessage(groupId: String, content: String) {
        val currentUser = firebaseAuth.currentUser?.email
        if (currentUser != null) {
            val newMessage = Message(UUID.randomUUID().toString(), currentUser, content)
            val currentList = _currentGroupMessages.value ?: mutableListOf()
            currentList.add(newMessage)
            _currentGroupMessages.value = currentList
        }
    }

    fun selectGroup(groupId: String?) {
        _currentGroupId.value = groupId
        _currentGroupMessages.value = _currentGroupMessages.value
    }

    fun isUserInGroup(groupId: String): Boolean {
        val currentUser = firebaseAuth.currentUser?.email
        return _studyGroups.value?.find { it.id == groupId }?.members?.contains(currentUser)
            ?: false
    }
}
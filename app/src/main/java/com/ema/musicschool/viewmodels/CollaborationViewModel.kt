package com.ema.musicschool.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.Message
import com.ema.musicschool.data.StudyGroup
import com.ema.musicschool.data.UserProfile
import com.ema.musicschool.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.UUID

class CollaborationViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance() // Instância do Firestore

    private val _studyGroups = MutableLiveData<MutableList<StudyGroup>>(mutableListOf())
    val studyGroups: LiveData<MutableList<StudyGroup>> = _studyGroups

    private val _currentGroupMessages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val currentGroupMessages: LiveData<MutableList<Message>> = _currentGroupMessages

    private val _currentGroupId = MutableLiveData<String?>(null)
    val currentGroupId: LiveData<String?> = _currentGroupId

    // Cache simples de perfis de usuário para evitar múltiplas buscas do mesmo perfil
    private val userProfileCache = mutableMapOf<String, UserProfile>()

    // Referência à coleção de perfis de usuário
    private val userProfilesCollection = firestore.collection("user_profiles")

    init {
        loadStudyGroups() // Chamada para carregar grupos
        // Mensagens de exemplo para o grupo 1 (apenas se for o início do protótipo)
        // Em um app real, as mensagens seriam carregadas do Firestore para um grupo selecionado
        _currentGroupMessages.value?.add(Message(UUID.randomUUID().toString(), "aluno1_uid", "aluno1@email.com", "João Silva", "Olá a todos! Alguém tem dicas de acordes dissonantes?"))
        _currentGroupMessages.value?.add(Message(UUID.randomUUID().toString(), "outro_uid", "outro@email.com", "Maria Souza", "Dá uma olhada no livro de harmonia do Arnold Schoenberg!"))
    }

    private fun loadStudyGroups() {
        // Por enquanto, os grupos são estáticos.
        val group1 = StudyGroup("group1", "Violão Acústico", "Grupo para quem ama violão e bossa nova.")
        group1.members.add("aluno1@email.com") // IDs dos membros podem ser UIDs
        val group2 = StudyGroup("group2", "Teoria Musical Avançada", "Para os futuros maestros!")
        group2.members.add("outro@email.com") // IDs dos membros podem ser UIDs

        _studyGroups.value?.add(group1)
        _studyGroups.value?.add(group2)
    }

    fun joinGroup(groupId: String) {
        val currentUserEmail = firebaseAuth.currentUser?.email
        if (currentUserEmail != null) {
            _studyGroups.value?.find { it.id == groupId }?.apply {
                if (!members.contains(currentUserEmail)) {
                    members.add(currentUserEmail)
                    _studyGroups.value = _studyGroups.value // Notifica o LiveData
                }
            }
            _currentGroupId.value = groupId
            // Em um app real, você carregaria as mensagens do grupo aqui do Firestore
            // e então enriqueceria elas com os nomes.
            // Para o protótipo, _currentGroupMessages ainda é estático.
        }
    }

    fun postMessage(groupId: String, content: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val senderId = currentUser.uid
            val senderEmail = currentUser.email ?: "Desconhecido"

            // Busca o nome do usuário no cache ou no Firestore antes de postar a mensagem
            getSenderName(senderId) { senderName ->
                val newMessage = Message(
                    id = UUID.randomUUID().toString(),
                    senderId = senderId,
                    senderUsername = senderEmail,
                    senderName = senderName, // Nome do remetente
                    content = content,
                    timestamp = Date()
                )

                // Em um app real, você salvaria esta mensagem em uma subcoleção do grupo no Firestore.
                // Ex: firestore.collection("study_groups").document(groupId).collection("messages").add(newMessage)

                val currentList = _currentGroupMessages.value ?: mutableListOf()
                currentList.add(newMessage)
                _currentGroupMessages.value = currentList // Atualiza a UI
            }
        }
    }

    fun selectGroup(groupId: String?) {
        _currentGroupId.value = groupId
        // Em um cenário real, você carregaria as mensagens específicas do grupo selecionado aqui do Firestore.
        // E então, para cada mensagem carregada, você chamaria `getSenderName` para preencher o `senderName`.
        _currentGroupMessages.value = _currentGroupMessages.value // Simplesmente renotifica para atualizar a UI se necessário
    }

    fun isUserInGroup(groupId: String): Boolean {
        val currentUserEmail = firebaseAuth.currentUser?.email
        return _studyGroups.value?.find { it.id == groupId }?.members?.contains(currentUserEmail) ?: false
    }

    /**
     * Busca o nome completo de um usuário pelo seu UID.
     * Usa um cache local para performance.
     */
    private fun getSenderName(userId: String, callback: (String) -> Unit) {
        if (userProfileCache.containsKey(userId)) {
            callback(userProfileCache[userId]?.fullName ?: "Nome Desconhecido")
            return
        }

        userProfilesCollection.document(userId).get()
            .addOnSuccessListener { document ->
                val userProfile = document.toObject(UserProfile::class.java)
                val name = userProfile?.fullName ?: "Nome Desconhecido"
                if (userProfile != null) {
                    userProfileCache[userId] = userProfile // Adiciona ao cache
                }
                callback(name)
            }
            .addOnFailureListener { e ->
                Log.e("CollaborationViewModel", "Erro ao buscar perfil para UID: $userId", e)
                callback("Nome Desconhecido") // Fallback em caso de erro
            }
    }

    // Método público para ser usado pelo Adapter para buscar nomes de mensagens já existentes
    fun getSenderNameForDisplay(userId: String, callback: (String) -> Unit) {
        getSenderName(userId, callback)
    }
}
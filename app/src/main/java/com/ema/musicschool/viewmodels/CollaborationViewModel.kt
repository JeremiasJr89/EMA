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
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.UUID

class CollaborationViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _studyGroups = MutableLiveData<MutableList<StudyGroup>>(mutableListOf())
    val studyGroups: LiveData<MutableList<StudyGroup>> = _studyGroups

    private val _currentGroupMessages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val currentGroupMessages: LiveData<MutableList<Message>> = _currentGroupMessages

    private val _currentGroupId = MutableLiveData<String?>(null)
    val currentGroupId: LiveData<String?> = _currentGroupId

    private val userProfileCache = mutableMapOf<String, UserProfile>()
    private val userProfilesCollection = firestore.collection("user_profiles")
    private val studyGroupsCollection = firestore.collection("study_groups") // Coleção de grupos

    init {
        // Carrega os grupos (ainda estáticos no protótipo)
        loadStaticStudyGroups()
        // Observa a seleção de grupo para carregar as mensagens correspondentes
        _currentGroupId.observeForever { groupId ->
            if (groupId != null) {
                loadGroupMessages(groupId)
            } else {
                _currentGroupMessages.value = mutableListOf() // Limpa mensagens se nenhum grupo selecionado
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
                    _studyGroups.value = _studyGroups.value // Notifica o LiveData
                }
            }
            _currentGroupId.value = groupId // Seleciona o grupo
        }
    }

    fun postMessage(groupId: String, content: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e("CollaborationViewModel", "Usuário não logado, não pode postar mensagem.")
            return
        }

        val senderId = currentUser.uid
        val senderEmail = currentUser.email ?: "Desconhecido"

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

            // SALVANDO A MENSAGEM NA SUBCOLEÇÃO DO FIRESTORE
            firestore.collection("study_groups")
                .document(groupId)
                .collection("messages") // Subcoleção 'messages' dentro do documento do grupo
                .document(messageId) // ID único para a mensagem
                .set(newMessage)
                .addOnSuccessListener {
                    Log.d("CollaborationViewModel", "Mensagem salva no Firestore para grupo $groupId.")
                    // Após salvar, adiciona à lista local para exibição imediata
                    val currentList = _currentGroupMessages.value ?: mutableListOf()
                    currentList.add(newMessage)
                    _currentGroupMessages.value = currentList
                }
                .addOnFailureListener { e ->
                    Log.e("CollaborationViewModel", "Erro ao salvar mensagem no Firestore: $e", e)
                }
        }
    }

    fun selectGroup(groupId: String?) {
        _currentGroupId.value = groupId
        // loadGroupMessages será chamado pelo observer do _currentGroupId
    }

    // NOVO: Carregar mensagens de um grupo específico do Firestore
    private fun loadGroupMessages(groupId: String) {
        val messagesCollection = firestore.collection("study_groups")
            .document(groupId)
            .collection("messages")

        messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { querySnapshot ->
                val loadedMessages = mutableListOf<Message>() // <--- Garante que loadedMessages é MutableList
                val messagesToProcess = querySnapshot.documents.mapNotNull { it.toObject(Message::class.java) }

                if (messagesToProcess.isEmpty()) {
                    _currentGroupMessages.value = mutableListOf() // <--- Se vazio, atribua um novo MutableList vazio
                    Log.d("CollaborationViewModel", "Nenhuma mensagem carregada para grupo $groupId.")
                    return@addOnSuccessListener
                }

                var messagesProcessedCount = 0
                val finalMessages = messagesToProcess.toMutableList() // É uma MutableList, isso está ok.

                messagesToProcess.forEachIndexed { index, message ->
                    if (message.senderName.isEmpty() && message.senderId.isNotEmpty()) {
                        getSenderName(message.senderId) { name ->
                            finalMessages[index] = message.copy(senderName = name)
                            messagesProcessedCount++
                            if (messagesProcessedCount == messagesToProcess.size) {
                                // AQUI ESTÁ A LINHA 142 (OU PRÓXIMA): Certifique-se de que é toMutableList() antes do cast.
                                _currentGroupMessages.value = finalMessages.sortedBy { it.timestamp }.toMutableList() // <--- Adicionado .toMutableList()
                                Log.d("CollaborationViewModel", "Mensagens carregadas e enriquecidas para grupo $groupId: ${finalMessages.size}")
                            }
                        }
                    } else {
                        messagesProcessedCount++
                        if (messagesProcessedCount == messagesToProcess.size) {
                            // AQUI ESTÁ A LINHA SIMILAR: Certifique-se de que é toMutableList() antes do cast.
                            _currentGroupMessages.value = finalMessages.sortedBy { it.timestamp }.toMutableList() // <--- Adicionado .toMutableList()
                            Log.d("CollaborationViewModel", "Mensagens carregadas (sem enriquecimento) para grupo $groupId: ${finalMessages.size}")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CollaborationViewModel", "Erro ao carregar mensagens para grupo $groupId: $e", e)
                _currentGroupMessages.value = mutableListOf() // <--- Se falhar, atribua um novo MutableList vazio
            }
    }

    fun isUserInGroup(groupId: String): Boolean {
        val currentUserEmail = firebaseAuth.currentUser?.email
        return _studyGroups.value?.find { it.id == groupId }?.members?.contains(currentUserEmail) ?: false
    }

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
                    userProfileCache[userId] = userProfile
                }
                callback(name)
            }
            .addOnFailureListener { e ->
                Log.e("CollaborationViewModel", "Erro ao buscar perfil para UID: $userId", e)
                callback("Nome Desconhecido")
            }
    }

    fun getSenderNameForDisplay(userId: String, callback: (String) -> Unit) {
        getSenderName(userId, callback)
    }

    override fun onCleared() {
        super.onCleared()
        _currentGroupId.removeObserver { } // Remover este observer
    }
}
package com.ema.musicschool.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _saveProfileResult = MutableLiveData<Boolean>()
    val saveProfileResult: LiveData<Boolean> = _saveProfileResult

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile // Este é o LiveData que vamos observar

    init {
        loadUserProfile() // Carrega o perfil ao inicializar o ViewModel
    }

    fun saveUserProfile(profile: UserProfile) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _saveProfileResult.value = false
            Log.e("UserProfileViewModel", "Usuário não logado ao tentar salvar perfil.")
            return
        }

        firestore.collection("user_profiles")
            .document(userId)
            .set(profile)
            .addOnSuccessListener {
                _saveProfileResult.value = true
                _userProfile.value = profile // Atualiza o LiveData do perfil localmente após salvar
                Log.d("UserProfileViewModel", "Perfil do usuário salvo com sucesso para UID: $userId")
            }
            .addOnFailureListener { e ->
                _saveProfileResult.value = false
                Log.e("UserProfileViewModel", "Erro ao salvar perfil do usuário para UID: $userId", e)
            }
    }

    fun loadUserProfile() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            _userProfile.value = null // Define como nulo se não houver usuário logado
            return
        }

        firestore.collection("user_profiles")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _userProfile.value = document.toObject(UserProfile::class.java)
                    Log.d("UserProfileViewModel", "Perfil do usuário carregado com sucesso para UID: $userId")
                } else {
                    _userProfile.value = null
                    Log.d("UserProfileViewModel", "Perfil não encontrado para UID: $userId")
                }
            }
            .addOnFailureListener { e ->
                _userProfile.value = null
                Log.e("UserProfileViewModel", "Erro ao carregar perfil do usuário para UID: $userId", e)
            }
    }
}
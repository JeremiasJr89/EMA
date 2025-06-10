package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.User
import com.ema.musicschool.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _registrationResult = MutableLiveData<Boolean>()
    val registrationResult: LiveData<Boolean> = _registrationResult

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    // NOVO: LiveData para capturar exceções (para mensagens de erro mais detalhadas)
    private val _authException = MutableLiveData<Exception?>()
    val authException: LiveData<Exception?> = _authException

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
        _currentUser.value = firebaseAuth.currentUser
    }

    fun login(email: String, password: String) {
        _authException.value = null // Limpa exceção anterior
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _loginResult.value = true
                } else {
                    _loginResult.value = false
                    _authException.value = task.exception // Captura a exceção
                }
            }
    }

    fun register(email: String, password: String) {
        _authException.value = null // Limpa exceção anterior
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _registrationResult.value = true
                } else {
                    _registrationResult.value = false
                    _authException.value = task.exception // Captura a exceção
                }
            }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
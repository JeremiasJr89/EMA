package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.User
import com.ema.musicschool.data.UserRepository

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application.applicationContext)

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _registrationResult = MutableLiveData<Boolean>()
    val registrationResult: LiveData<Boolean> = _registrationResult

    private val _loggedInUser = MutableLiveData<String?>()
    val loggedInUser: LiveData<String?> = _loggedInUser

    init {
        checkLoggedInUser()
    }

    fun login(username: String, passwordHash: String) {
        val user = userRepository.findUserByUsername(username)
        if (user != null && user.passwordHash == passwordHash) {
            userRepository.saveLoggedInUser(username)
            _loggedInUser.value = username
            _loginResult.value = true
        } else {
            _loginResult.value = false
        }
    }

    fun register(username: String, passwordHash: String) {
        val newUser = User(username, passwordHash)
        if (userRepository.registerUser(newUser)) {
            userRepository.saveLoggedInUser(username)
            _loggedInUser.value = username
            _registrationResult.value = true
        } else {
            _registrationResult.value = false
        }
    }

    fun logout() {
        userRepository.logoutUser()
        _loggedInUser.value = null
    }

    fun checkLoggedInUser() {
        _loggedInUser.value = userRepository.getLoggedInUser()
    }
}
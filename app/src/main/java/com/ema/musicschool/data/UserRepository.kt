package com.ema.musicschool.data

import android.content.Context
import android.content.SharedPreferences

class UserRepository(context: Context) {
    private val PREFS_NAME = "user_prefs"
    private val KEY_LOGGED_IN_USER = "logged_in_user"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    private val registeredUsers = mutableMapOf<String, User>()

    init {
        registeredUsers["aluno1"] = User("aluno1", "senha123")
    }

    fun registerUser(user: User): Boolean {
        return if (registeredUsers.containsKey(user.username)) {
            false
        } else {
            registeredUsers[user.username] = user
            true
        }
    }

    fun findUserByUsername(username: String): User? {
        return registeredUsers[username]
    }

    fun saveLoggedInUser(username: String) {
        sharedPreferences.edit().putString(KEY_LOGGED_IN_USER, username).apply()
    }

    fun getLoggedInUser(): String? {
        return sharedPreferences.getString(KEY_LOGGED_IN_USER, null)
    }

    fun logoutUser() {
        sharedPreferences.edit().remove(KEY_LOGGED_IN_USER).apply()
    }
}
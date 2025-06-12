package com.ema.musicschool.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ema.musicschool.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val USER_PROFILE_COLLECTION = "user_profiles"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _saveProfileResult = MutableLiveData<Boolean>()
    val saveProfileResult: LiveData<Boolean> = _saveProfileResult

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    init {
        loadUserProfile()
    }

    fun saveUserProfile(profile: UserProfile) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _saveProfileResult.value = false
            return
        }

        firestore.collection(USER_PROFILE_COLLECTION)
            .document(userId)
            .set(profile)
            .addOnSuccessListener {
                _saveProfileResult.value = true
                _userProfile.value = profile
            }
            .addOnFailureListener { e ->
                _saveProfileResult.value = false
            }
    }

    fun loadUserProfile() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            _userProfile.value = null
        }

        userId?.let {
            firestore.collection(USER_PROFILE_COLLECTION)
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        _userProfile.value = document.toObject(UserProfile::class.java)
                    } else {
                        _userProfile.value = null
                    }
                }
                .addOnFailureListener { e ->
                    _userProfile.value = null
                }
        }
    }
}
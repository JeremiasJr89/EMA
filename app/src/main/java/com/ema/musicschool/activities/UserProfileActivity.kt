package com.ema.musicschool.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.data.UserProfile
import com.ema.musicschool.databinding.ActivityUserProfileBinding
import com.ema.musicschool.viewmodels.UserProfileViewModel

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val userProfileViewModel: UserProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val instrument = binding.etInstrument.text.toString().trim()
            val ageString = binding.etAge.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()

            if (fullName.isEmpty() || phoneNumber.isEmpty() || instrument.isEmpty() || ageString.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageString.toIntOrNull()
            if (age == null || age < 12 || age > 17) { // Idade foco em adolescentes (12-17)
                Toast.makeText(this, "A idade deve ser entre 12 e 17 anos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userProfile = UserProfile(
                fullName = fullName,
                phoneNumber = phoneNumber,
                instrument = instrument,
                age = age,
                address = address
            )

            userProfileViewModel.saveUserProfile(userProfile)
        }
    }

    private fun setupObservers() {
        userProfileViewModel.saveProfileResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Perfil salvo com sucesso!", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            } else {
                Toast.makeText(this, "Erro ao salvar perfil. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        // Adicione flags para limpar a back stack se necessário (para que o usuário não volte para o login/cadastro)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
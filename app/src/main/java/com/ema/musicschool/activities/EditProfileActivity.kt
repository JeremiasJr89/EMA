package com.ema.musicschool.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.data.UserProfile
import com.ema.musicschool.databinding.ActivityEditProfileBinding
import com.ema.musicschool.viewmodels.UserProfileViewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding // Binding para o layout de edição
    private val userProfileViewModel: UserProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater) // Inflar o novo layout
        setContentView(binding.root)

        setupToolbar() // Configurar a toolbar para ter um botão de voltar
        setupObservers()
        setupListeners()

        // Carrega o perfil ao iniciar a Activity
        userProfileViewModel.loadUserProfile()
    }

    private fun setupToolbar() {
        supportActionBar?.title = "Editar Perfil"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Habilita o botão de voltar
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Lida com o clique no botão de voltar da toolbar
        return true
    }

    private fun setupObservers() {
        userProfileViewModel.userProfile.observe(this) { userProfile ->
            // Preenche os campos com os dados existentes do perfil
            userProfile?.let {
                binding.etFullName.setText(it.fullName)
                binding.etPhoneNumber.setText(it.phoneNumber)
                binding.etInstrument.setText(it.instrument)
                binding.etAge.setText(it.age.toString())
                binding.etAddress.setText(it.address)
            }
        }

        userProfileViewModel.saveProfileResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish() // Volta para a tela anterior (Dashboard)
            } else {
                Toast.makeText(this, "Erro ao atualizar perfil. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        }
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
            if (age == null || age < 12 || age > 17) {
                Toast.makeText(this, "A idade deve ser entre 12 e 17 anos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedProfile = UserProfile(
                fullName = fullName,
                phoneNumber = phoneNumber,
                instrument = instrument,
                age = age,
                address = address
            )

            userProfileViewModel.saveUserProfile(updatedProfile) // Usa o mesmo método de salvamento
        }
    }
}
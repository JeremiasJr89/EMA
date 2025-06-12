package com.ema.musicschool.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.R
import com.ema.musicschool.data.UserProfile
import com.ema.musicschool.databinding.ActivityEditProfileBinding
import com.ema.musicschool.viewmodels.UserProfileViewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val userProfileViewModel: UserProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        setupListeners()

        userProfileViewModel.loadUserProfile()
    }

    private fun setupToolbar() {
        supportActionBar?.title = getString(R.string.ema_strings_editar_perfil)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupObservers() {
        userProfileViewModel.userProfile.observe(this) { userProfile ->
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
                Toast.makeText(this,
                    getString(R.string.ema_strings_perfil_atualizado_com_sucesso), Toast.LENGTH_SHORT).show()
                finish() // Volta para a tela anterior (Dashboard)
            } else {
                Toast.makeText(this,
                    getString(R.string.ema_strings_erro_ao_atualizar_perfil_tente_novamente), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this,
                    getString(R.string.ema_strings_por_favor_preencha_todos_os_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageString.toIntOrNull()
            if (age == null || age < 12 || age > 17) {
                Toast.makeText(this,
                    getString(R.string.ema_strings_a_idade_deve_ser_entre_12_e_17_anos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedProfile = UserProfile(
                fullName = fullName,
                phoneNumber = phoneNumber,
                instrument = instrument,
                age = age,
                address = address
            )

            userProfileViewModel.saveUserProfile(updatedProfile)
        }
    }
}
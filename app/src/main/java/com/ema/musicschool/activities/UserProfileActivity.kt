package com.ema.musicschool.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.R
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
                Toast.makeText(this, R.string.ema_strings_por_favor_preencha_todos_os_campos, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageString.toIntOrNull()
            if (age == null || age < 12 || age > 17) {
                Toast.makeText(this, R.string.ema_strings_a_idade_deve_ser_entre_12_e_17_anos, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this,
                    getString(R.string.ema_strings_perfil_salvo_com_sucesso), Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            } else {
                Toast.makeText(this,
                    getString(R.string.ema_strings_erro_ao_salvar_perfil_tente_novamente), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
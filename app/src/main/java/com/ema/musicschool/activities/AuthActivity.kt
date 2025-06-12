package com.ema.musicschool.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.R
import com.ema.musicschool.databinding.ActivityAuthBinding
import com.ema.musicschool.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (authViewModel.currentUser.value != null) {
            navigateToDashboard()
            return
        }

        setupObservers()
        setupListeners()
        setupOnBackPressedCallback()
    }

    // NOVO MÉTODO: Configura o comportamento do botão Voltar
    private fun setupOnBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@AuthActivity)
                    .setTitle(R.string.ema_strings_sair_do_aplicativo)
                    .setMessage(R.string.ema_strings_tem_certeza_que_deseja_sair_do_aplicativo)
                    .setPositiveButton(R.string.ema_strings_sim) { dialog, which ->
                        finishAffinity()
                    }
                    .setNegativeButton(R.string.ema_strings_nao) { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupObservers() {
        authViewModel.loginResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, R.string.ema_strings_login_realizado_com_sucesso, Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
        }

        authViewModel.registrationResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this,
                    getString(R.string.ema_strings_cadastro_de_usu_rio_realizado_agora_complete_seu_perfil), Toast.LENGTH_LONG).show()
                navigateToUserProfile()
            }
        }

        authViewModel.authException.observe(this) { exception ->
            exception?.let {
                val message = when (it) {
                    is FirebaseAuthUserCollisionException -> getString(R.string.ema_strings_este_e_mail_j_est_cadastrado)
                    is FirebaseAuthWeakPasswordException -> getString(R.string.ema_strings_a_senha_muito_fraca_use_uma_senha_mais_forte_m_nimo_6_caracteres)
                    is FirebaseAuthInvalidCredentialsException -> getString(R.string.ema_strings_credenciais_inv_lidas_verifique_o_e_mail_e_a_senha)
                    else -> getString(R.string.ema_strings_ocorreu_um_erro, it.localizedMessage)
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this,
                    getString(R.string.ema_strings_preencha_o_e_mail_e_a_senha), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.length >= 6) {
                authViewModel.register(email, password)
            } else if (password.length < 6) {
                Toast.makeText(this,
                    getString(R.string.ema_strings_a_senha_deve_ter_no_m_nimo_6_caracteres), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, R.string.ema_strings_preencha_o_e_mail_e_a_senha, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun navigateToUserProfile() {
        val intent = Intent(this, UserProfileActivity::class.java)
        startActivity(intent)
        finish()
    }
}
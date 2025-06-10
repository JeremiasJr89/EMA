package com.ema.musicschool.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

        // Verifica o estado de login ao iniciar a atividade
        // Se o usuário já estiver logado (e tiver um perfil, opcional), vai direto para o Dashboard.
        // Se já autenticado mas não tem perfil, pode ir para a tela de perfil.
        // Para este protótipo, se autenticado, vai para o dashboard.
        if (authViewModel.currentUser.value != null) {
            navigateToDashboard() // Ou navigateToUserProfileIfMissing()
            return
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        authViewModel.loginResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                navigateToDashboard() // Login vai para o Dashboard
            } else {
                // A mensagem de erro será tratada pelo observer de authException
            }
        }

        authViewModel.registrationResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Cadastro de usuário realizado! Agora complete seu perfil.", Toast.LENGTH_LONG).show()
                navigateToUserProfile() // CADASTRO VAI PARA A TELA DE PERFIL
            } else {
                // A mensagem de erro será tratada pelo observer de authException
            }
        }

        authViewModel.authException.observe(this) { exception ->
            exception?.let {
                val message = when (it) {
                    is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado."
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use uma senha mais forte (mínimo 6 caracteres)."
                    is FirebaseAuthInvalidCredentialsException -> "Credenciais inválidas. Verifique o e-mail e a senha."
                    else -> "Ocorreu um erro: ${it.localizedMessage}"
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
                Toast.makeText(this, "Preencha o e-mail e a senha.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.length >= 6) {
                authViewModel.register(email, password)
            } else if (password.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Preencha o e-mail e a senha.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Limpa a back stack
        startActivity(intent)
        finish()
    }

    private fun navigateToUserProfile() {
        val intent = Intent(this, UserProfileActivity::class.java)
        // Não limpa a back stack aqui, para que o usuário possa voltar se precisar (ou se quiserem forçar a conclusão do perfil)
        // Para um fluxo de "cadastro obrigatório de perfil", você não limparia aqui e limpara APENAS no DashboardActivity
        startActivity(intent)
        finish() // Finaliza AuthActivity
    }
}
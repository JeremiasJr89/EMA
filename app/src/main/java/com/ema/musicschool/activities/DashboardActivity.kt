package com.ema.musicschool.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.databinding.ActivityDashboardBinding
import com.ema.musicschool.viewmodels.AuthViewModel
import com.ema.musicschool.viewmodels.DashboardViewModel
import java.util.Locale
import android.widget.Toast
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        authViewModel.loggedInUser.observe(this) { username ->
            if (username == null) {
                // Se o usuário não estiver logado, redireciona para a tela de autenticação
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                binding.tvWelcome.text = "Olá, $username!"
            }
        }

        dashboardViewModel.studyTimeToday.observe(this) { timeInMillis ->
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
            binding.tvStudyTime.text = String.format(Locale.getDefault(), "Tempo de estudo hoje: %d minutos", minutes)
            binding.tvProgressStatus.text = dashboardViewModel.getPointsForStudyTime()

            val progress = (minutes * 100 / 60).toInt().coerceIn(0, 100)
            binding.progressBarStudy.progress = progress
        }

        dashboardViewModel.isStudying.observe(this) { isStudying ->
            if (isStudying) {
                binding.btnToggleStudy.text = "Parar Estudo"
                binding.btnToggleStudy.setBackgroundColor(getColor(android.R.color.holo_red_light))
            } else {
                binding.btnToggleStudy.text = "Iniciar Estudo"
                binding.btnToggleStudy.setBackgroundColor(getColor(com.google.android.material.R.color.design_default_color_primary))
            }
        }
    }

    private fun setupListeners() {
        binding.btnToggleStudy.setOnClickListener {
            if (dashboardViewModel.isStudying.value == true) {
                dashboardViewModel.stopStudySession()
                Toast.makeText(this, "Sessão de estudo encerrada.", Toast.LENGTH_SHORT).show()
            } else {
                dashboardViewModel.startStudySession()
                Toast.makeText(this, "Sessão de estudo iniciada!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPerformances.setOnClickListener {
            val intent = Intent(this, PerformanceActivity::class.java)
            startActivity(intent)
        }

        binding.btnCollaboration.setOnClickListener {
            val intent = Intent(this, CollaborationActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            Toast.makeText(this, "Logout realizado com sucesso.", Toast.LENGTH_SHORT).show()
        }
    }
}
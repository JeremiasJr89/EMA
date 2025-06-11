package com.ema.musicschool.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ema.musicschool.databinding.ActivityDashboardBinding
import com.ema.musicschool.viewmodels.AuthViewModel
import com.ema.musicschool.viewmodels.DashboardViewModel
import java.util.Locale
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ema.musicschool.data.StudyLog
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var studyLogAdapter: StudyLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        studyLogAdapter = StudyLogAdapter(dashboardViewModel)
        binding.rvStudyHistory.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = studyLogAdapter
        }
    }

    private fun setupObservers() {
        authViewModel.currentUser.observe(this) { firebaseUser ->
            if (firebaseUser == null) {
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                dashboardViewModel.updateLoggedInUser(firebaseUser.email ?: "Usuário Desconhecido")
            }
        }

        dashboardViewModel.userFullName.observe(this) { fullName ->
            binding.tvWelcome.text = "Olá, ${fullName}!"
        }

        // NOVO: Observar o tempo de exibição do cronômetro (que zera)
        dashboardViewModel.currentSessionDisplayTime.observe(this) { timeInMillis ->
            binding.tvStudyTime.text =
                "Tempo de estudo: ${dashboardViewModel.formatTime(timeInMillis)}" // Texto do cronômetro
            // A barra de progresso e o status da gamificação serão baseados no tempo total do dia
        }

        // NOVO: Observar o tempo TOTAL acumulado do dia (para gamificação e barra de progresso)
        dashboardViewModel.totalStudyTimeTodayFromFirestore.observe(this) { totalTimeForDay ->
            // Atualizar barra de progresso e status da gamificação com o tempo TOTAL do dia
            val minutes = totalTimeForDay / (1000 * 60)
            val progress = (minutes * 100 / 60).toInt().coerceIn(0, 100)
            binding.progressBarStudy.progress = progress
            binding.tvProgressStatus.text = dashboardViewModel.getPointsForStudyTime()
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

        dashboardViewModel.pastStudyLogs.observe(this) { logs ->
            studyLogAdapter.submitList(logs)
            Log.d(
                "DashboardActivity",
                "Observer de pastStudyLogs acionado. Itens recebidos: ${logs.size}"
            )
        }
    }

    private fun setupListeners() {
        binding.btnToggleStudy.setOnClickListener {
            if (dashboardViewModel.isStudying.value == true) {
                dashboardViewModel.stopStudySession()
                Toast.makeText(
                    this,
                    "Sessão de estudo encerrada e tempo salvo!",
                    Toast.LENGTH_SHORT
                ).show()
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

    inner class StudyLogAdapter(private val dashboardViewModel: DashboardViewModel) :
        RecyclerView.Adapter<StudyLogAdapter.StudyLogViewHolder>() {

        private var logsList: List<StudyLog> = emptyList()

        fun submitList(list: List<StudyLog>) {
            logsList = list
            notifyDataSetChanged()
            Log.d(
                "StudyLogAdapter",
                "submitList chamado. Adapter agora tem ${logsList.size} itens."
            ) // ADICIONE ESTE LOG
            if (logsList.isEmpty()) {
                Log.d("StudyLogAdapter", "submitList recebeu lista vazia. Histórico não aparecerá.")
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyLogViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(com.ema.musicschool.R.layout.item_study_log, parent, false)
            return StudyLogViewHolder(view)
        }

        override fun onBindViewHolder(holder: StudyLogViewHolder, position: Int) {
            val log = logsList[position]
            holder.bind(log)
        }

        override fun getItemCount(): Int = logsList.size

        inner class StudyLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvLogDate: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_log_date)
            private val tvLogTime: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_log_time)

            fun bind(log: StudyLog) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val parsedDate = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(log.date)
                } catch (e: Exception) {
                    null
                }

                if (parsedDate != null) {
                    tvLogDate.text = dateFormat.format(parsedDate)
                } else {
                    tvLogDate.text = log.date
                }

                tvLogTime.text = dashboardViewModel.formatTime(log.totalTimeMillis)
            }
        }
    }
}
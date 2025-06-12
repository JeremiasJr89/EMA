package com.ema.musicschool.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ema.musicschool.R
import com.ema.musicschool.data.StudyLog
import com.ema.musicschool.databinding.ActivityDashboardBinding
import com.ema.musicschool.viewmodels.AuthViewModel
import com.ema.musicschool.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var studyLogAdapter: StudyLogAdapter

    private val FORMATO_DE_DATA_INICIAL = "dd/MM/yyyy"
    private val CHANCE_FORMATO_DE_DATA = "yyyy-MM-dd"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupOnBackPressedCallback()

    }

    // NOVO MÉTODO: Configura o comportamento do botão Voltar
    private fun setupOnBackPressedCallback() {
        val callback = object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@DashboardActivity)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_profile -> {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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
                dashboardViewModel.updateLoggedInUser(firebaseUser.email ?: getString(R.string.ema_strings_usu_rio_desconhecido))
            }
        }

        dashboardViewModel.userFullName.observe(this) { fullName ->
            binding.tvWelcome.text = getString(R.string.ema_strings_ola, fullName)
        }

        dashboardViewModel.currentSessionDisplayTime.observe(this) { timeInMillis ->
            binding.tvStudyTime.text =
                getString(
                    R.string.ema_strings_tempo_de_estudo,
                    dashboardViewModel.formatTime(timeInMillis)
                )
        }

        dashboardViewModel.studyStatusMessage.observe(this) { message ->
            binding.tvProgressStatus.text = message
        }


        dashboardViewModel.totalStudyTimeTodayFromFirestore.observe(this) { totalTimeForDay ->
            val minutes = totalTimeForDay / (1000 * 60)
            val progress = (minutes * 100 / 60).toInt().coerceIn(0, 100)
            binding.progressBarStudy.progress = progress
        }

        dashboardViewModel.isStudying.observe(this) { isStudying ->
            if (isStudying) {
                binding.btnToggleStudy.text = getString(R.string.ema_strings_parar_estudo)
                binding.btnToggleStudy.setBackgroundColor(getColor(android.R.color.holo_red_light))
            } else {
                binding.btnToggleStudy.text = getString(R.string.ema_strings_iniciar_estudo)
                binding.btnToggleStudy.setBackgroundColor(getColor(R.color.ema_green_dark))
            }
        }

        dashboardViewModel.pastStudyLogs.observe(this) { logs ->
            studyLogAdapter.submitList(logs)
        }
    }

    private fun setupListeners() {
        binding.btnToggleStudy.setOnClickListener {
            if (dashboardViewModel.isStudying.value == true) {
                dashboardViewModel.stopStudySession()
                Toast.makeText(
                    this,
                    getString(R.string.ema_strings_sess_o_de_estudo_encerrada_e_tempo_salvo),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                dashboardViewModel.startStudySession()
                Toast.makeText(this,
                    getString(R.string.ema_strings_sess_o_de_estudo_iniciada), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this,
                getString(R.string.ema_strings_logout_realizado_com_sucesso), Toast.LENGTH_SHORT).show()
        }
    }

    inner class StudyLogAdapter(private val dashboardViewModel: DashboardViewModel) :
        RecyclerView.Adapter<StudyLogAdapter.StudyLogViewHolder>() {
        private var logsList: List<StudyLog> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun submitList(list: List<StudyLog>) {
            logsList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyLogViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_study_log, parent, false)
            return StudyLogViewHolder(view)
        }

        override fun onBindViewHolder(holder: StudyLogViewHolder, position: Int) {
            val log = logsList[position]
            holder.bind(log)

        }

        override fun getItemCount(): Int {
            val count = logsList.size
            return count
        }

        inner class StudyLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvLogDate: TextView =
                itemView.findViewById(com.ema.musicschool.R.id.tv_log_date)
            private val tvLogTime: TextView =
                itemView.findViewById(com.ema.musicschool.R.id.tv_log_time)

            fun bind(log: StudyLog) {
                val dateFormat = SimpleDateFormat(FORMATO_DE_DATA_INICIAL, Locale.getDefault())
                val parsedDate = try {
                    SimpleDateFormat(CHANCE_FORMATO_DE_DATA, Locale.getDefault()).parse(log.date)
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
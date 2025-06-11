package com.ema.musicschool.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.ema.musicschool.data.Performance
import com.ema.musicschool.databinding.ActivityPerformanceBinding
import com.ema.musicschool.viewmodels.PerformanceViewModel

class PerformanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerformanceBinding
    private val performanceViewModel: PerformanceViewModel by viewModels()
    private lateinit var performanceAdapter: PerformanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        performanceAdapter = PerformanceAdapter()
        binding.rvPerformances.apply {
            layoutManager = LinearLayoutManager(this@PerformanceActivity)
            adapter = performanceAdapter
        }
    }

    private fun setupObservers() {
        performanceViewModel.performances.observe(this) { performances ->
            performanceAdapter.submitList(performances)
        }
    }

    private fun setupListeners() {
        binding.fabAddPerformance.setOnClickListener {
            showPublishPerformanceDialog()
        }
    }

    private fun showPublishPerformanceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(com.ema.musicschool.R.layout.dialog_publish_performance, null)
        val etTitle = dialogView.findViewById<EditText>(com.ema.musicschool.R.id.et_performance_title)
        val etVideoLink = dialogView.findViewById<EditText>(com.ema.musicschool.R.id.et_video_link)
        val btnPublish = dialogView.findViewById<Button>(com.ema.musicschool.R.id.btn_publish_performance)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Publicar Nova Performance")
            .setView(dialogView)
            .create()

        btnPublish.setOnClickListener {
            val title = etTitle.text.toString()
            val videoLink = etVideoLink.text.toString()

            if (title.isNotEmpty() && videoLink.isNotEmpty()) {
                performanceViewModel.publishPerformance(title, videoLink)
                Toast.makeText(this, "Performance publicada!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    inner class PerformanceAdapter : RecyclerView.Adapter<PerformanceAdapter.PerformanceViewHolder>() {

        private var performancesList: MutableList<Performance> = mutableListOf()

        fun submitList(list: MutableList<Performance>) {
            performancesList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerformanceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(com.ema.musicschool.R.layout.item_performance, parent, false)
            return PerformanceViewHolder(view)
        }

        override fun onBindViewHolder(holder: PerformanceViewHolder, position: Int) {
            val performance = performancesList[position]
            holder.bind(performance)
        }

        override fun getItemCount(): Int = performancesList.size

        inner class PerformanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_performance_title)
            private val tvUploader: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_performance_uploader)
            private val tvVideoLink: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_performance_video_link)

            fun bind(performance: Performance) {
                tvTitle.text = performance.title
                tvUploader.text = "Por: ${performance.username}"
                tvVideoLink.text = "Link: ${performance.videoLink}"
            }
        }
    }
}
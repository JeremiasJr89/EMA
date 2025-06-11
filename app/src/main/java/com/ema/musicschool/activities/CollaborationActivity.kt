package com.ema.musicschool.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ema.musicschool.data.Message
import com.ema.musicschool.data.StudyGroup
import com.ema.musicschool.databinding.ActivityCollaborationBinding
import com.ema.musicschool.viewmodels.CollaborationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CollaborationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollaborationBinding
    private val collaborationViewModel: CollaborationViewModel by viewModels()
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollaborationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGroupRecyclerView()
        setupMessageRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupGroupRecyclerView() {
        groupAdapter = GroupAdapter { group ->
            collaborationViewModel.selectGroup(group.id)
        }
        binding.rvStudyGroups.apply {
            layoutManager = LinearLayoutManager(this@CollaborationActivity)
            adapter = groupAdapter
        }
    }

    private fun setupMessageRecyclerView() {
        // Passar o ViewModel para o MessageAdapter
        messageAdapter = MessageAdapter(collaborationViewModel)
        binding.rvGroupChat.apply {
            layoutManager = LinearLayoutManager(this@CollaborationActivity)
            adapter = messageAdapter
        }
    }

    private fun setupObservers() {
        collaborationViewModel.studyGroups.observe(this) { groups ->
            groupAdapter.submitList(groups)
        }

        collaborationViewModel.currentGroupId.observe(this) { groupId ->
            if (groupId != null) {
                binding.llGroupChat.visibility = View.VISIBLE
                binding.tvSelectGroupPrompt.visibility = View.GONE
                val group = collaborationViewModel.studyGroups.value?.find { it.id == groupId }
                binding.tvCurrentGroupName.text = group?.name ?: "Grupo Selecionado"
            } else {
                binding.llGroupChat.visibility = View.GONE
                binding.tvSelectGroupPrompt.visibility = View.VISIBLE
            }
        }

        collaborationViewModel.currentGroupMessages.observe(this) { messages ->
            messageAdapter.submitList(messages)
            binding.rvGroupChat.scrollToPosition(messages.size - 1)
        }
    }

    private fun setupListeners() {
        binding.btnSendMessage.setOnClickListener {
            val messageContent = binding.etMessageInput.text.toString()
            val currentGroupId = collaborationViewModel.currentGroupId.value
            if (messageContent.isNotEmpty() && currentGroupId != null) {
                collaborationViewModel.postMessage(currentGroupId, messageContent)
                binding.etMessageInput.text?.clear()
            } else if (currentGroupId == null) {
                Toast.makeText(this, "Selecione um grupo para enviar mensagens.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Adaptador para RecyclerView de Grupos de Estudo (mantém-se o mesmo)
    inner class GroupAdapter(private val onGroupClick: (StudyGroup) -> Unit) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {
        private var groupsList: MutableList<StudyGroup> = mutableListOf()
        fun submitList(list: MutableList<StudyGroup>) {
            groupsList = list
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(com.ema.musicschool.R.layout.item_study_group, parent, false)
            return GroupViewHolder(view)
        }
        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            val group = groupsList[position]
            holder.bind(group, onGroupClick)
        }
        override fun getItemCount(): Int = groupsList.size
        inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvGroupName: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_group_name)
            private val tvGroupDescription: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_group_description)
            private val btnJoinGroup: Button = itemView.findViewById(com.ema.musicschool.R.id.btn_join_group)
            private val tvMemberCount: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_member_count)
            fun bind(group: StudyGroup, onGroupClick: (StudyGroup) -> Unit) {
                tvGroupName.text = group.name
                tvGroupDescription.text = group.description
                tvMemberCount.text = "Membros: ${group.members.size}"
                if (collaborationViewModel.isUserInGroup(group.id)) {
                    btnJoinGroup.text = "Ver Grupo"
                    btnJoinGroup.isEnabled = true
                    btnJoinGroup.setBackgroundColor(itemView.context.getColor(com.google.android.material.R.color.design_default_color_primary))
                } else {
                    btnJoinGroup.text = "Entrar no Grupo"
                    btnJoinGroup.isEnabled = true
                    btnJoinGroup.setBackgroundColor(itemView.context.getColor(com.google.android.material.R.color.design_default_color_secondary))
                }
                btnJoinGroup.setOnClickListener {
                    if (collaborationViewModel.isUserInGroup(group.id)) {
                        onGroupClick(group)
                    } else {
                        collaborationViewModel.joinGroup(group.id)
                        Toast.makeText(itemView.context, "Você entrou no grupo '${group.name}'!", Toast.LENGTH_SHORT).show()
                        notifyDataSetChanged()
                        onGroupClick(group)
                    }
                }
            }
        }
    }

    // Adaptador para RecyclerView de Mensagens
    inner class MessageAdapter(private val collaborationViewModel: CollaborationViewModel) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

        private var messagesList: MutableList<Message> = mutableListOf()

        fun submitList(list: MutableList<Message>) {
            messagesList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(com.ema.musicschool.R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messagesList[position]
            holder.bind(message)
        }

        override fun getItemCount(): Int = messagesList.size

        inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvSender: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_message_sender)
            private val tvContent: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_message_content)
            private val tvTimestamp: TextView = itemView.findViewById(com.ema.musicschool.R.id.tv_message_timestamp)

            fun bind(message: Message) {
                // Priorize o senderName do objeto Message se ele já estiver preenchido
                if (message.senderName.isNotEmpty()) {
                    tvSender.text = message.senderName
                } else {
                    // Se senderName estiver vazio, busque o nome do perfil pelo senderId
                    // Isso ocorrerá para mensagens estáticas ou as que não foram salvas com senderName
                    collaborationViewModel.getSenderNameForDisplay(message.senderId) { name ->
                        tvSender.text = name
                    }
                }

                tvContent.text = message.content
                val dateFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
                tvTimestamp.text = dateFormat.format(message.timestamp ?: Date()) // Use Date() como fallback
            }
        }
    }
}
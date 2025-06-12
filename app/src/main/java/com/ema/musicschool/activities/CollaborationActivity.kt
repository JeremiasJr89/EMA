package com.ema.musicschool.activities

import android.annotation.SuppressLint
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
import com.ema.musicschool.R
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
                binding.tvCurrentGroupName.text = group?.name ?: getString(R.string.ema_strings_grupo_selecionado)
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
                Toast.makeText(this,
                    getString(R.string.ema_strings_selecione_um_grupo_para_enviar_mensagens), Toast.LENGTH_SHORT).show()
            }
        }
    }
    inner class GroupAdapter(private val onGroupClick: (StudyGroup) -> Unit) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {
        private var groupsList: MutableList<StudyGroup> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        fun submitList(list: MutableList<StudyGroup>) {
            groupsList = list
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_study_group, parent, false)
            return GroupViewHolder(view)
        }
        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            val group = groupsList[position]
            holder.bind(group, onGroupClick)
        }
        override fun getItemCount(): Int = groupsList.size
        inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
            private val tvGroupDescription: TextView = itemView.findViewById(R.id.tv_group_description)
            private val btnJoinGroup: Button = itemView.findViewById(R.id.btn_join_group)
            private val tvMemberCount: TextView = itemView.findViewById(R.id.tv_member_count)
            @SuppressLint("StringFormatMatches", "NotifyDataSetChanged")
            fun bind(group: StudyGroup, onGroupClick: (StudyGroup) -> Unit) {
                tvGroupName.text = group.name
                tvGroupDescription.text = group.description
                tvMemberCount.text = getString(R.string.ema_strings_membros, group.members.size)
                if (collaborationViewModel.isUserInGroup(group.id)) {
                    btnJoinGroup.text = getString(R.string.ema_strings_ver_grupo)
                    btnJoinGroup.isEnabled = true
                    btnJoinGroup.setBackgroundColor(itemView.context.getColor(com.google.android.material.R.color.design_default_color_primary))
                } else {
                    btnJoinGroup.text = R.string.ema_strings_entrar_no_grupo.toString()
                    btnJoinGroup.isEnabled = true
                    btnJoinGroup.setBackgroundColor(itemView.context.getColor(com.google.android.material.R.color.design_default_color_secondary))
                }
                btnJoinGroup.setOnClickListener {
                    if (collaborationViewModel.isUserInGroup(group.id)) {
                        onGroupClick(group)
                    } else {
                        collaborationViewModel.joinGroup(group.id)
                        Toast.makeText(itemView.context,
                            getString(R.string.ema_strings_voc_entrou_no_grupo, group.name), Toast.LENGTH_SHORT).show()
                        notifyDataSetChanged()
                        onGroupClick(group)
                    }
                }
            }
        }
    }

    inner class MessageAdapter(private val collaborationViewModel: CollaborationViewModel) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

        private var messagesList: MutableList<Message> = mutableListOf()

        @SuppressLint("NotifyDataSetChanged")
        fun submitList(list: MutableList<Message>) {
            messagesList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messagesList[position]
            holder.bind(message)
        }

        override fun getItemCount(): Int = messagesList.size

        inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvSender: TextView = itemView.findViewById(R.id.tv_message_sender)
            private val tvContent: TextView = itemView.findViewById(R.id.tv_message_content)
            private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_message_timestamp)

            fun bind(message: Message) {
                tvSender.text = message.senderName
                tvContent.text = message.content
                val dateFormat = SimpleDateFormat(R.string.ema_strings_hh_mm_dd_mm.toString(), Locale.getDefault())
                tvTimestamp.text = dateFormat.format(message.timestamp ?: Date())
            }
        }
    }
}
package com.studycheck.student.ui.feedback

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.Feedback
import com.studycheck.student.databinding.ActivityFeedbackBinding
import com.studycheck.student.databinding.ItemFeedbackBinding
import com.studycheck.student.network.ApiClient
import kotlinx.coroutines.launch

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private lateinit var adapter: FeedbackAdapter
    private val feedbackList = mutableListOf<Feedback>()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAdmin = App.instance.prefs.isAdmin()

        binding.toolbar.title = if (isAdmin) "反馈管理" else "意见反馈"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupViews()
        loadFeedback()
    }

    private fun setupViews() {
        adapter = FeedbackAdapter(feedbackList, isAdmin) { feedback ->
            showReplyDialog(feedback)
        }

        binding.rvMyFeedback.layoutManager = LinearLayoutManager(this)
        binding.rvMyFeedback.adapter = adapter

        // 管理员不显示提交表单
        if (isAdmin) {
            binding.cardSubmit.visibility = View.GONE
            binding.tvRecordTitle.text = "所有用户反馈"
        } else {
            binding.btnSubmit.setOnClickListener {
                submitFeedback()
            }
        }
    }

    private fun submitFeedback() {
        val content = binding.etContent.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入反馈内容", Toast.LENGTH_SHORT).show()
            return
        }

        val categories = listOf("功能建议", "Bug反馈", "体验问题", "其他")
        val category = categories[binding.chipGroupCategory.checkedChipId.let { id ->
            binding.chipGroupCategory.findViewById<View>(id)?.let { v ->
                categories.indexOf((v as com.google.android.material.chip.Chip).text.toString())
            } ?: 0
        }]

        val contact = binding.etContact.text.toString().trim()

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.submitFeedback(
                    mapOf(
                        "category" to category,
                        "content" to content,
                        "contact" to contact
                    )
                )
                if (response.code == 200) {
                    Toast.makeText(this@FeedbackActivity, "反馈提交成功", Toast.LENGTH_SHORT).show()
                    binding.etContent.text?.clear()
                    binding.etContact.text?.clear()
                    loadFeedback()
                } else {
                    Toast.makeText(this@FeedbackActivity, response.msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackActivity, "提交失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFeedback() {
        lifecycleScope.launch {
            try {
                val response = if (isAdmin) {
                    ApiClient.apiService.getAllFeedback()
                } else {
                    ApiClient.apiService.getMyFeedback()
                }

                if (response.code == 200 && response.data != null) {
                    feedbackList.clear()
                    feedbackList.addAll(response.data)
                    adapter.notifyDataSetChanged()

                    binding.tvEmpty.visibility = if (feedbackList.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReplyDialog(feedback: Feedback) {
        val edit = android.widget.EditText(this)
        edit.hint = "输入回复内容..."
        edit.setText(feedback.admin_reply ?: "")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("回复 - ${feedback.nickname ?: feedback.username ?: "用户"}")
            .setView(edit)
            .setPositiveButton("回复") { _, _ ->
                val reply = edit.text.toString().trim()
                if (reply.isNotEmpty()) {
                    replyFeedback(feedback.id, reply)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun replyFeedback(id: Int, reply: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.replyFeedback(id, mapOf("reply" to reply))
                if (response.code == 200) {
                    Toast.makeText(this@FeedbackActivity, "回复成功", Toast.LENGTH_SHORT).show()
                    loadFeedback()
                } else {
                    Toast.makeText(this@FeedbackActivity, response.msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackActivity, "回复失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class FeedbackAdapter(
    private val items: List<Feedback>,
    private val isAdmin: Boolean,
    private val onReply: (Feedback) -> Unit
) : RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFeedbackBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedbackBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding

        b.tvCategory.text = item.category
        b.tvContent.text = item.content
        b.tvTime.text = item.created_at?.substring(0, 10) ?: ""

        val statusText = when (item.status) {
            "replied" -> "已回复"
            "processing" -> "处理中"
            "resolved" -> "已解决"
            else -> "待处理"
        }
        b.tvStatus.text = statusText

        if (!item.admin_reply.isNullOrEmpty()) {
            b.layoutReply.visibility = View.VISIBLE
            b.tvReply.text = item.admin_reply
        } else {
            b.layoutReply.visibility = View.GONE
        }

        if (isAdmin) {
            b.layoutAdminInfo.visibility = View.VISIBLE
            b.tvUsername.text = "用户：${item.nickname ?: item.username ?: "未知"}"
            b.btnReply.setOnClickListener { onReply(item) }
        } else {
            b.layoutAdminInfo.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}

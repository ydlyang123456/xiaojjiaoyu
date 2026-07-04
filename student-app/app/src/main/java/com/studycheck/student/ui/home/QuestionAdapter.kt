package com.studycheck.student.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.studycheck.student.R
import com.studycheck.student.data.Question
import com.studycheck.student.databinding.ItemQuestionBinding
import com.studycheck.student.network.ApiClient

class QuestionAdapter(
    private val questions: List<Question>
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: Question) {
            val imageUrl = if (question.image_url.startsWith("http")) {
                question.image_url
            } else {
                ApiClient.retrofit.baseUrl().toString().removeSuffix("/") + question.image_url
            }

            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.ivQuestion)

            binding.tvSubject.text = question.subject ?: "未知学科"
            binding.tvTime.text = question.created_at ?: ""
            binding.tvStatus.text = when (question.status) {
                "pending" -> "待解答"
                "answered" -> "已解答"
                else -> question.status
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size
}

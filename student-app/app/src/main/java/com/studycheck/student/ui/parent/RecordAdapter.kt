package com.studycheck.student.ui.parent

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.studycheck.student.R
import com.studycheck.student.data.SearchRecord
import com.studycheck.student.databinding.ItemParentSearchRecordBinding
import com.studycheck.student.network.ApiClient

class RecordAdapter(
    private val records: List<SearchRecord>,
    private val onItemClick: (SearchRecord) -> Unit
) : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemParentSearchRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: SearchRecord) {
            binding.tvSubject.text = record.subject ?: "未知学科"
            binding.tvDifficulty.text = "难度：${record.difficulty ?: "未知"}"
            binding.tvTime.text = record.created_at ?: ""

            val preview = record.query_text?.take(50) ?: "(图片题目)"
            binding.tvPreview.text = preview

            // 加载缩略图
            if (!record.query_image_url.isNullOrEmpty()) {
                val imageUrl = if (record.query_image_url.startsWith("http")) {
                    record.query_image_url
                } else {
                    ApiClient.retrofit.baseUrl().toString().removeSuffix("/") + record.query_image_url
                }
                binding.ivThumbnail.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParentSearchRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size
}

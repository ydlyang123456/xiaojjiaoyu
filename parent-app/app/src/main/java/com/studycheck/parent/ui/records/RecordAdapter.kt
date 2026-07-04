package com.studycheck.parent.ui.records

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.studycheck.parent.data.SearchRecord
import com.studycheck.parent.databinding.ItemSearchRecordBinding

class RecordAdapter(
    private val records: List<SearchRecord>,
    private val onItemClick: (SearchRecord) -> Unit
) : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSearchRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: SearchRecord) {
            binding.tvSubject.text = record.subject ?: "未知学科"
            binding.tvDifficulty.text = "难度：${record.difficulty ?: "未知"}"
            binding.tvTime.text = record.created_at ?: ""

            val preview = record.query_text?.take(50) ?: "(图片题目)"
            binding.tvPreview.text = preview

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchRecordBinding.inflate(
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

package com.studycheck.parent.ui.analysis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.studycheck.parent.data.AIAnalysis
import com.studycheck.parent.databinding.ItemAnalysisHistoryBinding

class AnalysisHistoryAdapter(
    private val analyses: List<AIAnalysis>,
    private val onItemClick: (AIAnalysis) -> Unit
) : RecyclerView.Adapter<AnalysisHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAnalysisHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(analysis: AIAnalysis) {
            binding.tvTitle.text = "综合学习分析报告"
            binding.tvTime.text = analysis.created_at ?: ""
            binding.tvType.text = analysis.analysis_type

            binding.root.setOnClickListener {
                onItemClick(analysis)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalysisHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(analyses[position])
    }

    override fun getItemCount(): Int = analyses.size
}

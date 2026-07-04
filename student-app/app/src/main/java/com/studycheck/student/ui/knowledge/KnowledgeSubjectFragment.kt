package com.studycheck.student.ui.knowledge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.studycheck.student.R
import com.studycheck.student.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KnowledgeSubjectFragment : Fragment() {

    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_knowledge_subject, container, false)
            initViews()
        }
        return rootView
    }

    private fun initViews() {
        val view = rootView ?: return

        val etQuery = view.findViewById<TextInputEditText>(R.id.etKnowledgeQuery)
        val btnSearch = view.findViewById<MaterialButton>(R.id.btnSearch)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressKnowledge)
        val tvResult = view.findViewById<TextView>(R.id.tvKnowledgeResult)
        val cardResult = view.findViewById<CardView>(R.id.cardResult)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)

        val chipIds = listOf(
            R.id.chipGougu to "勾股定理",
            R.id.chipNiudun to "牛顿第一定律",
            R.id.chipYuanSu to "元素周期表",
            R.id.chipCell to "细胞结构"
        )

        chipIds.forEach { (id, text) ->
            view.findViewById<Chip>(id)?.setOnClickListener {
                etQuery.setText(text)
                queryKnowledge(text, progressBar, tvResult, cardResult, emptyState)
            }
        }

        btnSearch.setOnClickListener {
            val query = etQuery.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "请输入要查询的内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            queryKnowledge(query, progressBar, tvResult, cardResult, emptyState)
        }
    }

    private fun queryKnowledge(
        query: String,
        progressBar: ProgressBar,
        tvResult: TextView,
        cardResult: CardView,
        emptyState: LinearLayout
    ) {
        progressBar.visibility = View.VISIBLE
        cardResult.visibility = View.GONE
        emptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.queryKnowledge(query)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.code == 200 && response.data != null) {
                        tvResult.text = response.data.content
                        cardResult.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), response.msg ?: "查询失败", Toast.LENGTH_SHORT).show()
                        emptyState.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                    emptyState.visibility = View.VISIBLE
                }
            }
        }
    }
}

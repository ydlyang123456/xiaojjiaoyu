package com.studycheck.student.ui.search

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.SearchRecord
import com.studycheck.student.databinding.ActivitySearchDetailBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchDetailBinding
    private var recordId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(binding.btnBack)

        recordId = intent.getIntExtra("record_id", 0)

        binding.btnBack.setOnClickListener { finish() }

        loadDetail()
    }

    private fun loadDetail() {
        showLoading(true)

        ApiClient.apiService.getSearchRecord(recordId).enqueue(object : Callback<ApiResponse<SearchRecord>> {
            override fun onResponse(call: Call<ApiResponse<SearchRecord>>, response: Response<ApiResponse<SearchRecord>>) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    displayDetail(body.data)
                } else {
                    Toast.makeText(this@SearchDetailActivity, body?.msg ?: "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<SearchRecord>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@SearchDetailActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayDetail(record: SearchRecord) {
        if (!record.query_image_url.isNullOrEmpty()) {
            val imageUrl = if (record.query_image_url.startsWith("http")) {
                record.query_image_url
            } else {
                ApiClient.retrofit.baseUrl().toString().removeSuffix("/") + record.query_image_url
            }
            binding.ivQuestion.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .fitCenter()
                .into(binding.ivQuestion)
        } else {
            binding.ivQuestion.visibility = View.GONE
        }

        if (!record.query_text.isNullOrEmpty()) {
            binding.tvQuery.text = record.query_text
            binding.tvQuery.visibility = View.VISIBLE
        } else {
            binding.tvQuery.visibility = View.GONE
        }

        binding.tvSubject.text = "学科：${record.subject ?: "未知"}"
        binding.tvDifficulty.text = "难度：${record.difficulty ?: "未知"}"
        binding.tvTime.text = "时间：${record.created_at ?: ""}"

        binding.tvAnswer.text = record.ai_answer ?: "暂无解答"

        if (!record.knowledge_points.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(record.knowledge_points)
                val points = StringBuilder()
                for (i in 0 until jsonArray.length()) {
                    points.append("${i + 1}. ${jsonArray.getString(i)}\n")
                }
                binding.tvKnowledgePoints.text = points.toString().trim()
            } catch (e: Exception) {
                binding.tvKnowledgePoints.text = record.knowledge_points
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}

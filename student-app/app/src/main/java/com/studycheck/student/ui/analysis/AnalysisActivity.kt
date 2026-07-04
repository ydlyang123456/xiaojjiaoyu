package com.studycheck.student.ui.analysis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.AIAnalysis
import com.studycheck.student.data.PaginatedResponse
import com.studycheck.student.databinding.ActivityAnalysisBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private var studentId: Int = 0
    private var selectedDays = 30

    private val daysOptions = arrayOf("最近7天", "最近15天", "最近30天", "最近90天")
    private val daysValues = intArrayOf(7, 15, 30, 90)

    private val analysisList = mutableListOf<AIAnalysis>()
    private lateinit var adapter: AnalysisHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(binding.btnBack, binding.btnGenerateAnalysis)

        studentId = intent.getIntExtra("student_id", App.instance.prefs.currentStudentId)

        setupViews()
        loadHistory()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTime.adapter = spinnerAdapter
        binding.spinnerTime.setSelection(2)

        binding.spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDays = daysValues[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDays = 30
            }
        }

        binding.btnGenerateAnalysis.setOnClickListener {
            generateAnalysis()
        }

        adapter = AnalysisHistoryAdapter(analysisList) { analysis ->
            val intent = Intent(this, AnalysisDetailActivity::class.java)
            intent.putExtra("analysis_id", analysis.id)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadHistory()
        }
    }

    private fun generateAnalysis() {
        showLoading(true)

        val body = mapOf(
            "student_id" to studentId,
            "days" to selectedDays,
            "analysis_type" to "comprehensive"
        )

        ApiClient.apiService.generateAnalysis(body).enqueue(object : Callback<ApiResponse<AIAnalysis>> {
            override fun onResponse(call: Call<ApiResponse<AIAnalysis>>, response: Response<ApiResponse<AIAnalysis>>) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    Toast.makeText(this@AnalysisActivity, "分析生成成功", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@AnalysisActivity, AnalysisDetailActivity::class.java)
                    intent.putExtra("analysis_id", body.data.id)
                    startActivity(intent)
                    loadHistory()
                } else {
                    Toast.makeText(this@AnalysisActivity, body?.msg ?: "分析失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<AIAnalysis>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AnalysisActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadHistory() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.apiService.getAnalysisHistory(1, 10)
            .enqueue(object : Callback<ApiResponse<PaginatedResponse<AIAnalysis>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PaginatedResponse<AIAnalysis>>>,
                    response: Response<ApiResponse<PaginatedResponse<AIAnalysis>>>
                ) {
                    binding.swipeRefresh.isRefreshing = false
                    val body = response.body()
                    if (body != null && body.code == 200 && body.data != null) {
                        analysisList.clear()
                        analysisList.addAll(body.data.items)
                        adapter.notifyDataSetChanged()

                        if (analysisList.isEmpty()) {
                            binding.emptyView.visibility = View.VISIBLE
                        } else {
                            binding.emptyView.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PaginatedResponse<AIAnalysis>>>, t: Throwable) {
                    binding.swipeRefresh.isRefreshing = false
                }
            })
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGenerateAnalysis.isEnabled = !show
    }
}

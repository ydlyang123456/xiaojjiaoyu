package com.studycheck.student.ui.analysis

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.AIAnalysis
import com.studycheck.student.databinding.ActivityAnalysisDetailBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class AnalysisDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisDetailBinding
    private var analysisId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(binding.btnBack)

        analysisId = intent.getIntExtra("analysis_id", 0)

        binding.btnBack.setOnClickListener { finish() }

        loadAnalysis()
    }

    private fun loadAnalysis() {
        showLoading(true)

        ApiClient.apiService.getAnalysis(analysisId).enqueue(object : Callback<ApiResponse<AIAnalysis>> {
            override fun onResponse(call: Call<ApiResponse<AIAnalysis>>, response: Response<ApiResponse<AIAnalysis>>) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    displayAnalysis(body.data)
                } else {
                    Toast.makeText(this@AnalysisDetailActivity, body?.msg ?: "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<AIAnalysis>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AnalysisDetailActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayAnalysis(analysis: AIAnalysis) {
        binding.tvOverall.text = analysis.overall_evaluation ?: "暂无评估"
        binding.tvStudyHabits.text = analysis.study_habits ?: "暂无分析"

        if (!analysis.weak_points.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(analysis.weak_points)
                val sb = StringBuilder()
                for (i in 0 until jsonArray.length()) {
                    sb.append("${i + 1}. ${jsonArray.getString(i)}\n")
                }
                binding.tvWeakPoints.text = sb.toString().trim()
            } catch (e: Exception) {
                binding.tvWeakPoints.text = analysis.weak_points
            }
        }

        if (!analysis.strong_points.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(analysis.strong_points)
                val sb = StringBuilder()
                for (i in 0 until jsonArray.length()) {
                    sb.append("${i + 1}. ${jsonArray.getString(i)}\n")
                }
                binding.tvStrongPoints.text = sb.toString().trim()
            } catch (e: Exception) {
                binding.tvStrongPoints.text = analysis.strong_points
            }
        }

        if (!analysis.suggestions.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(analysis.suggestions)
                val sb = StringBuilder()
                for (i in 0 until jsonArray.length()) {
                    sb.append("${i + 1}. ${jsonArray.getString(i)}\n")
                }
                binding.tvSuggestions.text = sb.toString().trim()
            } catch (e: Exception) {
                binding.tvSuggestions.text = analysis.suggestions
            }
        }

        if (!analysis.statistics_data.isNullOrEmpty()) {
            try {
                val stats = JSONObject(analysis.statistics_data)
                setupPieChart(binding.pieChart, stats)
            } catch (e: Exception) {
                binding.pieChart.visibility = View.GONE
            }
        }
    }

    private fun setupPieChart(pieChart: PieChart, stats: JSONObject) {
        val subjectStats = stats.optJSONObject("subject_stats")
        if (subjectStats == null || subjectStats.length() == 0) {
            pieChart.visibility = View.GONE
            return
        }

        val entries = mutableListOf<PieEntry>()
        val keys = subjectStats.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = subjectStats.getInt(key).toFloat()
            entries.add(PieEntry(value, key))
        }

        val dataSet = PieDataSet(entries, "学科分布")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = android.graphics.Color.WHITE

        val data = PieData(dataSet)
        pieChart.data = data

        val description = Description()
        description.text = "学科搜题分布"
        pieChart.description = description

        pieChart.setUsePercentValues(true)
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.setEntryLabelTextSize(12f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
    }
}

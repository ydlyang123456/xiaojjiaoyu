package com.studycheck.parent.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.studycheck.parent.App
import com.studycheck.parent.R
import com.studycheck.parent.data.ApiResponse
import com.studycheck.parent.data.PaginatedResponse
import com.studycheck.parent.data.SearchRecord
import com.studycheck.parent.data.StudentRelation
import com.studycheck.parent.databinding.FragmentHomeBinding
import com.studycheck.parent.network.ApiClient
import com.studycheck.parent.ui.analysis.AnalysisActivity
import com.studycheck.parent.ui.analysis.AnalysisDetailActivity
import com.studycheck.parent.ui.bind.BindStudentActivity
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var studentList = listOf<StudentRelation>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        loadStudents()
    }

    private fun setupViews() {
        val nickname = App.instance.prefs.nickname ?: "家长"
        binding.tvGreeting.text = "你好，$nickname"

        binding.btnBindStudent.setOnClickListener {
            startActivity(Intent(requireContext(), BindStudentActivity::class.java))
        }

        binding.btnAIAnalysis.setOnClickListener {
            val studentId = App.instance.prefs.currentStudentId
            if (studentId > 0) {
                val intent = Intent(requireContext(), AnalysisActivity::class.java)
                intent.putExtra("student_id", studentId)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "请先绑定学生", Toast.LENGTH_SHORT).show()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadStudents()
        }
    }

    private fun loadStudents() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.apiService.getMyStudents().enqueue(object : Callback<ApiResponse<List<StudentRelation>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<StudentRelation>>>,
                response: Response<ApiResponse<List<StudentRelation>>>
            ) {
                binding.swipeRefresh.isRefreshing = false
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    studentList = body.data
                    updateStudentInfo()
                    loadStatistics()
                } else {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.studentInfoLayout.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<StudentRelation>>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStudentInfo() {
        if (studentList.isNotEmpty()) {
            val firstStudent = studentList[0].student
            if (firstStudent != null) {
                App.instance.prefs.currentStudentId = firstStudent.id
                App.instance.prefs.currentStudentName = firstStudent.nickname ?: firstStudent.username

                binding.tvStudentName.text = firstStudent.nickname ?: firstStudent.username
                binding.tvStudentGrade.text = "年级：${firstStudent.grade ?: "未设置"}"
                binding.studentInfoLayout.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            }
        } else {
            binding.studentInfoLayout.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        }
    }

    private fun loadStatistics() {
        val studentId = App.instance.prefs.currentStudentId
        if (studentId <= 0) return

        ApiClient.apiService.getSearchRecords(1, 100, studentId)
            .enqueue(object : Callback<ApiResponse<PaginatedResponse<SearchRecord>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PaginatedResponse<SearchRecord>>>,
                    response: Response<ApiResponse<PaginatedResponse<SearchRecord>>>
                ) {
                    val body = response.body()
                    if (body != null && body.code == 200 && body.data != null) {
                        val records = body.data.items
                        binding.tvTotalSearches.text = body.data.total.toString()

                        val subjects = HashSet<String>()
                        var difficultCount = 0
                        for (record in records) {
                            record.subject?.let { subjects.add(it) }
                            if (record.difficulty == "困难") difficultCount++
                        }
                        binding.tvSubjectCount.text = subjects.size.toString()
                        binding.tvDifficultCount.text = difficultCount.toString()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PaginatedResponse<SearchRecord>>>, t: Throwable) {
                    // 静默失败
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            loadStudents()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

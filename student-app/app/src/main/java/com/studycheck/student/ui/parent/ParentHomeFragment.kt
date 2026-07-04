package com.studycheck.student.ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.PaginatedResponse
import com.studycheck.student.data.SearchRecord
import com.studycheck.student.data.StudentRelation
import com.studycheck.student.databinding.FragmentParentHomeBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.ui.analysis.AnalysisActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ParentHomeFragment : Fragment() {

    private var _binding: FragmentParentHomeBinding? = null
    private val binding get() = _binding!!

    private var studentList = listOf<StudentRelation>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentHomeBinding.inflate(inflater, container, false)
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getMyStudents()
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    if (response.code == 200 && response.data != null) {
                        studentList = response.data
                        updateStudentInfo()
                        loadStatistics()
                    } else {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.studentInfoLayout.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        ApiClient.apiService.getSearchRecords(1, 100)
            .enqueue(object : Callback<ApiResponse<PaginatedResponse<SearchRecord>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PaginatedResponse<SearchRecord>>>,
                    response: Response<ApiResponse<PaginatedResponse<SearchRecord>>>
                ) {
                    val body = response.body()
                    if (body != null && body.code == 200 && body.data != null) {
                        val records = body.data.items
                        binding.tvTotalSearches.text = body.data.total.toString()

                        val subjects = hashSetOf<String>()
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

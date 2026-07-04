package com.studycheck.student.ui.parent

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studycheck.student.R
import com.studycheck.student.data.StudentRelation
import com.studycheck.student.databinding.ActivityBindStudentBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BindStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBindStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBindStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(binding.btnBack, binding.btnBind)

        setupViews()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnBind.setOnClickListener {
            doBind()
        }
    }

    private fun doBind() {
        val studentUsername = binding.etStudentUsername.text.toString().trim()
        val relationType = binding.etRelation.text.toString().trim()

        if (studentUsername.isEmpty()) {
            Toast.makeText(this, "请输入学生用户名", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val body = mutableMapOf(
            "student_username" to studentUsername
        )
        if (relationType.isNotEmpty()) {
            body["relation_type"] = relationType
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.bindStudent(body)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.code == 200) {
                        Toast.makeText(this@BindStudentActivity, "绑定成功", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@BindStudentActivity, response.msg ?: "绑定失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@BindStudentActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnBind.isEnabled = !show
    }
}

package com.studycheck.parent.ui.bind

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.parent.R
import com.studycheck.parent.data.ApiResponse
import com.studycheck.parent.data.StudentRelation
import com.studycheck.parent.databinding.ActivityBindStudentBinding
import com.studycheck.parent.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BindStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBindStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBindStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        ApiClient.apiService.bindStudent(body).enqueue(object : Callback<ApiResponse<StudentRelation>> {
            override fun onResponse(
                call: Call<ApiResponse<StudentRelation>>,
                response: Response<ApiResponse<StudentRelation>>
            ) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200) {
                    Toast.makeText(this@BindStudentActivity, "绑定成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@BindStudentActivity, body?.msg ?: "绑定失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<StudentRelation>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@BindStudentActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnBind.isEnabled = !show
    }
}

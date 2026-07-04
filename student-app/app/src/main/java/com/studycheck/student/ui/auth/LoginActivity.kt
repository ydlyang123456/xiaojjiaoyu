package com.studycheck.student.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.LoginResponse
import com.studycheck.student.databinding.ActivityLoginBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.ui.MainActivity
import com.studycheck.student.util.TouchFeedback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isRegister = false
    private var selectedRole = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        TouchFeedback.applyAll(binding.btnLogin, binding.btnRoleStudent, binding.btnRoleParent)

        binding.btnLogin.setOnClickListener {
            if (isRegister) {
                doRegister()
            } else {
                doLogin()
            }
        }

        binding.tvToggle.setOnClickListener {
            isRegister = !isRegister
            if (isRegister) {
                binding.tvTitle.text = "学生注册"
                binding.btnLogin.text = "注册"
                binding.tvToggle.text = "已有账号？去登录"
                binding.etNickname.visibility = View.VISIBLE
                binding.etGrade.visibility = View.VISIBLE
            } else {
                binding.tvTitle.text = "学生登录"
                binding.btnLogin.text = "登录"
                binding.tvToggle.text = "没有账号？去注册"
                binding.etNickname.visibility = View.GONE
                binding.etGrade.visibility = View.GONE
            }
        }

        binding.btnRoleStudent.setOnClickListener {
            selectedRole = "student"
            binding.btnRoleStudent.setBackgroundColor(getColor(R.color.primary))
            binding.btnRoleStudent.setTextColor(getColor(android.R.color.white))
            binding.btnRoleParent.setBackgroundColor(getColor(android.R.color.white))
            binding.btnRoleParent.setTextColor(getColor(R.color.text_primary))
            binding.tilGrade.visibility = View.VISIBLE
        }

        binding.btnRoleParent.setOnClickListener {
            selectedRole = "parent"
            binding.btnRoleParent.setBackgroundColor(getColor(R.color.primary))
            binding.btnRoleParent.setTextColor(getColor(android.R.color.white))
            binding.btnRoleStudent.setBackgroundColor(getColor(android.R.color.white))
            binding.btnRoleStudent.setTextColor(getColor(R.color.text_primary))
            binding.tilGrade.visibility = View.GONE
        }
    }

    private fun doLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val body = mapOf(
            "username" to username,
            "password" to password,
            "role" to selectedRole
        )

        ApiClient.apiService.login(body).enqueue(object : Callback<ApiResponse<LoginResponse>> {
            override fun onResponse(call: Call<ApiResponse<LoginResponse>>, response: Response<ApiResponse<LoginResponse>>) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    saveLoginData(body.data)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, body?.msg ?: "登录失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun doRegister() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val nickname = binding.etNickname.text.toString().trim()
        val grade = binding.etGrade.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val body = mutableMapOf(
            "username" to username,
            "password" to password,
            "role" to selectedRole
        )
        if (selectedRole == "student") {
            if (nickname.isNotEmpty()) body["nickname"] = nickname
            if (grade.isNotEmpty()) body["grade"] = grade
        } else {
            if (nickname.isNotEmpty()) body["nickname"] = nickname
        }

        ApiClient.apiService.register(body).enqueue(object : Callback<ApiResponse<LoginResponse>> {
            override fun onResponse(call: Call<ApiResponse<LoginResponse>>, response: Response<ApiResponse<LoginResponse>>) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    saveLoginData(body.data)
                    Toast.makeText(this@LoginActivity, "注册成功", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, body?.msg ?: "注册失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveLoginData(data: LoginResponse) {
        App.instance.prefs.token = data.token
        App.instance.prefs.saveUser(data.user)
        App.instance.prefs.role = selectedRole
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }
}

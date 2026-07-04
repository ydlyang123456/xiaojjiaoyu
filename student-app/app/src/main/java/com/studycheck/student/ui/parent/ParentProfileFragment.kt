package com.studycheck.student.ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.User
import com.studycheck.student.databinding.FragmentParentProfileBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.ui.auth.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ParentProfileFragment : Fragment() {

    private var _binding: FragmentParentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        loadProfile()
    }

    private fun setupViews() {
        binding.tvNickname.text = App.instance.prefs.nickname ?: "未设置"
        binding.tvUsername.text = "账号：${App.instance.prefs.username ?: ""}"

        binding.btnBindStudent.setOnClickListener {
            startActivity(Intent(requireContext(), BindStudentActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            App.instance.prefs.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }
    }

    private fun loadProfile() {
        ApiClient.apiService.getProfile().enqueue(object : Callback<ApiResponse<User>> {
            override fun onResponse(call: Call<ApiResponse<User>>, response: Response<ApiResponse<User>>) {
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    val user = body.data
                    binding.tvNickname.text = user.nickname ?: "未设置"
                    App.instance.prefs.saveUser(user)
                }
            }

            override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                Toast.makeText(requireContext(), "加载失败：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            val studentName = App.instance.prefs.currentStudentName
            if (!studentName.isNullOrEmpty()) {
                binding.tvStudentName.text = "已绑定学生：$studentName"
            } else {
                binding.tvStudentName.text = "尚未绑定学生"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

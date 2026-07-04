package com.studycheck.student.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.PaginatedResponse
import com.studycheck.student.data.Question
import com.studycheck.student.databinding.FragmentHomeBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.ui.search.SearchActivity
import com.studycheck.student.ui.pet.PetActivity
import com.studycheck.student.ui.leaderboard.LeaderboardActivity
import com.studycheck.student.util.TouchFeedback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val questionList = mutableListOf<Question>()
    private lateinit var adapter: QuestionAdapter

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
        loadQuestions()
    }

    private fun setupViews() {
        val nickname = App.instance.prefs.nickname ?: "同学"
        binding.tvGreeting.text = "你好，$nickname"

        TouchFeedback.applyAll(
            binding.btnCameraSearch,
            binding.cardPetEntry,
            binding.cardLeaderboardEntry
        )

        binding.btnCameraSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.cardPetEntry.setOnClickListener {
            startActivity(Intent(requireContext(), PetActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.cardLeaderboardEntry.setOnClickListener {
            startActivity(Intent(requireContext(), LeaderboardActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        adapter = QuestionAdapter(questionList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadQuestions()
        }

        animateEntry()
    }

    private fun animateEntry() {
        val interpolator = android.view.animation.DecelerateInterpolator()

        binding.tvGreeting.alpha = 0f
        binding.tvGreeting.translationY = -20f
        binding.tvGreeting.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(100)
            .setInterpolator(interpolator)
            .start()

        binding.btnCameraSearch.alpha = 0f
        binding.btnCameraSearch.translationY = 20f
        binding.btnCameraSearch.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(200)
            .setInterpolator(interpolator)
            .start()

        binding.cardPetEntry.alpha = 0f
        binding.cardPetEntry.translationY = 30f
        binding.cardPetEntry.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(300)
            .setInterpolator(interpolator)
            .start()

        binding.cardLeaderboardEntry.alpha = 0f
        binding.cardLeaderboardEntry.translationY = 30f
        binding.cardLeaderboardEntry.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(400)
            .setInterpolator(interpolator)
            .start()
    }

    private fun loadQuestions() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.apiService.getQuestions(1, 10).enqueue(object : Callback<ApiResponse<PaginatedResponse<Question>>> {
            override fun onResponse(
                call: Call<ApiResponse<PaginatedResponse<Question>>>,
                response: Response<ApiResponse<PaginatedResponse<Question>>>
            ) {
                binding.swipeRefresh.isRefreshing = false
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    questionList.clear()
                    questionList.addAll(body.data.items)
                    adapter.notifyDataSetChanged()

                    binding.tvTotalQuestions.text = "共 ${body.data.total} 道题目"

                    if (questionList.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.emptyView.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(requireContext(), body?.msg ?: "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PaginatedResponse<Question>>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            loadQuestions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

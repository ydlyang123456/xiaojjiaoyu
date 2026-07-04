package com.studycheck.student.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.PaginatedResponse
import com.studycheck.student.data.SearchRecord
import com.studycheck.student.databinding.FragmentSearchBinding
import com.studycheck.student.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val recordList = mutableListOf<SearchRecord>()
    private lateinit var adapter: SearchRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        loadRecords()
    }

    private fun setupViews() {
        binding.btnNewSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        adapter = SearchRecordAdapter(recordList) { record ->
            val intent = Intent(requireContext(), SearchResultActivity::class.java)
            intent.putExtra("record_id", record.id)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadRecords()
        }
    }

    private fun loadRecords() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.apiService.getSearchRecords(1, 20).enqueue(object : Callback<ApiResponse<PaginatedResponse<SearchRecord>>> {
            override fun onResponse(
                call: Call<ApiResponse<PaginatedResponse<SearchRecord>>>,
                response: Response<ApiResponse<PaginatedResponse<SearchRecord>>>
            ) {
                binding.swipeRefresh.isRefreshing = false
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    recordList.clear()
                    recordList.addAll(body.data.items)
                    adapter.notifyDataSetChanged()

                    if (recordList.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.emptyView.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(requireContext(), body?.msg ?: "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PaginatedResponse<SearchRecord>>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            loadRecords()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.studycheck.parent.ui.records

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
import com.studycheck.parent.databinding.FragmentRecordsBinding
import com.studycheck.parent.network.ApiClient
import com.studycheck.parent.ui.search.SearchDetailActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordList = mutableListOf<SearchRecord>()
    private lateinit var adapter: RecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        loadRecords()
    }

    private fun setupViews() {
        adapter = RecordAdapter(recordList) { record ->
            val intent = Intent(requireContext(), SearchDetailActivity::class.java)
            intent.putExtra("record_id", record.id)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadRecords()
        }
    }

    private fun loadRecords() {
        val studentId = App.instance.prefs.currentStudentId
        if (studentId <= 0) {
            binding.emptyView.visibility = View.VISIBLE
            binding.tvEmptyText.text = "请先绑定学生"
            return
        }

        binding.swipeRefresh.isRefreshing = true

        ApiClient.apiService.getSearchRecords(1, 20, studentId)
            .enqueue(object : Callback<ApiResponse<PaginatedResponse<SearchRecord>>> {
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

                        binding.tvTitle.text = "搜题记录（${body.data.total}条）"

                        if (recordList.isEmpty()) {
                            binding.emptyView.visibility = View.VISIBLE
                            binding.tvEmptyText.text = "暂无搜题记录"
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

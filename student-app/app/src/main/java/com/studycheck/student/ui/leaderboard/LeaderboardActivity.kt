package com.studycheck.student.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studycheck.student.data.LeaderboardItem
import com.studycheck.student.databinding.ActivityLeaderboardBinding
import com.studycheck.student.databinding.ItemLeaderboardBinding
import com.studycheck.student.network.ApiClient
import kotlinx.coroutines.*

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaderboardBinding
    private val rankIcons = listOf("🥇", "🥈", "🥉")
    private val skinIcons = mapOf(
        "default" to "🐱", "dog" to "🐶", "bunny" to "🐰", "fox" to "🦊",
        "panda" to "🐼", "unicorn" to "🦄", "dragon" to "🐲", "alien" to "👽"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener { loadLeaderboard() }

        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        binding.swipeRefresh.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getLeaderboard()
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    if (response.code == 200 && response.data != null) {
                        val list = response.data
                        if (list.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.rvLeaderboard.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.rvLeaderboard.visibility = View.VISIBLE
                            binding.rvLeaderboard.adapter = LeaderboardAdapter(list)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@LeaderboardActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class LeaderboardAdapter(private val items: List<LeaderboardItem>) : RecyclerView.Adapter<LeaderboardAdapter.VH>() {
        inner class VH(val binding: ItemLeaderboardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val rank = position + 1

            if (rank <= 3) {
                holder.binding.tvRank.text = rankIcons[position]
                holder.binding.tvRank.textSize = 24f
            } else {
                holder.binding.tvRank.text = rank.toString()
                holder.binding.tvRank.textSize = 16f
            }

            holder.binding.tvLeaderboardIcon.text = skinIcons[item.pet_skin] ?: "🐱"
            holder.binding.tvLeaderboardName.text = item.pet_name
            holder.binding.tvLeaderboardStudent.text = "${item.student_name} · Lv.${item.level}"
            holder.binding.tvLeaderboardCorrected.text = "${item.total_corrected}题"
            holder.binding.tvLeaderboardExp.text = "${item.exp}exp"
        }

        override fun getItemCount() = items.size
    }
}

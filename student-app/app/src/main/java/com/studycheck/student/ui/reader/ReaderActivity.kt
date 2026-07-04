package com.studycheck.student.ui.reader

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.student.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val content = intent.getStringExtra("content") ?: ""
        val title = intent.getStringExtra("title") ?: "阅读原文"

        binding.tvReaderTitle.text = title
        binding.tvReaderContent.text = content

        binding.btnBack.setOnClickListener { finish() }

        binding.btnFullscreen.setOnClickListener {
            try {
                if (window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "全屏切换失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

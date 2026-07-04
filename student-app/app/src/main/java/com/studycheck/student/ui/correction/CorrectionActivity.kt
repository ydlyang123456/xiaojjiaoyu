package com.studycheck.student.ui.correction

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.student.databinding.ActivityCorrectionBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CorrectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCorrectionBinding
    private var recordId: Int = 0
    private var selectedImageFile: File? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageFile = getFileFromUri(uri)
                binding.ivCorrectionImage.setImageURI(uri)
                binding.ivCorrectionImage.visibility = View.VISIBLE
                binding.btnUploadImage.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(binding.btnUploadImage, binding.btnSubmitCorrection)

        recordId = intent.getIntExtra("record_id", 0)
        val originalAnswer = intent.getStringExtra("original_answer") ?: ""

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvOriginalAnswer.text = originalAnswer

        binding.btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnSubmitCorrection.setOnClickListener { submitCorrection() }
    }

    private fun submitCorrection() {
        val text = binding.etCorrectionText.text.toString().trim()
        val isCorrect = binding.cbIsCorrect.isChecked

        if (text.isEmpty() && selectedImageFile == null) {
            Toast.makeText(this, "请输入订正内容或上传图片", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitCorrection.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imagePart = selectedImageFile?.let { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", file.name, requestFile)
                }

                val textBody = text.toRequestBody("text/plain".toMediaTypeOrNull())
                val isCorrectBody = isCorrect.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiClient.apiService.uploadCorrection(
                    imagePart, recordId, textBody, isCorrectBody
                )

                withContext(Dispatchers.Main) {
                    binding.btnSubmitCorrection.isEnabled = true
                    if (response.code == 200) {
                        Toast.makeText(this@CorrectionActivity, response.msg, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@CorrectionActivity, response.msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSubmitCorrection.isEnabled = true
                    Toast.makeText(this@CorrectionActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, "correction_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { inputStream.copyTo(it) }
        return file
    }
}

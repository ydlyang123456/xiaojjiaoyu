package com.studycheck.student.ui.search

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.studycheck.student.R
import com.studycheck.student.data.ApiResponse
import com.studycheck.student.data.SearchRecord
import com.studycheck.student.databinding.ActivitySearchBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var imageUri: Uri? = null
    private var imageFile: File? = null
    private var selectedSubject: String? = null

    private val subjects = arrayOf("数学", "语文", "英语", "物理", "化学", "生物", "历史", "地理", "政治", "其他")

    companion object {
        private const val REQUEST_CAMERA = 1001
        private const val REQUEST_GALLERY = 1002
        private const val REQUEST_CAMERA_PERMISSION = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(
            binding.btnBack, binding.btnTakePhoto,
            binding.btnChooseGallery, binding.btnSearch
        )

        setupViews()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjects)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubject.adapter = adapter

        binding.spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSubject = subjects[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSubject = null
            }
        }

        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermission()
        }

        binding.btnChooseGallery.setOnClickListener {
            openGallery()
        }

        binding.btnSearch.setOnClickListener {
            doSearch()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File.createTempFile("SEARCH_${timeStamp}_", ".jpg", storageDir)

        imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile!!)
        } else {
            Uri.fromFile(imageFile)
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    imageFile?.let {
                        binding.ivPreview.setImageURI(Uri.fromFile(it))
                        binding.ivPreview.visibility = View.VISIBLE
                    }
                }
                REQUEST_GALLERY -> {
                    data?.data?.let { uri ->
                        imageUri = uri
                        imageFile = getFileFromUri(uri)
                        binding.ivPreview.setImageURI(uri)
                        binding.ivPreview.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(filePathColumn[0])
        val filePath = columnIndex?.let { cursor.getString(it) }
        cursor?.close()
        return filePath?.let { File(it) }
    }

    private fun doSearch() {
        if (imageFile == null && binding.etQuery.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "请上传图片或输入题目", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        var imagePart: MultipartBody.Part? = null
        imageFile?.let { file ->
            if (file.exists()) {
                val requestFile = RequestBody.create(
                    "image/*".toMediaTypeOrNull(),
                    file
                )
                imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
            }
        }

        val queryText = if (binding.etQuery.text.toString().trim().isNotEmpty()) {
            RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etQuery.text.toString().trim())
        } else null

        val subjectBody = selectedSubject?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        ApiClient.apiService.searchQuestion(
            image = imagePart,
            queryText = queryText,
            subject = subjectBody
        ).enqueue(object : Callback<ApiResponse<SearchRecord>> {
            override fun onResponse(call: Call<ApiResponse<SearchRecord>>, response: Response<ApiResponse<SearchRecord>>) {
                showLoading(false)
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    val intent = Intent(this@SearchActivity, SearchResultActivity::class.java)
                    intent.putExtra("record_id", body.data.id)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@SearchActivity, body?.msg ?: "搜题失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<SearchRecord>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@SearchActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSearch.isEnabled = !show
    }
}

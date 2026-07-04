package com.studycheck.student.ui

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.studycheck.student.network.ApiClient
import kotlinx.coroutines.*

class UpdateManager(private val context: Context) {
    private var downloadId: Long = -1
    private var apkDownloadUrl: String = ""
    private var serverVersionCode: Int = 0
    private var serverVersionName: String = ""
    private var apkChangelog: String = ""

    fun checkUpdate(manual: Boolean = false, onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getAppVersion()
                if (response.code == 200 && response.data != null) {
                    serverVersionCode = response.data.version_code
                    serverVersionName = response.data.version_name
                    apkChangelog = response.data.changelog.takeIf { it.isNotEmpty() } ?: "有新版本可用，是否更新？"
                    apkDownloadUrl = response.data.download_url

                    if (!apkDownloadUrl.startsWith("http")) {
                        apkDownloadUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + apkDownloadUrl.trimStart('/')
                    }

                    val currentVersionCode = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                    } catch (e: Exception) { 1 }

                    withContext(Dispatchers.Main) {
                        if (serverVersionCode > currentVersionCode) {
                            showUpdateDialog(response.data.force_update)
                            onResult?.invoke(true)
                        } else if (manual) {
                            AlertDialog.Builder(context)
                                .setTitle("当前已是最新版本")
                                .setMessage("版本号：$serverVersionName")
                                .setPositiveButton("确定", null)
                                .show()
                            onResult?.invoke(false)
                        }
                        Unit
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Check update failed", e)
                if (manual) {
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(context)
                            .setTitle("检查更新失败")
                            .setMessage("无法连接到服务器，请稍后再试")
                            .setPositiveButton("确定", null)
                            .show()
                    }
                }
                onResult?.invoke(false)
            }
        }
    }

    private fun showUpdateDialog(force: Boolean) {
        val builder = AlertDialog.Builder(context)
            .setTitle("发现新版本 $serverVersionName")
            .setMessage(apkChangelog)
            .setCancelable(!force)
            .setPositiveButton("立即更新") { _, _ -> downloadApk() }

        if (!force) {
            builder.setNegativeButton("稍后再说", null)
        }
        builder.show()
    }

    private fun downloadApk() {
        if (apkDownloadUrl.isEmpty()) return

        try {
            val request = DownloadManager.Request(Uri.parse(apkDownloadUrl)).apply {
                setTitle("学习检测 更新")
                setDescription("正在下载新版本...")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "studycheck_update.apk")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(context, downloadManager, id)
                        context.unregisterReceiver(this)
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (e: Exception) {
            Log.e("UpdateManager", "Download failed", e)
        }
    }

    private fun installApk(context: Context, downloadManager: DownloadManager, id: Long) {
        try {
            val query = DownloadManager.Query().apply { setFilterById(id) }
            val cursor: Cursor = downloadManager.query(query) ?: return
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    val apkUri = Uri.parse(uriString)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val apkFile = java.io.File(apkUri.path!!)
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            apkFile
                        )
                        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                            setDataAndType(contentUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(installIntent)
                    } else {
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(installIntent)
                    }
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("UpdateManager", "Install failed", e)
        }
    }
}
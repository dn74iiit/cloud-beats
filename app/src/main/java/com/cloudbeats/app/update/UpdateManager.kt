package com.cloudbeats.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class UpdateManager(private val context: Context) {

    private var downloadId: Long = -1L
    private var expectedFileName: String = "CloudBeats-Update.apk"

    fun downloadAndInstallUpdate(apkUrl: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Setup download request
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Downloading CloudBeats Update")
            .setDescription("Please wait while the new version downloads.")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, expectedFileName)
            
        // Delete old apk if exists
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), expectedFileName)
        if (oldFile.exists()) {
            oldFile.delete()
        }

        // Register receiver to know when it finishes
        context.registerReceiver(
            onDownloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )

        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        downloadId = downloadManager.enqueue(request)
    }

    private val onDownloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                // Download finished, unregister receiver
                context.unregisterReceiver(this)
                installApk()
            }
        }
    }

    private fun installApk() {
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), expectedFileName)
        
        if (!apkFile.exists()) {
            Toast.makeText(context, "Failed to find downloaded APK.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start installation.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}

package com.bandbbs.ebook.notifications

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo

class ForegroundTransferService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "传输中"
        val content = intent?.getStringExtra(EXTRA_CONTENT)
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, -1)
        val progressPercent = if (progress != null && progress >= 0) progress else null

        val notification: Notification = LiveNotificationManager.buildTransferNotification(title, content, progressPercent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(LiveNotificationManager.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(LiveNotificationManager.NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveNotificationManager.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_PROGRESS = "extra_progress"

        fun startService(context: Context, title: String, content: String?, progressPercent: Int?) {
            val intent = Intent(context, ForegroundTransferService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_PROGRESS, progressPercent ?: -1)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, ForegroundTransferService::class.java))
        }
    }
}



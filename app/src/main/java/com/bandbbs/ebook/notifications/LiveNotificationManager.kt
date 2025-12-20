package com.bandbbs.ebook.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.Intent
import com.bandbbs.ebook.R

object LiveNotificationManager {
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context
    const val CHANNEL_ID = "transfer_channel_id"
    private const val CHANNEL_NAME = "传输通知"
    private const val NOTIFICATION_ID = 2001

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    fun initialize(context: Context, notifManager: NotificationManager) {
        notificationManager = notifManager
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
    }

    fun showTransferNotification(title: String, contentText: String? = null, progressPercent: Int? = null) {
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        contentText?.let {
            builder.setContentText(it)
        }

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(appContext, com.bandbbs.ebook.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(appContext, 0, launchIntent, flags)
        builder.setContentIntent(pendingIntent)
            .setRequestPromotedOngoing(true)

        if (progressPercent == null || progressPercent <= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                builder.setStyle(NotificationCompat.ProgressStyle().setProgressIndeterminate(true))
            } else {
                builder.setProgress(0, 0, true)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                builder.setStyle(NotificationCompat.ProgressStyle().setProgress(progressPercent))
            } else {
                builder.setProgress(100, progressPercent, false)
            }
            builder.setContentText("$progressPercent%")
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel() {
        if (::notificationManager.isInitialized) {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    fun isPostPromotionsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            notificationManager.canPostPromotedNotifications()
        } else {
            true
        }
    }
}



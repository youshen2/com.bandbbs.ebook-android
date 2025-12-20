package com.bandbbs.ebook

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.bandbbs.ebook.logic.InterHandshake

class App : Application() {
    lateinit var conn: InterHandshake

    override fun onCreate() {
        super.onCreate()
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        com.bandbbs.ebook.notifications.LiveNotificationManager.initialize(this, notifManager)
    }
}
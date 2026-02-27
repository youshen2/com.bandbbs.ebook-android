package com.bandbbs.ebook

import android.app.Application
import android.app.NotificationManager
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.notifications.LiveNotificationManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class App : Application() {
    lateinit var conn: InterHandshake

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        LiveNotificationManager.initialize(this, notifManager)
    }
}
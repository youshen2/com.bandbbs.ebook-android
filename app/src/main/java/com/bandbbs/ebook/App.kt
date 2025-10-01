package com.bandbbs.ebook

import android.app.Application
import com.bandbbs.ebook.logic.InterHandshake
class App: Application() {
    lateinit var conn : InterHandshake
}
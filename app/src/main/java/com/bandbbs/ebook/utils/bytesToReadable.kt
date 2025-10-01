package com.bandbbs.ebook.utils

import android.annotation.SuppressLint


fun bytesToReadable(bytes: Long): String {
    return bytesToReadable(bytes.toDouble())
}
@SuppressLint("DefaultLocale")
fun bytesToReadable(bytes: Double): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var b = bytes
    var i = 0
    while (b >= 1024 && i < units.size - 1) {
        b /= 1024
        i++
    }
    return String.format("%.2f %s", b, units[i])
}
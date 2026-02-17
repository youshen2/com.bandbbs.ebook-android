package com.bandbbs.ebook.utils

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

object PdfParser {
    fun isPdfFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(5)
                val read = input.read(header)
                read == 5 && String(header, Charsets.US_ASCII) == "%PDF-"
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun getPageCount(file: File): Int {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val renderer = PdfRenderer(pfd)
            try {
                return renderer.pageCount
            } finally {
                renderer.close()
            }
        } finally {
            pfd.close()
        }
    }

    fun extractPlainText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val stripper = PDFTextStripper()
                return stripper.getText(document) ?: ""
            }
        }
        throw IllegalArgumentException("无法读取文件")
    }
}


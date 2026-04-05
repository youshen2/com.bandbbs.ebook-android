package com.bandbbs.ebook.utils.parser

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream

object EpubParser {
    data class EpubBook(
        val title: String,
        val author: String,
        val chapters: List<EpubChapter>,
        val coverImage: ByteArray? = null
    )

    data class EpubChapter(
        val title: String,
        val content: String,
        val wordCount: Int
    )

    fun parse(context: Context, uri: Uri): EpubBook {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            return parseFromInputStream(inputStream)
        } ?: throw IllegalArgumentException("无法打开文件")
    }

    private fun parseFromInputStream(inputStream: InputStream): EpubBook {
        val zipEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val content = zipStream.readBytes()
                    zipEntries[entry.name] = content
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }

        val containerXml = zipEntries["META-INF/container.xml"]
            ?: throw IllegalArgumentException("无效的EPUB文件：缺少container.xml")

        val opfPath = parseContainerXml(containerXml)
        val opfContent = zipEntries[opfPath]
            ?: throw IllegalArgumentException("无效的EPUB文件：找不到$opfPath")

        val opfDir = opfPath.substringBeforeLast('/', "")
        val (metadata, spine, coverHref) = parseOpf(opfContent)

        val coverImage = coverHref?.let { href ->
            val coverPath = if (opfDir.isNotEmpty()) normalizePath("$opfDir/$href") else href
            zipEntries[coverPath]
        }

        var chapters = spine.mapNotNull { rawHref ->
            val href = rawHref.substringBefore("#")
            val itemPath = if (opfDir.isNotEmpty()) normalizePath("$opfDir/$href") else href
            zipEntries[itemPath]?.let { content ->
                parseHtmlChapter(content)
            }
        }.filter { it.content.isNotBlank() }

        if (chapters.isEmpty()) {
            chapters = zipEntries.filterKeys {
                it.endsWith(".html", ignoreCase = true) ||
                        it.endsWith(".htm", ignoreCase = true) ||
                        it.endsWith(".xhtml", ignoreCase = true)
            }.toSortedMap().values.mapNotNull { content ->
                parseHtmlChapter(content)
            }.filter { it.content.isNotBlank() }
        }

        return EpubBook(
            title = metadata["title"] ?: "未知书名",
            author = metadata["author"] ?: "未知作者",
            chapters = chapters,
            coverImage = coverImage
        )
    }

    private fun normalizePath(path: String): String {
        val parts = path.split("/")
        val result = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (result.isNotEmpty()) result.removeAt(result.size - 1)
            } else if (part != "." && part.isNotEmpty()) {
                result.add(part)
            }
        }
        return result.joinToString("/")
    }

    private fun parseContainerXml(xmlBytes: ByteArray): String {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xmlBytes.inputStream(), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name.substringAfterLast(":")
                if (tagName == "rootfile") {
                    val fullPath = parser.getAttributeValue(null, "full-path")
                    if (fullPath != null) {
                        return fullPath
                    }
                }
            }
            eventType = parser.next()
        }
        throw IllegalArgumentException("无效的container.xml")
    }

    private fun parseOpf(opfBytes: ByteArray): Triple<Map<String, String>, List<String>, String?> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(opfBytes.inputStream(), "UTF-8")

        val metadata = mutableMapOf<String, String>()
        val manifest = mutableMapOf<String, String>()
        val manifestProperties = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()
        var coverItemId: String? = null

        var eventType = parser.eventType
        var inMetadata = false
        var inManifest = false
        var inSpine = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name.substringAfterLast(":")
                    when (tagName) {
                        "metadata" -> inMetadata = true
                        "manifest" -> inManifest = true
                        "spine" -> inSpine = true
                        "title" -> {
                            if (inMetadata) {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    metadata["title"] = parser.text
                                }
                            }
                        }

                        "creator" -> {
                            if (inMetadata) {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    metadata["author"] = parser.text
                                }
                            }
                        }

                        "meta" -> {
                            if (inMetadata) {
                                val name = parser.getAttributeValue(null, "name")
                                val content = parser.getAttributeValue(null, "content")
                                if (name == "cover" && content != null) {
                                    coverItemId = content
                                }
                            }
                        }

                        "item" -> {
                            if (inManifest) {
                                val id = parser.getAttributeValue(null, "id")
                                val rawHref = parser.getAttributeValue(null, "href")
                                val properties = parser.getAttributeValue(null, "properties")

                                if (id != null && rawHref != null) {
                                    val href = try {
                                        URLDecoder.decode(rawHref, "UTF-8").substringBefore("#")
                                    } catch (e: Exception) {
                                        rawHref.substringBefore("#")
                                    }
                                    manifest[id] = href

                                    if (properties != null) {
                                        manifestProperties[id] = properties
                                        if (properties.contains("cover-image")) {
                                            coverItemId = id
                                        }
                                    }
                                }
                            }
                        }

                        "itemref" -> {
                            if (inSpine) {
                                val idref = parser.getAttributeValue(null, "idref")
                                if (idref != null) {
                                    manifest[idref]?.let { href ->
                                        spine.add(href)
                                    }
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val tagName = parser.name.substringAfterLast(":")
                    when (tagName) {
                        "metadata" -> inMetadata = false
                        "manifest" -> inManifest = false
                        "spine" -> inSpine = false
                    }
                }
            }
            eventType = parser.next()
        }

        if (coverItemId == null) {
            coverItemId = manifest.entries.find { (_, href) ->
                href.contains("cover", ignoreCase = true) &&
                        (href.endsWith(".jpg", ignoreCase = true) ||
                                href.endsWith(".jpeg", ignoreCase = true) ||
                                href.endsWith(".png", ignoreCase = true))
            }?.key
        }
        val coverHref = coverItemId?.let { manifest[it] }

        return Triple(metadata, spine, coverHref)
    }

    private fun parseHtmlChapter(htmlBytes: ByteArray): EpubChapter {
        val htmlContent = String(htmlBytes, Charsets.UTF_8)
        val title = extractTitle(htmlContent)
        var content = extractTextContent(htmlContent)

        content = removeTitleFromContent(content, title)
        content = normalizeContentStart(content)
        val wordCount = content.length

        return EpubChapter(title, content, wordCount)
    }

    private fun removeTitleFromContent(content: String, title: String): String {
        if (title == "未命名章节" || title.isBlank()) {
            return content
        }

        val lines = content.lines()
        if (lines.isEmpty()) {
            return content
        }

        val titleTrimmed = title.trim()
        val titleNormalized = titleTrimmed.replace(Regex("\\s+"), " ")

        val filteredLines = mutableListOf<String>()
        var removedCount = 0

        for (i in lines.indices) {
            if (i < 3 && removedCount < 3) {
                val line = lines[i].trim()
                if (line.isEmpty()) {
                    filteredLines.add(lines[i])
                    continue
                }

                val isTitleLine = line == titleTrimmed ||
                        line == titleNormalized ||
                        line.contains(titleTrimmed, ignoreCase = true) ||
                        titleTrimmed.contains(line, ignoreCase = true) ||
                        (line.length > 3 && titleTrimmed.length > 3 &&
                                (line.substring(
                                    0,
                                    minOf(10, line.length)
                                ) == titleTrimmed.substring(0, minOf(10, titleTrimmed.length)) ||
                                        titleTrimmed.substring(
                                            0,
                                            minOf(10, titleTrimmed.length)
                                        ) == line.substring(0, minOf(10, line.length))))

                if (isTitleLine) {
                    removedCount++
                    continue
                }
            }
            filteredLines.add(lines[i])
        }

        val result = filteredLines.joinToString("\n")
        return if (result.isBlank()) content else result
    }

    private fun normalizeContentStart(content: String): String {
        if (content.isBlank()) {
            return content
        }
        val trimmed = content.trimStart()
        return if (trimmed.isNotEmpty()) {
            "\n$trimmed"
        } else {
            trimmed
        }
    }

    private fun extractTitle(html: String): String {
        val titleRegex = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)
        titleRegex.find(html)?.let {
            return it.groupValues[1].trim()
        }

        val h1Regex = Regex("<h1[^>]*>([^<]+)</h1>", RegexOption.IGNORE_CASE)
        h1Regex.find(html)?.let {
            return it.groupValues[1].trim()
        }

        val h2Regex = Regex("<h2[^>]*>([^<]+)</h2>", RegexOption.IGNORE_CASE)
        h2Regex.find(html)?.let {
            return it.groupValues[1].trim()
        }

        return "未命名章节"
    }

    private fun extractTextContent(html: String): String {
        var text = html.replace(
            Regex(
                "<script[^>]*>.*?</script>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ), ""
        )
        text = text.replace(
            Regex(
                "<style[^>]*>.*?</style>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ), ""
        )
        text = text.replace(Regex("<[^>]+>"), " ")
        text = text.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\n\\s*\\n+"), "\n\n")

        return text.trim()
    }

    fun isEpubFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name == "mimetype") {
                            val mimetype = String(zipStream.readBytes(), Charsets.US_ASCII).trim()
                            return mimetype == "application/epub+zip"
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                    false
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}

package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * EPUB 格式解析器
 * EPUB 是一种基于 XML 和 ZIP 的电子书格式
 */
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

    /**
     * 从 Uri 解析 EPUB 文件
     */
    fun parse(context: Context, uri: Uri): EpubBook {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            return parseFromInputStream(inputStream)
        } ?: throw IllegalArgumentException("无法打开文件")
    }

    /**
     * 从 InputStream 解析 EPUB 文件
     */
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
        val (metadata, spine, coverItemId) = parseOpf(opfContent)

        
        val coverImage = coverItemId?.let { itemId ->
            val coverPath = if (opfDir.isNotEmpty()) "$opfDir/$itemId" else itemId
            zipEntries[coverPath]
        }

        
        val chapters = spine.mapNotNull { itemId ->
            val itemPath = if (opfDir.isNotEmpty()) "$opfDir/$itemId" else itemId
            zipEntries[itemPath]?.let { content ->
                parseHtmlChapter(content)
            }
        }.filter { it.content.isNotBlank() }

        return EpubBook(
            title = metadata["title"] ?: "未知书名",
            author = metadata["author"] ?: "未知作者",
            chapters = chapters,
            coverImage = coverImage
        )
    }

    /**
     * 解析 container.xml 获取 OPF 文件路径
     */
    private fun parseContainerXml(xmlBytes: ByteArray): String {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xmlBytes.inputStream(), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                val fullPath = parser.getAttributeValue(null, "full-path")
                if (fullPath != null) {
                    return fullPath
                }
            }
            eventType = parser.next()
        }
        throw IllegalArgumentException("无效的container.xml")
    }

    /**
     * 解析 OPF 文件获取元数据和章节顺序
     */
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
                    when (parser.name) {
                        "metadata" -> inMetadata = true
                        "manifest" -> inManifest = true
                        "spine" -> inSpine = true
                        "dc:title", "title" -> {
                            if (inMetadata) {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    metadata["title"] = parser.text
                                }
                            }
                        }
                        "dc:creator", "creator" -> {
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
                                val href = parser.getAttributeValue(null, "href")
                                val properties = parser.getAttributeValue(null, "properties")
                                if (id != null && href != null) {
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
                    when (parser.name) {
                        "metadata" -> inMetadata = false
                        "manifest" -> inManifest = false
                        "spine" -> inSpine = false
                    }
                }
            }
            eventType = parser.next()
        }

        
        if (coverItemId == null) {
            coverItemId = manifest.entries.find { (id, href) ->
                href.contains("cover", ignoreCase = true) && 
                (href.endsWith(".jpg", ignoreCase = true) || 
                 href.endsWith(".jpeg", ignoreCase = true) || 
                 href.endsWith(".png", ignoreCase = true))
            }?.key
        }

        val coverHref = coverItemId?.let { manifest[it] }
        return Triple(metadata, spine, coverHref)
    }

    /**
     * 解析 HTML/XHTML 章节内容
     */
    private fun parseHtmlChapter(htmlBytes: ByteArray): EpubChapter {
        val htmlContent = String(htmlBytes, Charsets.UTF_8)
        
        
        val title = extractTitle(htmlContent)
        
        
        val content = extractTextContent(htmlContent)
        val wordCount = content.length

        return EpubChapter(title, content, wordCount)
    }

    /**
     * 提取标题
     */
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

    /**
     * 提取文本内容（移除 HTML 标签）
     */
    private fun extractTextContent(html: String): String {
        
        var text = html.replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        text = text.replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        
        
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

    /**
     * 验证文件是否为 EPUB 格式
     */
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


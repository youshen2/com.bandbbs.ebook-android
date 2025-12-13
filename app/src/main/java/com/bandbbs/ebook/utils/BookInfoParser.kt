package com.bandbbs.ebook.utils

/**
 * 书籍信息解析器
 * 用于从"简介"章节中解析书籍元数据
 */
object BookInfoParser {

    data class ParsedBookInfo(
        val title: String? = null,
        val author: String? = null,
        val status: String? = null,
        val chapterCount: String? = null,
        val tags: String? = null,
        val summary: String? = null
    )

    /**
     * 从"简介"章节内容中解析书籍信息
     *
     * 支持的格式示例：
     * ```
     * 介绍
     *
     *  职业提督，深海代行者
     *
     *  作者： 苗苗
     *
     *  状态： 已完结； 章节数： 565
     *
     *  标签： 已完结 | 动漫衍生 | 穿越 | 二次元 | 同人 | 衍生
     *
     *  简介
     *
     *  在漫长的提督生涯中，我体会到了一些事...
     * ```
     */
    fun parseIntroductionContent(content: String): ParsedBookInfo? {
        if (content.isBlank()) return null

        val lines = content.lines()
        var title: String? = null
        var author: String? = null
        var status: String? = null
        var chapterCount: String? = null
        var tags: String? = null
        var summary: String? = null

        var foundIntroduction = false
        var foundSummary = false
        val summaryLines = mutableListOf<String>()

        for (i in lines.indices) {
            val line = lines[i].trim()


            if (line.isEmpty() ||
                line == "介绍" ||
                line == "简介" ||
                line == "书籍信息" ||
                line.contains("介绍") && line.length < 10
            ) {
                if (line == "介绍" || line == "简介") {
                    foundIntroduction = true
                }
                continue
            }


            if (foundIntroduction && title == null && line.isNotBlank() &&
                !line.contains("作者") && !line.contains("状态") &&
                !line.contains("章节") && !line.contains("标签")
            ) {
                title = line
                continue
            }


            if (line.contains("作者") || line.contains("作者：") || line.contains("作者:")) {
                author = extractValue(line, "作者")
                continue
            }


            if (line.contains("状态") || line.contains("状态：") || line.contains("状态:")) {

                val statusMatch = Regex("状态[：:]\\s*([^；;]+)").find(line)
                status = statusMatch?.groupValues?.get(1)?.trim()


                val chapterMatch = Regex("章节数[：:]\\s*(\\d+)").find(line)
                chapterCount = chapterMatch?.groupValues?.get(1)
                continue
            }


            if (chapterCount == null && (line.contains("章节数") || line.contains("章节数：") || line.contains(
                    "章节数:"
                ))
            ) {
                val chapterMatch = Regex("章节数[：:]\\s*(\\d+)").find(line)
                chapterCount = chapterMatch?.groupValues?.get(1)
                continue
            }


            if (line.contains("标签") || line.contains("标签：") || line.contains("标签:")) {
                tags = extractValue(line, "标签")
                continue
            }


            if (line == "简介" || (foundIntroduction && line.contains("简介") && line.length < 10)) {
                foundSummary = true
                continue
            }


            if (foundSummary && line.isNotBlank()) {
                summaryLines.add(line)
            }
        }


        if (!foundSummary && summaryLines.isEmpty()) {

            var startCollecting = false
            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.contains("简介") || line.contains("内容简介") || line.contains("作品简介")) {
                    startCollecting = true
                    continue
                }
                if (startCollecting && line.isNotBlank() &&
                    !line.contains("作者") && !line.contains("状态") &&
                    !line.contains("章节") && !line.contains("标签")
                ) {
                    summaryLines.add(line)
                }
            }
        }


        if (title == null) {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() &&
                    !trimmed.contains("作者") && !trimmed.contains("状态") &&
                    !trimmed.contains("章节") && !trimmed.contains("标签") &&
                    !trimmed.contains("介绍") && !trimmed.contains("简介")
                ) {
                    title = trimmed
                    break
                }
            }
        }

        summary = if (summaryLines.isNotEmpty()) {
            summaryLines.joinToString("\n").trim()
        } else {
            null
        }


        if (title != null || author != null || status != null ||
            chapterCount != null || tags != null || summary != null
        ) {
            return ParsedBookInfo(
                title = title,
                author = author,
                status = status,
                chapterCount = chapterCount,
                tags = tags,
                summary = summary
            )
        }

        return null
    }

    /**
     * 从字符串中提取值
     * 例如："作者： 苗苗" -> "苗苗"
     * 例如："标签： 已完结 | 动漫衍生 | 穿越" -> "已完结 | 动漫衍生 | 穿越"
     */
    private fun extractValue(line: String, key: String): String? {
        val patterns = listOf(
            Regex("$key[：:]\\s*(.+?)(?:；|;|,|$)"),
            Regex("$key\\s*[：:]\\s*(.+?)(?:；|;|,|$)"),
            Regex("$key[：:]\\s*(.+)$")
        )

        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                val value = match.groupValues[1].trim()
                if (value.isNotBlank()) {
                    return value
                }
            }
        }

        return null
    }
}


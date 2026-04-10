package com.bandbbs.ebook.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 阅读位置管理器
 * 使用防抖动机制减少磁盘I/O频率
 */
class ReadingPositionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val pendingSaves = mutableMapOf<String, SaveTask>()
    private val saveJobs = mutableMapOf<String, Job>()

    private data class SaveTask(
        val chapterId: Int,
        val index: Int,
        val offset: Int
    )

    companion object {
        private const val PREFS_NAME = "chapter_reader_prefs"
        private const val KEY_READING_POSITION = "reading_position_index_"
        private const val KEY_READING_OFFSET = "reading_position_offset_"
        private const val KEY_LAST_READ_CHAPTER = "last_read_chapter_"

        // 防抖动延迟时间
        private const val DEBOUNCE_DELAY_MS = 1000L
        // 最大延迟时间,确保数据不会丢失太久
        private const val MAX_DELAY_MS = 5000L
    }

    /**
     * 保存阅读位置(带防抖动)
     */
    fun saveReadingPosition(chapterId: Int, index: Int, offset: Int) {
        val key = "position_$chapterId"

        // 取消之前的保存任务
        saveJobs[key]?.cancel()

        // 记录待保存的数据
        pendingSaves[key] = SaveTask(chapterId, index, offset)

        // 启动新的防抖动任务
        saveJobs[key] = scope.launch {
            val startTime = System.currentTimeMillis()

            while (true) {
                delay(DEBOUNCE_DELAY_MS)

                // 检查是否超过最大延迟时间
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= MAX_DELAY_MS) {
                    break
                }

                // 如果在延迟期间没有新的更新,则执行保存
                val currentTask = pendingSaves[key]
                if (currentTask != null) {
                    delay(100) // 短暂等待,检查是否有新更新
                    if (pendingSaves[key] == currentTask) {
                        break
                    }
                }
            }

            // 执行实际的保存操作
            pendingSaves[key]?.let { task ->
                saveImmediately(task.chapterId, task.index, task.offset)
                pendingSaves.remove(key)
            }
            saveJobs.remove(key)
        }
    }

    /**
     * 立即保存阅读位置(用于关键时刻,如退出应用)
     */
    fun saveReadingPositionImmediately(chapterId: Int, index: Int, offset: Int) {
        val key = "position_$chapterId"
        saveJobs[key]?.cancel()
        pendingSaves.remove(key)
        saveImmediately(chapterId, index, offset)
    }

    private fun saveImmediately(chapterId: Int, index: Int, offset: Int) {
        prefs.edit()
            .putInt("$KEY_READING_POSITION$chapterId", index)
            .putInt("$KEY_READING_OFFSET$chapterId", offset)
            .apply()
    }

    /**
     * 加载阅读位置
     */
    fun loadReadingPosition(chapterId: Int): Pair<Int, Int> {
        val index = prefs.getInt("$KEY_READING_POSITION$chapterId", 0)
        val offset = prefs.getInt("$KEY_READING_OFFSET$chapterId", 0)
        return index to offset
    }

    /**
     * 保存最后阅读的章节
     */
    fun saveLastReadChapter(bookId: Int, chapterId: Int) {
        prefs.edit()
            .putInt("$KEY_LAST_READ_CHAPTER$bookId", chapterId)
            .apply()
    }

    /**
     * 刷新所有待保存的数据
     */
    fun flushAll() {
        saveJobs.values.forEach { it.cancel() }
        pendingSaves.forEach { (key, task) ->
            if (key.startsWith("position_")) {
                saveImmediately(task.chapterId, task.index, task.offset)
            }
        }
        pendingSaves.clear()
        saveJobs.clear()
    }
}

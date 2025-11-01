package com.bandbbs.ebook.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    suspend fun getChaptersForBook(bookId: Int): List<Chapter>

    /**
     * 获取章节信息（不包含内容），避免CursorWindow溢出
     */
    @Query("SELECT id, bookId, `index`, name, wordCount FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    suspend fun getChapterInfoForBook(bookId: Int): List<ChapterInfo>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterById(chapterId: Int): Chapter?

    @Query("SELECT id, bookId, `index`, name, wordCount FROM chapters WHERE bookId = :bookId AND `index` = :index LIMIT 1")
    suspend fun getChapterInfoByIndex(bookId: Int, index: Int): ChapterInfo?

    @Query("SELECT LENGTH(content) FROM chapters WHERE id = :chapterId")
    suspend fun getChapterContentLength(chapterId: Int): Int?

    @Query("SELECT SUBSTR(content, :offset, :length) FROM chapters WHERE id = :chapterId")
    suspend fun getChapterContentChunk(chapterId: Int, offset: Int, length: Int): String?

    /**
     * 根据书籍ID和章节索引列表批量获取章节
     * 避免加载所有章节导致CursorWindow溢出
     */
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `index` IN (:indices) ORDER BY `index` ASC")
    suspend fun getChaptersByIndices(bookId: Int, indices: List<Int>): List<Chapter>

    @Query("SELECT COUNT(id) FROM chapters WHERE bookId = :bookId")
    suspend fun getChapterCountForBook(bookId: Int): Int

    @Query("SELECT SUM(wordCount) FROM chapters WHERE bookId = :bookId")
    suspend fun getTotalWordCountForBook(bookId: Int): Long?

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Int)
}

package com.bandbbs.ebook.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移策略
 * 避免使用fallbackToDestructiveMigration导致用户数据丢失
 */
object DatabaseMigrations {

    /**
     * 从版本3到版本4的迁移
     * 根据实际需求添加迁移逻辑
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 示例:如果版本4添加了新字段或表,在这里执行SQL
            // database.execSQL("ALTER TABLE books ADD COLUMN new_field TEXT")

            // 如果没有schema变更,保持空实现以保留数据
        }
    }

    /**
     * 从版本2到版本3的迁移
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 添加实际的迁移逻辑
        }
    }

    /**
     * 从版本1到版本2的迁移
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 添加实际的迁移逻辑
        }
    }

    /**
     * 获取所有迁移策略
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4
        )
    }
}

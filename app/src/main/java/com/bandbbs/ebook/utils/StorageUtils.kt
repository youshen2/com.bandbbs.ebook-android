package com.bandbbs.ebook.utils

object StorageUtils {
    fun getReservedStorage(product: String?): Long {
        if (product == null) return 0
        return when {
            product == "REDMI Watch 6" -> 120 * 1024 * 1024
            product == "REDMI Watch 5" -> 120 * 1024 * 1024
            product == "Xiaomi Smart Band 9" -> 64 * 1024 * 1024
            product == "Xiaomi Smart Band 9 Pro" -> 64 * 1024 * 1024
            product == "Xiaomi Smart Band 8 Pro" -> 84 * 1024 * 1024
            product == "o65m" -> 1024 * 1024 * 1024
            product.contains("Xiaomi Smart Band 10") -> 90 * 1024 * 1024
            else -> 0
        }
    }

    fun calculateStorageInfo(
        totalStorage: Long,
        availableStorage: Long,
        product: String?
    ): StorageInfo {
        val reservedStorage = getReservedStorage(product)
        val usedStorage = totalStorage - availableStorage
        val actualAvailable = totalStorage - reservedStorage - usedStorage
        return StorageInfo(
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            reservedStorage = reservedStorage,
            usedStorage = usedStorage,
            actualAvailable = actualAvailable
        )
    }

    fun isStorageLow(actualAvailable: Long): Boolean {
        return actualAvailable < 2 * 1024 * 1024
    }

    fun getReservedStorageText(product: String?): String {
        return when {
            product == "REDMI Watch 6" -> "120MB"
            product == "REDMI Watch 5" -> "120MB"
            product == "Xiaomi Smart Band 9 Pro" -> "64MB"
            product == "Xiaomi Smart Band 8 Pro" -> "84MB"
            product != null && product.contains("Xiaomi Smart Band 10") -> "90MB"
            else -> "0MB"
        }
    }

    data class StorageInfo(
        val totalStorage: Long,
        val availableStorage: Long,
        val reservedStorage: Long,
        val usedStorage: Long,
        val actualAvailable: Long
    )
}


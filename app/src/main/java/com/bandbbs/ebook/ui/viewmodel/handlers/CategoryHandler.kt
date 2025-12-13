package com.bandbbs.ebook.ui.viewmodel.handlers

import android.content.SharedPreferences
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.ui.viewmodel.CategoryState
import com.bandbbs.ebook.ui.viewmodel.ImportState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryHandler(
    private val prefs: SharedPreferences,
    private val db: AppDatabase,
    private val scope: CoroutineScope,
    private val categoryState: MutableStateFlow<CategoryState?>,
    private val importState: MutableStateFlow<ImportState?>,
    private val onBooksChanged: () -> Unit,
) {

    companion object {
        private const val CATEGORIES_KEY = "book_categories"
    }

    fun getCategories(): List<String> {
        return try {
            val set = prefs.getStringSet(CATEGORIES_KEY, null)
            if (set != null) {
                set.map { it.toString() }.sorted()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            try {
                prefs.edit().remove(CATEGORIES_KEY).apply()
            } catch (_: Exception) {
            }
            emptyList()
        }
    }

    fun showCategorySelector(book: Book? = null) {
        val categories = getCategories()
        val selectedCategory = book?.localCategory ?: importState.value?.selectedCategory
        categoryState.value = CategoryState(
            categories = categories,
            selectedCategory = selectedCategory,
            book = book,
            onCategorySelectedForEdit = null
        )
    }

    fun showCategorySelectorForEdit(
        selectedCategory: String?,
        onCategorySelected: (String?) -> Unit
    ) {
        val categories = getCategories()
        categoryState.value = CategoryState(
            categories = categories,
            selectedCategory = selectedCategory,
            book = null,
            onCategorySelectedForEdit = onCategorySelected
        )
    }

    fun createCategory(categoryName: String) {
        try {
            val currentSet =
                prefs.getStringSet(CATEGORIES_KEY, null)?.toMutableSet() ?: mutableSetOf()
            currentSet.add(categoryName)
            prefs.edit().putStringSet(CATEGORIES_KEY, HashSet(currentSet)).apply()
            categoryState.value?.let { state ->
                categoryState.value = state.copy(categories = currentSet.toList().sorted())
            }
        } catch (_: Exception) {
            try {
                prefs.edit().remove(CATEGORIES_KEY).apply()
                val newSet = hashSetOf(categoryName)
                prefs.edit().putStringSet(CATEGORIES_KEY, newSet).apply()
                categoryState.value?.let { state ->
                    categoryState.value = state.copy(categories = listOf(categoryName))
                }
            } catch (_: Exception) {
            }
        }
    }

    fun deleteCategory(categoryName: String) {
        try {
            val currentSet =
                prefs.getStringSet(CATEGORIES_KEY, null)?.toMutableSet() ?: mutableSetOf()
            currentSet.remove(categoryName)
            prefs.edit().putStringSet(CATEGORIES_KEY, HashSet(currentSet)).apply()

            scope.launch(Dispatchers.IO) {
                val books = db.bookDao().getAllBooks()
                books.forEach { bookEntity ->
                    if (bookEntity.localCategory == categoryName) {
                        db.bookDao().update(bookEntity.copy(localCategory = null))
                    }
                }
                withContext(Dispatchers.Main) {
                    onBooksChanged()
                }
            }

            categoryState.value?.let { state ->
                categoryState.value = state.copy(categories = currentSet.toList().sorted())
            }
        } catch (_: Exception) {
            try {
                prefs.edit().remove(CATEGORIES_KEY).apply()
                categoryState.value?.let { state ->
                    categoryState.value = state.copy(categories = emptyList())
                }
            } catch (_: Exception) {
            }
        }
    }

    fun selectCategory(category: String?) {
        categoryState.value?.let { state ->

            state.onCategorySelectedForEdit?.invoke(category)


            if (state.onCategorySelectedForEdit == null) {
                val book = state.book
                if (book != null) {
                    scope.launch(Dispatchers.IO) {
                        val bookEntity = db.bookDao().getBookByPath(book.path)
                        if (bookEntity != null) {
                            db.bookDao().update(bookEntity.copy(localCategory = category))
                            withContext(Dispatchers.Main) {
                                onBooksChanged()
                            }
                        }
                    }
                } else {
                    importState.value?.let { current ->
                        importState.value = current.copy(selectedCategory = category)
                    }
                }
            }
            categoryState.value = null
        }
    }

    fun dismissCategorySelector() {
        categoryState.value = null
    }
}


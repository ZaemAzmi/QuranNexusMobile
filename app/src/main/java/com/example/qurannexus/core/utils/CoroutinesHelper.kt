package com.example.qurannexus.core.utils

import com.example.qurannexus.features.bookmark.enums.RecentlyReadType
import com.example.qurannexus.features.bookmark.repositories.RecentlyReadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// CoroutinesHelper.kt
object CoroutinesHelper {
    @JvmStatic
    fun addRecentlyRead(
        repository: RecentlyReadRepository,
        type: RecentlyReadType,
        itemId: String,
        durationSeconds: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                when (val result = repository.addRecentlyRead(type, itemId, durationSeconds)) {
                    is Result.Success -> onSuccess()
                    is Result.Error -> onError(result.exception.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
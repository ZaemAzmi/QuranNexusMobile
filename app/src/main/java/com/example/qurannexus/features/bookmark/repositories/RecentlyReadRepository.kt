package com.example.qurannexus.features.bookmark.repositories
import com.example.qurannexus.core.exceptions.AuthException
import com.example.qurannexus.core.utils.Result
import com.example.qurannexus.core.utils.TokenManager
import com.example.qurannexus.features.bookmark.enums.RecentlyReadType
import com.example.qurannexus.features.bookmark.interfaces.BookmarkApi
import com.example.qurannexus.features.bookmark.models.AddRecentlyReadRequest
import com.example.qurannexus.features.bookmark.models.RecentlyRead
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RecentlyReadRepository @Inject constructor(
    private val bookmarkApi: BookmarkApi,
    private val tokenManager: TokenManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getRecentlyRead(type: RecentlyReadType): Result<List<RecentlyRead>> {
        return withContext(dispatcher) {
            try {
                val token = tokenManager.getToken()
                    ?: return@withContext Result.error(AuthException(AuthException.NOT_LOGGED_IN))

                val response = bookmarkApi.getRecentlyRead("Bearer $token")

                if (response.status == "success") {
                    val items = when (type) {
                        RecentlyReadType.CHAPTER -> response.recentlyRead.chapters
                        RecentlyReadType.PAGE -> response.recentlyRead.pages
                        RecentlyReadType.JUZ -> response.recentlyRead.juzs
                    }
                    Result.success(items)
                } else {
                    Result.error(Exception(response.message ?: "Unknown error occurred"))
                }
            } catch (e: Exception) {
                Result.error(e)
            }
        }
    }

    suspend fun addRecentlyRead(type: RecentlyReadType, itemId: String, durationSeconds: Long): Result<Unit> {
        return withContext(dispatcher) {
            try {
                val token = tokenManager.getToken()
                    ?: return@withContext Result.error(AuthException(AuthException.NOT_LOGGED_IN))

                val request = AddRecentlyReadRequest(
                    type = type.toApiString(),
                    itemId = itemId,
                    durationSeconds = durationSeconds
                )

                val response = bookmarkApi.addRecentlyRead("Bearer $token", request)

                if (response.status == "success") {
                    Result.success(Unit)
                } else {
                    Result.error(Exception(response.message ?: "Unknown error occurred"))
                }
            } catch (e: Exception) {
                Result.error(e)
            }
        }
    }

    suspend fun removeRecentlyRead(item: RecentlyRead, type: RecentlyReadType): Result<Unit> {
        return withContext(dispatcher) {
            try {
                val token = tokenManager.getToken()
                    ?: return@withContext Result.error(AuthException(AuthException.NOT_LOGGED_IN))

                val response = bookmarkApi.removeRecentlyRead(
                    "Bearer $token",
                    type.toApiString(),
                    item.itemId
                )

                if (response.status == "success") {
                    Result.success(Unit)
                } else {
                    Result.error(Exception(response.message ?: "Unknown error occurred"))
                }
            } catch (e: Exception) {
                Result.error(e)
            }
        }
    }
}
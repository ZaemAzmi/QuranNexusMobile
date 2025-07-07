package com.example.qurannexus.features.bookmark

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.core.utils.TokenManager
import com.example.qurannexus.features.bookmark.models.BookmarkVerse
import com.example.qurannexus.features.bookmark.models.BookmarkVersesAdapter
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.recitation.data.RecitationDao
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class BookmarkVersesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookmarkVersesAdapter: BookmarkVersesAdapter
    private lateinit var quranApi: QuranApi
    @Inject
    lateinit var recitationDao: RecitationDao
    @Inject
    lateinit var tokenManager: TokenManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookmark_verse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.bookmarkVersesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        bookmarkVersesAdapter = BookmarkVersesAdapter(emptyList(), recitationDao, this)
        recyclerView.adapter = bookmarkVersesAdapter

        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        fetchBookmarks()

    }

    private fun fetchBookmarks() {
        val currentToken = tokenManager.getToken()

        // Check if the fresh token is null or empty.
        if (currentToken.isNullOrEmpty()) {
            if (isAdded) {
                Toast.makeText(context, "Please login to view bookmarks", Toast.LENGTH_SHORT).show()
            }
            return // Stop execution if there's no token.
        }
        quranApi.getBookmarks("Bearer $currentToken").enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                if (response.isSuccessful) {
                    // Use a safe-call `?.` instead of the `!!` operator
                    val bookmarksResponse = response.body()

                    // Check if the body and the nested properties are not null
                    if (bookmarksResponse?.status == "success") {
                        val verseList = bookmarksResponse.bookmarks?.verses
                        if (verseList == null) {
                            if (isAdded) {
                                Toast.makeText(context, "No bookmarked verses found", Toast.LENGTH_SHORT).show()
                            }
                            // Update the adapter with an empty list to clear any old data
                            bookmarkVersesAdapter.updateData(emptyList())
                            return // Exit the onResponse block
                        }
                        val verses = verseList.mapNotNull { verseBookmark ->
                            // The 'mapNotNull' will automatically filter out any null results from the mapping
                            try {
                                // This part is fine
                                BookmarkVerse(
                                    itemProperties = verseBookmark.itemProperties,
                                    notes = verseBookmark.notes,
                                    createdAt = verseBookmark.createdAt
                                )
                            } catch (e: Exception) {
                                null // Return null if any error occurs, mapNotNull will discard it
                            }
                        }
                        bookmarkVersesAdapter.updateData(verses)

                    } else if (isAdded) {
                        // Handle the case where the status is not "success"
                        Toast.makeText(context, "Failed to load bookmarks: Invalid response", Toast.LENGTH_SHORT).show()
                    }
                } else if(isAdded) {
                    // Handle unsuccessful HTTP responses (e.g., 401, 404, 500)
                    Toast.makeText(context, "Error: ${response.code()} ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                if (isAdded) {
                    Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
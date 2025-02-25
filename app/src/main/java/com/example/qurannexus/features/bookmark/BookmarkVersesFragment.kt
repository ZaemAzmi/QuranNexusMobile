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
import com.example.qurannexus.features.bookmark.models.BookmarkVerse
import com.example.qurannexus.features.bookmark.models.BookmarkVersesAdapter
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@AndroidEntryPoint
class BookmarkVersesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookmarkVersesAdapter: BookmarkVersesAdapter
    private lateinit var quranApi: QuranApi
    private var authToken: String? = null
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

        bookmarkVersesAdapter = BookmarkVersesAdapter(emptyList())
        recyclerView.adapter = bookmarkVersesAdapter

        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)
        if (authToken != null) {
            fetchBookmarks()
        } else {
            Toast.makeText(context, "Please login to view bookmarks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchBookmarks() {
        quranApi.getBookmarks("Bearer $authToken").enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val bookmarksResponse = response.body()!!
                    if (bookmarksResponse.status == "success") {
                        val verses = bookmarksResponse.bookmarks.verses.map { verseBookmark ->
                            try {
                                // Get chapter details from QuranMetadata
                                val chapterNumber = verseBookmark.itemProperties.chapterId.toInt()
                                val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterNumber)

                                BookmarkVerse(
                                    itemProperties = BookmarkVerse.VerseProperties(
                                        verseId = verseBookmark.itemProperties.verseId,
                                        chapterId = verseBookmark.itemProperties.chapterId
                                    ),
                                    notes = verseBookmark.notes,
                                    createdAt = verseBookmark.createdAt
                                )
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }.filterNotNull()

                        if (verses.isEmpty()) {
                            Toast.makeText(context, "No bookmarked verses found", Toast.LENGTH_SHORT).show()
                        }
                        bookmarkVersesAdapter.updateData(verses)
                    } else {
                        Toast.makeText(context, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
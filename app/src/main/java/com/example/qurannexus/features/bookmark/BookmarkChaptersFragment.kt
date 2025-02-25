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
import com.example.qurannexus.features.bookmark.models.BookmarkChapter
import com.example.qurannexus.features.bookmark.models.BookmarkChaptersAdapter
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
@AndroidEntryPoint
class BookmarkChaptersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookmarkChaptersAdapter: BookmarkChaptersAdapter
    private lateinit var quranApi: QuranApi
    private var authToken: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookmark_chapter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.bookmarkChaptersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        bookmarkChaptersAdapter = BookmarkChaptersAdapter(emptyList())
        recyclerView.adapter = bookmarkChaptersAdapter

        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)

        // Get token from SharedPreferences
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
                if (!isAdded) return
                if (response.isSuccessful && response.body() != null) {
                    val bookmarksResponse = response.body()!!
                    if (bookmarksResponse.status == "success") {
                        val chapters = bookmarksResponse.bookmarks.chapters
                        if(chapters.isEmpty()){
                            Toast.makeText(context, "No bookmarked chapters found", Toast.LENGTH_SHORT).show()
                            return
                        }
                        // Map the chapters with QuranMetadata
                        val bookmarkChapters = chapters.mapNotNull { chapter ->
                            try {
                                val chapterNumber = chapter.itemProperties.chapterId.toInt()
                                val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterNumber)

                                surahDetails?.let {
                                    BookmarkChapter(
                                        itemProperties = BookmarkChapter.ChapterProperties(
                                            chapterId = chapter.itemProperties.chapterId,
                                            chapterNumber = it.surahIndex,
                                            chapterTitle = it.englishName,
                                            chapterInfo = it.revelationPlace,
                                            arabicTitle = it.arabicName,
                                            verseCount = it.numberOfVerses,
                                            translationName = it.translationName
                                        ),
                                        notes = chapter.notes,
                                        createdAt = chapter.createdAt
                                    )
                                }
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }

                        if (isAdded) {  // Check again before updating UI
                            bookmarkChaptersAdapter.updateData(bookmarkChapters)
                        }
                    } else {
                        Toast.makeText(context, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
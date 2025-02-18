package com.example.qurannexus.features.bookmark

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.bookmark.models.BookmarkPage
import com.example.qurannexus.features.bookmark.models.BookmarkPagesAdapter
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookmarkPagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookmarkPagesAdapter: BookmarkPagesAdapter
    private lateinit var quranApi: QuranApi
    private lateinit var emptyMessage: TextView
    private var authToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookmark_pages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.bookmarkPagesRecyclerView)
        emptyMessage = view.findViewById(R.id.emptyPagesMessage)
        recyclerView.layoutManager = LinearLayoutManager(context)

        bookmarkPagesAdapter = BookmarkPagesAdapter(emptyList())
        recyclerView.adapter = bookmarkPagesAdapter

        setupQuranApi()
        fetchBookmarks()
    }

    private fun setupQuranApi() {
        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)
    }

    private fun fetchBookmarks() {
        if (authToken == null) {
            Toast.makeText(context, "Please login to view bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        quranApi.getBookmarks("Bearer $authToken").enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(
                call: Call<BookmarksResponse>,
                response: Response<BookmarksResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val bookmarksResponse = response.body()!!
                    if (bookmarksResponse.status == "success") {
                        val pages = bookmarksResponse.bookmarks.pages.map { pageBookmark ->
                            try {
                                // Get chapter details from QuranMetadata for the first surah on this page
                                val surahNumber = QuranMetadata.getInstance()
                                    .getSurahNumberForPage(pageBookmark.itemProperties.pageNumber)
                                val surahDetails = QuranMetadata.getInstance().getSurahDetails(surahNumber)

                                BookmarkPage(
                                    itemProperties = pageBookmark.itemProperties,
                                    notes = pageBookmark.notes,
                                    createdAt = pageBookmark.createdAt
                                )
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }.filterNotNull()

                        updateUI(pages)
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

    private fun updateUI(pages: List<BookmarkPage>) {
        if (!isAdded) return

        if (pages.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyMessage.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyMessage.visibility = View.GONE
            bookmarkPagesAdapter.updateData(pages)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchBookmarks() // Refresh bookmarks when fragment becomes visible
    }
}
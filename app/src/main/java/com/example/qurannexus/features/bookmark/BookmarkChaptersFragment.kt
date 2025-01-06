package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.bookmark.interfaces.BookmarkService
import com.example.qurannexus.features.bookmark.models.BookmarkChapter
import com.example.qurannexus.features.bookmark.models.BookmarkChaptersAdapter
import kotlinx.coroutines.launch

class BookmarkChaptersFragment : Fragment() {

    private lateinit var chaptersList: List<BookmarkChapter>
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookmarkChaptersAdapter: BookmarkChaptersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmark_chapter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.bookmarkChaptersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        bookmarkChaptersAdapter = BookmarkChaptersAdapter(emptyList())
        recyclerView.adapter = bookmarkChaptersAdapter

        fetchBookmarks()
    }

    private fun fetchBookmarks() {
        val retrofit = ApiService.getQuranClient()
        val service = retrofit.create(BookmarkService::class.java)

        lifecycleScope.launch {
            try {
                val response = service.getBookmarks()
                if (response.isSuccessful && response.body() != null) {
                    val bookmarks = response.body()!!.bookmarks
                        .filter { it.name != null } // Filter chapters only
                        .map { bookmark ->
                            BookmarkChapter(
                                bookmark._id.toInt(),
                                bookmark.tname ?: "Unknown",
                                bookmark.ename ?: "Unknown"
                            )
                        }
                    chaptersList = bookmarks
                    bookmarkChaptersAdapter.updateData(chaptersList)
                } else {
                    Toast.makeText(context, "Failed to fetch bookmarks", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

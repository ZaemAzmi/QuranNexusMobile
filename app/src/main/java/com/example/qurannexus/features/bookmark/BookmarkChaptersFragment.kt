package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkChapter
import com.example.qurannexus.features.bookmark.models.BookmarkChaptersAdapter

class BookmarkChaptersFragment : Fragment() {

    private lateinit var chaptersList: List<BookmarkChapter>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmark_chapter, container, false)
    }

    override fun onViewCreated(view : View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        // Example data
        chaptersList = listOf(
            BookmarkChapter(1, "Al-Fatiha", "The Opening"),
            BookmarkChapter(99, "Al-Baqarah", "The Cow"),
            BookmarkChapter(114, "Al-Imran", "The Family of Imran"),
            // Add more chapters as needed
        )
        val recyclerView = view.findViewById<RecyclerView>(R.id.bookmarkChaptersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val bookmarkChaptersAdapter = BookmarkChaptersAdapter(chaptersList)
        recyclerView.adapter = bookmarkChaptersAdapter
    }

}
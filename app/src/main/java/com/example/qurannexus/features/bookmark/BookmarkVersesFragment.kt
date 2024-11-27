package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkVerse
import com.example.qurannexus.features.bookmark.models.BookmarkVersesAdapter

class BookmarkVersesFragment : Fragment() {

    private lateinit var bookmarkVersesList: List<BookmarkVerse>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onViewCreated(view : View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        bookmarkVersesList = listOf(
            BookmarkVerse(1, "Al-Fatiha", "The Opening"),
            BookmarkVerse(2, "Al-Baqarah", "The Cow"),
            BookmarkVerse(3, "Al-Imran", "The Family of Imran"),
            // Add more chapters as needed
        )
        val recyclerView = view.findViewById<RecyclerView>(R.id.bookmarkVersesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val bookmarkVersesAdapter = BookmarkVersesAdapter(bookmarkVersesList)
        recyclerView.adapter = bookmarkVersesAdapter


    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmark_verse, container, false)
    }

}
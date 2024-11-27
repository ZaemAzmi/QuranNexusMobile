package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkQuote
import com.example.qurannexus.features.bookmark.models.BookmarkQuotesAdapter

class BookmarkQuotesFragment : Fragment() {

    private lateinit var bookmarkQuotesList: List<BookmarkQuote>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onViewCreated(view : View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        bookmarkQuotesList = listOf(
            BookmarkQuote(1, "Al-Fatiha", "The Opening"),
            BookmarkQuote(2, "Al-Baqarah", "The Cow"),
            BookmarkQuote(3, "Al-Imran", "The Family of Imran"),
            // Add more chapters as needed
        )
        val recyclerView = view.findViewById<RecyclerView>(R.id.bookmarkQuotesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val bookmarkQuotesAdapter = BookmarkQuotesAdapter(bookmarkQuotesList)
        recyclerView.adapter = bookmarkQuotesAdapter
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmark_quotes, container, false)
    }


}
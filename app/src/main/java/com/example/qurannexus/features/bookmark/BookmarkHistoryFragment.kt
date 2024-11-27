package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkHistory
import com.example.qurannexus.features.bookmark.models.BookmarkHistoriesAdapter

class BookmarkHistoryFragment : Fragment() {

    private lateinit var historiesList: List<BookmarkHistory>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmark_history, container, false)
    }
    override fun onViewCreated(view : View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        historiesList = listOf(
            BookmarkHistory(1, "Al-Fatiha", "The Opening"),
            BookmarkHistory(2, "Al-Baqarah", "The Cow"),
            BookmarkHistory(3, "Al-Imran", "The Family of Imran"),
            // Add more chapters as needed
        )
        val recyclerView = view.findViewById<RecyclerView>(R.id.bookmarkHistoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val bookmarkHistoriesAdapter = BookmarkHistoriesAdapter(historiesList)
        recyclerView.adapter = bookmarkHistoriesAdapter

    }

}
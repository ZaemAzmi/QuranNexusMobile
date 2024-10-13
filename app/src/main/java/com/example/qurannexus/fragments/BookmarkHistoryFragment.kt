package com.example.qurannexus.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.models.BookmarkChapter
import com.example.qurannexus.models.BookmarkHistory
import com.example.qurannexus.models.adapters.BookmarkHistoriesAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BookmarkHistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarkHistoryFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var historiesList: List<BookmarkHistory>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BookmarkHistoryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BookmarkHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
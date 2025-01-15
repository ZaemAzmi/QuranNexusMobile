package com.example.qurannexus.features.bookmark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.R
import com.example.qurannexus.databinding.FragmentBookmarkWordsBinding
import com.example.qurannexus.features.bookmark.models.BookmarkWord
import com.example.qurannexus.features.home.WordDetailsActivity
import com.example.qurannexus.features.home.models.WordDetails
import com.example.qurannexus.features.words.AccordionAdapter
import com.example.qurannexus.features.words.AccordionSection
import com.example.qurannexus.features.words.models.WordManagementViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarkWordsFragment : Fragment() {

    private var _binding: FragmentBookmarkWordsBinding? = null
    private val binding get() = _binding!!
    private lateinit var accordionAdapter: AccordionAdapter
    private val viewModel: WordManagementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBookmarkWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchBookmarkedWords()
    }

    private fun setupRecyclerView() {
        accordionAdapter = AccordionAdapter { wordDetail ->
            // Navigate to WordDetailsActivity when word is clicked
            navigateToWordDetails(wordDetail)
        }

        binding.recyclerViewAccordion.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accordionAdapter
        }
    }

    private fun fetchBookmarkedWords() {
        // Get token from SharedPreferences
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Toast.makeText(context, "Please login to view bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.getBookmarks(token).observe(viewLifecycleOwner) { response ->
            if (response.status == "success") {
                val bookmarkedWords = response.bookmarks.words

                // Group words by first Arabic letter
                val groupedWords = bookmarkedWords.groupBy { word ->
                    word.word_text.first().toString()
                }

                // Create accordion sections
                val sections = groupedWords.map { (letter, words) ->
                    AccordionSection(
                        title = letter,
                        words = words
                    )
                }.sortedBy { it.title } // Sort sections alphabetically

                accordionAdapter.submitList(sections)
            }
        }
    }

    private fun navigateToWordDetails(word: BookmarkWord) {
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra("WORD_ID", word.word_id)
            putExtra("WORD_TEXT", word.word_text)
            putExtra("TRANSLATION", word.translation)
            putExtra("TRANSLITERATION", word.transliteration)
            putExtra("SURAH_NAME", word.surah_name)
            putExtra("AYAH_KEY", word.ayah_key)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

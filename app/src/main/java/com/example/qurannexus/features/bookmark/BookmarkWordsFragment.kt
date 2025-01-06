package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.R
import com.example.qurannexus.databinding.FragmentBookmarkWordsBinding
import com.example.qurannexus.features.home.models.WordDetails
import com.example.qurannexus.features.words.AccordionAdapter
import com.example.qurannexus.features.words.AccordionSection

class BookmarkWordsFragment : Fragment() {

    private var _binding: FragmentBookmarkWordsBinding? = null
    private val binding get() = _binding!!

    private lateinit var accordionAdapter: AccordionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarkWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadTemporaryData()
    }

    private fun setupRecyclerView() {
        accordionAdapter = AccordionAdapter()
        binding.recyclerViewAccordion.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accordionAdapter
        }
    }

    private fun loadTemporaryData() {
        // Mock data grouped by Arabic letters
        val mockData = mapOf(
            "أ" to listOf(
                WordDetails(id = "1", text = "أَسْتَغْفِرُ", translation = "I seek forgiveness"),
                WordDetails(id = "2", text = "ٱلْحَمْدُ", translation = "Ahmad")
            ),
            "ب" to listOf(
                WordDetails(id = "3", text = "بِسْمِ", translation = "In the name"),
                WordDetails(id = "4", text = "بَرَكَةٌ", translation = "Blessing")
            )
        )

        val sections = mockData.map { (title, words) ->
            AccordionSection(title, words)
        }

        accordionAdapter.submitList(sections)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


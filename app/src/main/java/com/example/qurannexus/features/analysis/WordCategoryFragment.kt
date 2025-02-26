package com.example.qurannexus.features.analysis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.adapters.CategoryWordsAdapter
import com.example.qurannexus.features.words.WordDetailsActivity
import dagger.hilt.android.AndroidEntryPoint

data class CategoryWord(
    val text: String,
    val translation: String,
    val occurrences: Int,
    val description: String
)

@AndroidEntryPoint
class WordCategoryFragment : Fragment() {

    private lateinit var backButton: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var wordsRecyclerView: RecyclerView

    private var categoryType: String = ""

    // Sample data for each category
    private val heavenWords = listOf(
        CategoryWord("جنة", "Jannah (Garden/Paradise)", 77, "The word for paradise or garden, describing the eternal abode of believers"),
        CategoryWord("فردوس", "Firdaws", 2, "The highest level of Paradise"),
        CategoryWord("نعيم", "Na'eem", 17, "Bliss or delight in Paradise"),
        CategoryWord("طوبى", "Tooba", 1, "Blessedness or goodness in Paradise"),
        CategoryWord("سلام", "Salaam", 42, "Peace, a greeting in Paradise")
    )

    private val hellWords = listOf(
        CategoryWord("جهنم", "Jahannam", 77, "The main word for Hell in the Quran"),
        CategoryWord("نار", "Naar", 145, "Fire, often referring to hellfire"),
        CategoryWord("سعير", "Sa'eer", 16, "Blazing fire"),
        CategoryWord("جحيم", "Jaheem", 26, "Intense fire"),
        CategoryWord("حطمة", "Hutamah", 2, "That which breaks to pieces, a name for Hell")
    )

    private val prophetWords = listOf(
        CategoryWord("محمد", "Muhammad", 4, "The final prophet of Islam"),
        CategoryWord("إبراهيم", "Ibrahim", 69, "Abraham, the friend of Allah"),
        CategoryWord("موسى", "Musa", 136, "Moses, who received the Torah"),
        CategoryWord("عيسى", "Isa", 25, "Jesus, the son of Mary"),
        CategoryWord("نوح", "Nuh", 43, "Noah, who built the ark")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryType = it.getString(ARG_CATEGORY_TYPE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupCategoryInfo()
        loadCategoryWords()
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        titleTextView = view.findViewById(R.id.titleTextView)
        descriptionTextView = view.findViewById(R.id.descriptionTextView)
        wordsRecyclerView = view.findViewById(R.id.wordsRecyclerView)

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set up recycler view
        wordsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupCategoryInfo() {
        when (categoryType) {
            "heaven" -> {
                titleTextView.text = "Heaven (Jannah)"
                descriptionTextView.text = "Words related to paradise, rewards, and blessings in the Quran. These words describe the eternal abode promised to the believers."
            }
            "hell" -> {
                titleTextView.text = "Hell (Jahannam)"
                descriptionTextView.text = "Words related to hellfire, punishment, and consequences in the Quran. These words describe the place of punishment for disbelievers and wrongdoers."
            }
            "prophets" -> {
                titleTextView.text = "Prophets (Anbiya)"
                descriptionTextView.text = "Names and mentions of prophets throughout the Quran. The Quran mentions 25 prophets by name, with stories and lessons from their lives."
            }
        }
    }

    private fun loadCategoryWords() {
        // Get words based on category type
        val words = when (categoryType) {
            "heaven" -> heavenWords
            "hell" -> hellWords
            "prophets" -> prophetWords
            else -> emptyList()
        }

        // Set up adapter
        val adapter = CategoryWordsAdapter(words) { word ->
            // Handle word click - navigate to word details
            val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
                putExtra("WORD_TEXT", word.text)
                putExtra("TRANSLATION", word.translation)
            }
            startActivity(intent)
        }

        wordsRecyclerView.adapter = adapter
    }

    companion object {
        private const val ARG_CATEGORY_TYPE = "category_type"

        fun newInstance(categoryType: String): WordCategoryFragment {
            return WordCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_TYPE, categoryType)
                }
            }
        }
    }
}
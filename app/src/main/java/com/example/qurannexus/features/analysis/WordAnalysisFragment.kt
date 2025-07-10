package com.example.qurannexus.features.analysis

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.analysis.adapters.FrequentWordsAdapter
import com.example.qurannexus.features.analysis.enums.SearchType
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot
import com.example.qurannexus.features.analysis.viewmodels.WordAnalysisViewModel
import com.example.qurannexus.features.words.WordDetailsActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class WordAnalysisFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi
    private val viewModel: WordAnalysisViewModel by viewModels() // Inject ViewModel
    private lateinit var backButton: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var rootCategoryCard: CardView
    private lateinit var lemmaCategoryCard: CardView
    private lateinit var othersCategoryCard: CardView
    private lateinit var wordFactTextView: TextView
    private lateinit var wordFactButton: Button
    private lateinit var frequentWordsRecyclerView: RecyclerView
    private lateinit var frequentWordsAdapter: FrequentWordsAdapter
    private lateinit var chipGroupSearchFilter : ChipGroup
    private lateinit var wordDefinitionInfoIcon: ImageView
    private lateinit var searchHelpButton: LinearLayout
    private lateinit var searchHelpLayout: LinearLayout
    private lateinit var wordDefinitionInfoCard: MaterialCardView
    private var isInSearchMode = false // To track if recycler is showing search results or frequent words
    private val wordFacts = listOf(
        "The word 'Rahman' (الرحمن) and 'Raheem' (الرحيم) which refer to Allah's mercy appear 57 and 114 times respectively in the Quran.",
        "The word 'Salat' (prayer) is mentioned 67 times in the Quran.",
        "The word 'Jannah' (paradise) is mentioned 77 times in the Quran.",
        "The word 'Jahannam' (hellfire) is mentioned 77 times in the Quran.",
        "The word 'Yawm' (day) is mentioned 365 times in the Quran.",
        "The word 'Shahr' (month) is mentioned 12 times in the Quran.",
        "The word 'Allah' is mentioned 2,699 times in the Quran."
    )
    private var currentFactIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView() // Renamed from setupFrequentWords
        setupClickListeners()
        observeViewModel()
        displayRandomFact() // Keep if you want this
        viewModel.fetchFrequentEntries() // Fetch data
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        chipGroupSearchFilter = view.findViewById(R.id.chipGroupSearchFilter)
        frequentWordsRecyclerView = view.findViewById(R.id.frequentWordsRecyclerView)
        rootCategoryCard = view.findViewById(R.id.rootCategoryCard)
        lemmaCategoryCard = view.findViewById(R.id.lemmaCategoryCard)
        othersCategoryCard = view.findViewById(R.id.othersCategoryCard)
        wordFactTextView = view.findViewById(R.id.wordFactTextView)
        wordFactButton = view.findViewById(R.id.wordFactButton)
        wordDefinitionInfoCard = view.findViewById(R.id.wordDefinitionInfoCard)
        searchHelpButton = view.findViewById(R.id.searchHelpButton) // Update ID
        searchHelpLayout = view.findViewById(R.id.searchHelpLayout)
    }
    private fun setupRecyclerView() {
        frequentWordsAdapter = FrequentWordsAdapter { displayableRoot ->
            navigateToWordDetails(displayableRoot)
        }
        frequentWordsRecyclerView.adapter = frequentWordsAdapter
        // You might want to set a LayoutManager if not set in XML
        // frequentWordsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false) // Example
    }


    private fun observeViewModel() {
        viewModel.frequentEntries.observe(viewLifecycleOwner) { roots ->
            if (!isInSearchMode) { // Only update if not in search mode
                frequentWordsAdapter.submitList(roots)
            }
        }
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (isInSearchMode) { // Only update if in search mode
                if (results.isEmpty()) {
                    Toast.makeText(requireContext(), "No results found for your search.", Toast.LENGTH_SHORT).show()
                }
                frequentWordsAdapter.submitList(results)
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Handle loading state (e.g., for a general progress bar)
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }
    private fun setupClickListeners() {
        // Back button click
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        searchHelpButton.setOnClickListener { // Listener is now on the LinearLayout
            if (searchHelpLayout.visibility == View.VISIBLE) {
                searchHelpLayout.visibility = View.GONE
            } else {
                searchHelpLayout.visibility = View.VISIBLE
            }
        }
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                val selectedChipId = chipGroupSearchFilter.checkedChipId
                Log.d("WordAnalysisFragment", "Selected Chip ID: $selectedChipId") // LOG THIS
                val searchType = when (selectedChipId) {
                    R.id.chipFilterArabicForm -> SearchType.ARABIC_FORM
                    R.id.chipFilterTranslation -> SearchType.TRANSLATION
                    R.id.chipFilterIdentifier -> SearchType.IDENTIFIER
                    R.id.chipFilterAll -> SearchType.GENERAL
                    else -> {
                        Log.w(
                            "WordAnalysisFragment",
                            "Unknown chip ID: $selectedChipId, defaulting to ALL"
                        )
                        SearchType.GENERAL // Default
                    }
                }
                Log.d("WordAnalysisFragment", "Determined SearchType: ${searchType.name}") // LOG THIS
                navigateToSearchResults(query, searchType)
            } else {
                Toast.makeText(requireContext(), "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }

        rootCategoryCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_root_placeholder,
                title = "Quranic Roots (أَصْل - Aṣl)",
                explanation = "A root is the fundamental, consonantal base of a word, typically consisting of three (triliteral) letters. It carries a core, abstract meaning from which various related words are derived through patterns.",
                examples = "From the root ك-ت-ب (k-t-b), related to 'writing', we get:\n • كَتَبَ (kataba) - 'he wrote'\n • كِتَاب (kitāb) - 'book'\n • كَاتِب (kātib) - 'writer'",
                appRelevance = "Searching by root reveals all Quranic words sharing that fundamental meaning, highlighting thematic connections across the text."
            )
        }

        lemmaCategoryCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_lemma_placeholder,
                title = "Lemmas",
                explanation = "A lemma is the dictionary or canonical form of a word or a word's base form that does not derive from a standard three-letter root. It groups together different inflections (e.g., plural, different cases) of the same word. Nouns have lemmas, but many particles are identified by Special Groups instead.",
                examples = "Examples from the database include:\n" +
                        "• The particle 'لَا' (lā), meaning 'no'.\n" +
                        "• The preposition 'فِى' (fī), meaning 'in'.\n" +
                        "• The adverbial phrase 'يَوْمَئِذٍ' (yawma-idhin), meaning 'that Day'.",
                appRelevance = "Analyzing by lemma groups different grammatical forms of a word under one entry, perfect for tracking a specific concept regardless of its case or number."
            )
        }

        othersCategoryCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_extension,
                title = "Others / Special Groups",
                explanation = "This category is assigned to any Quranic word that doesn't fit the standard root or lemma structure. It primarily includes particles (prepositions, conjunctions), pronouns, and crucially, the disconnected letters (Muqatta'at) at the start of some chapters.",
                examples = "• Pronouns, such as 'هُوَ' (huwa), meaning 'He'.\n" +
                        "• Disconnected letters like 'الم' (Alif, Lām, Mīm) from Surah Al-Baqarah.\n" +
                        "• Longer sets of disconnected letters like 'كٓهيعٓصٓ' (Kāf-Hā-Yā-ʿAin-Ṣād) from Surah Maryam.",
                appRelevance = "This allows for the analysis of the functional building blocks of the Quran—the particles that connect ideas, and unique symbolic forms like chapter initials."
            )
        }

        wordDefinitionInfoCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_show_hint,
                title = getString(R.string.tokenization_title),
                explanation = getString(R.string.tokenization_explanation),
                examples = getString(R.string.tokenization_examples),
                appRelevance = getString(R.string.tokenization_relevance)
            )
        }

        // Next fact button click
        wordFactButton.setOnClickListener {
            displayRandomFact()
        }
    }
    private fun showCategoryExplanationDialog(iconResId: Int, title: String, explanation: String, examples: String, appRelevance: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_word_category_explanation, null)

        val ivIcon: ImageView = dialogView.findViewById(R.id.ivDialogCategoryIcon)
        val tvTitle: TextView = dialogView.findViewById(R.id.tvDialogCategoryTitle)
        val tvExplanation: TextView = dialogView.findViewById(R.id.tvDialogCategoryExplanation)
        val tvExamples: TextView = dialogView.findViewById(R.id.tvDialogCategoryExamples)
        val tvAppRelevance: TextView = dialogView.findViewById(R.id.tvDialogAppRelevance)
        val btnClose: Button = dialogView.findViewById(R.id.btnDialogCategoryClose)

        ivIcon.setImageResource(iconResId) // Make sure you have these drawables
        tvTitle.text = title
        tvExplanation.text = explanation
        tvExamples.text = examples
        tvAppRelevance.text = appRelevance

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        // Optional: Make dialog background transparent if your dialog_category_explanation.xml has its own rounded background
        // dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
    private fun navigateToWordDetails(selectedEntry: DisplayableFrequentRoot) { // Renamed selectedRoot to selectedEntry
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra(WordDetailsActivity.EXTRA_IDENTIFIER_VALUE, selectedEntry.identifierValue) // Pass the root/lemma/form string
            putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, selectedEntry.displayArabicText)
        }
        startActivity(intent)
    }
    private fun displayRandomFact() {
        wordFactTextView.text = wordFacts[currentFactIndex]
        currentFactIndex = (currentFactIndex + 1) % wordFacts.size
    }

    private fun navigateToSearchResults(query: String, searchType: SearchType) {
        Log.d("WordAnalysisFragment", "Navigating to search results with query: '$query', type: ${searchType.name}")
        val fragment = WordSearchResultsFragment.newInstance(query, searchType.name) // Pass searchType.name (String)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): WordAnalysisFragment {
            return WordAnalysisFragment()
        }
    }
    override fun onResume() { // Or use a specific back handling mechanism
        super.onResume()
        if (searchEditText.text.isEmpty()) {
            if (isInSearchMode) { // If we were in search mode but now query is empty
                isInSearchMode = false
                viewModel.fetchFrequentEntries() // Refresh frequent words
            }
        }
    }
}
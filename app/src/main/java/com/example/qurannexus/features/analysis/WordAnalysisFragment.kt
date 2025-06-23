package com.example.qurannexus.features.analysis

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import com.example.qurannexus.features.words.models.WordOccurrenceResponse
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    private lateinit var formCategoryCard: CardView
    private lateinit var wordFactTextView: TextView
    private lateinit var wordFactButton: Button
    private lateinit var frequentWordsRecyclerView: RecyclerView
    private lateinit var frequentWordsAdapter: FrequentWordsAdapter
    private lateinit var chipGroupSearchFilter : ChipGroup
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
        formCategoryCard = view.findViewById(R.id.formCategoryCard)
        wordFactTextView = view.findViewById(R.id.wordFactTextView)
        wordFactButton = view.findViewById(R.id.wordFactButton)
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

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                val selectedChipId = chipGroupSearchFilter.checkedChipId
                Log.d("WordAnalysisFragment", "Selected Chip ID: $selectedChipId") // LOG THIS
                val searchType = when (selectedChipId) {
                    R.id.chipFilterRootLabel -> SearchType.ROOT_LABEL
                    R.id.chipFilterArabicForm -> SearchType.ARABIC_FORM
                    R.id.chipFilterTranslation -> SearchType.TRANSLATION
                    R.id.chipFilterAll -> SearchType.ALL
                    else -> {
                        Log.w(
                            "WordAnalysisFragment",
                            "Unknown chip ID: $selectedChipId, defaulting to ALL"
                        )
                        SearchType.ALL // Default
                    }
                }
                Log.d("WordAnalysisFragment", "Determined SearchType: ${searchType.name}") // LOG THIS
                navigateToSearchResults(query, searchType)
            } else {
                Toast.makeText(requireContext(), "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }


        // New Category Card Click Listeners
        rootCategoryCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_root_placeholder, // Replace with your actual icon
                title = "Quranic Roots (أَصْل - Aṣl)",
                explanation = "A root is typically a three-letter (triliteral), or sometimes four-letter, consonantal base that conveys a core meaning. Arabic words are formed by applying various patterns (أَوْزَان - awzān) to these roots, infusing them with specific grammatical functions and shades of meaning.",
                examples = "Root: ك-ت-ب (k-t-b) - relates to 'writing'.\n • كَتَبَ (kataba) - 'he wrote' (verb)\n • كِتَاب (kitāb) - 'book' (noun)\n • مَكْتَبَة (maktabah) - 'library'\n • كَاتِب (kātib) - 'writer'",
                appRelevance = "Analyzing by ROOT reveals all Quranic words sharing that fundamental meaning, highlighting thematic connections."
            )
        }

        lemmaCategoryCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_lemma_placeholder, // Replace
                title = "Lemmas (مَدْخَل - Madkhal)",
                explanation = "A lemma is the dictionary or base form of a word. For verbs, it's usually the third-person masculine singular perfect tense (e.g., فَعَلَ). For nouns, it's the singular, indefinite, nominative form. Particles, prepositions, and proper names also have lemmas even if they don’t come from a root.",
                examples = " • Lemma of 'يكتبون' (they write): كَتَبَ (kataba)\n • Lemma of 'المسلمين' (the Muslims): مُسْلِم (muslim)\n • Lemma of 'فِي' (in): فِي (fī)",
                appRelevance = "Analyzing by LEMMA groups different word forms under a single base entry. This is useful for understanding how a word is used regardless of its grammatical case or affixes."
            )
        }

        formCategoryCard.setOnClickListener {
            showCategoryExplanationDialog(
                iconResId = R.drawable.ic_form_placeholder, // Replace
                title = "Specific Forms (شَكْل - Shakl)", // Or "Ṣīghah Khāṣṣah" if you prefer the Arabic terminology
                explanation = "In our analysis, some Quranic words or word segments are primarily identified by their specific textual form or a common transliterated representation, especially when they don't have a traditional triliteral root or when their lemma covers many variations. This often applies to pronouns, particles, or very common short phrases where a particular vocalization or common combination is grouped under one 'FORM' identifier in our database.",
                examples = "Identifier: humo (Type: FORM)\nThis groups occurrences related to the pronoun 'them/they' often appearing with a specific vocalization or in common constructs. Examples of Quranic words that map to this 'humo' identifier include:\n • هُمْ (hum - 'they')\n • لَهُمْ (lahum - 'to them' / 'for them')\n • وَلَهُمْ (walahum - 'and for them')\n • فَهُمْ (fahum - 'so they')",
                appRelevance = "When you see an entry identified by 'FORM' (like 'humo'), it means we're looking at a collection of actual Quranic word instances that share this specific morphological identifier from our source data. This helps analyze the usage patterns of these particular forms or frequently occurring small word segments throughout the Quran, even if they consist of multiple morphological parts (like a prefix + pronoun)."
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
    private fun navigateToWordCategory(category: String) {
        // Navigate to category fragment
        val fragment = WordCategoryFragment.newInstance(category)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
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
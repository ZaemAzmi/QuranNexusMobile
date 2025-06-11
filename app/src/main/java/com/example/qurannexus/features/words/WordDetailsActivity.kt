package com.example.qurannexus.features.words

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.activities.MainActivity
import com.example.qurannexus.features.words.models.WordDetailsViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import androidx.lifecycle.Observer
import com.example.qurannexus.core.database.entities.EntryArabicFormWithFullDetails
import com.example.qurannexus.core.database.entities.AnalysisEntryEntity
import com.example.qurannexus.features.words.adapters.MorphFormsAdapter
import com.example.qurannexus.features.words.adapters.TranslationsAdapter
import com.example.qurannexus.features.words.adapters.WordOccurrencesAdapter
import com.example.qurannexus.features.words.models.WordOccurrenceDisplayItem
import com.google.android.flexbox.FlexboxLayout
import java.io.IOException

@AndroidEntryPoint
class WordDetailsActivity : AppCompatActivity() {
    private val viewModel: WordDetailsViewModel by viewModels()

    // --- UI Elements - General & Top Bar ---
    private lateinit var progressBar: ProgressBar
    private lateinit var tvActivityTitle: TextView // For the main page title (e.g., "Root Analysis")
    private lateinit var bookmarkButton: ImageView

    // --- UI Elements - Main Identifier Card (was Root Information Card) ---
    private lateinit var tvIdentifierLabel: TextView              // R.id.tvRootLabel - Displays "Root: XYZ" or "Lemma: ABC"
    private lateinit var tvIdentifierTotalOccurrences: TextView   // R.id.tvRootTotalOccurrences - Displays "Total Root Occurrences: 10"
    private lateinit var tvTitleContributingMorph: TextView       // R.id.tvContributingMorphFormsLabel - Title for morph forms section
    private lateinit var flexboxMorphForms: FlexboxLayout         // R.id.flexboxMorphForms
    private lateinit var btnShowAllMorphForms: Button             // R.id.btnShowAllMorphForms

    // --- UI Elements - Arabic Form Selection and Details Card ---
    private lateinit var tvTitleArabicForms: TextView             // Title of this card e.g. "Arabic Forms of this Root"
    private lateinit var spinnerArabicForms: Spinner              // R.id.spinnerArabicForms
    private lateinit var tvSelectedFormArabicText: TextView       // R.id.tvSelectedFormArabicText
    private lateinit var btnPlaySelectedFormAudio: Button         // R.id.btnPlaySelectedFormAudio
    private lateinit var tvSelectedFormTranslation: TextView      // R.id.tvSelectedFormTranslation_main
    private lateinit var ivMoreTranslations: ImageView            // R.id.ivMoreTranslations
    private lateinit var layoutTranslationSection: LinearLayout   // R.id.layoutTranslationSection
    private lateinit var tvSelectedFormTransliteration: TextView  // R.id.tvSelectedFormTransliteration
    private lateinit var tvSelectedFormCharacters: TextView       // R.id.tvSelectedFormCharacters

    // --- UI Elements - First Occurrence Details Card ---
    private lateinit var tvTitleFirstOccurrence: TextView         // Title of this card e.g. "First Occurrence of Root"
    private lateinit var tvFirstOccSurahName: TextView            // R.id.tvFirstOccSurahName
    private lateinit var tvFirstOccVerseText: TextView            // R.id.tvFirstOccVerseText
    private lateinit var tvFirstOccAyahKey: TextView              // R.id.tvFirstOccAyahKey
    private lateinit var tvFirstOccPageId: TextView               // R.id.tvFirstOccPageId
    private lateinit var tvFirstOccJuzId: TextView                // R.id.tvFirstOccJuzId
    private lateinit var tvFirstOccCharacters: TextView           // R.id.tvFirstOccCharacters

    // --- UI Elements - Distribution Analysis Card ---
    private lateinit var tvTitleDistribution: TextView            // Title of this card e.g. "Root Distribution"
    private lateinit var pieChart: PieChart                       // R.id.pieChart
    private lateinit var tvChartTotalOccurrences: TextView        // R.id.tvChartTotalOccurrences - Text like "Total Occurrences (Root): 150"
    private lateinit var tvChartMostLeastOccurrences: TextView    // R.id.tvChartMostLeastOccurrences

    // --- BottomSheet Dialogs & Adapters ---
    private var translationsBottomSheetDialog: BottomSheetDialog? = null
    private var morphFormsBottomSheetDialog: BottomSheetDialog? = null
    private var occurrencesBottomSheetDialog: BottomSheetDialog? = null // Renamed from bottomSheetDialog
    private lateinit var occurrencesAdapter: WordOccurrencesAdapter

    // --- Utilities ---
    private val bidiFormatter = BidiFormatter.getInstance()
    private var mediaPlayer: MediaPlayer? = null
    private var currentJuzForBottomSheet: Int = 0
    companion object {
        // Renamed for clarity, this is the identifier string (root/lemma/form)
        const val EXTRA_IDENTIFIER_VALUE = "IDENTIFIER_VALUE"
        // This is the S:A:W key if navigating from recitation
        const val EXTRA_WORD_KEY_FROM_RECITATION = "WORD_KEY_FROM_RECITATION"
        // This is the specific Arabic text of the word clicked/bookmarked, for pre-selection
        const val EXTRA_WORD_TEXT_FOR_PRESELECTION = "WORD_TEXT_FOR_PRESELECTION"
        private const val BASE_AUDIO_URL = "https://quran.seaade2024.com/data/quran-audio/"
        private const val AUDIO_PLAYBACK_TAG = "AudioPlayback"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_details)

        initializeViews()
        setupChartListeners()
        setupObservers()

        // Initialize adapter for bottom sheet early
        occurrencesAdapter = WordOccurrencesAdapter { occurrenceItem -> // occurrenceItem is WordOccurrenceDisplayItem
            navigateToVerse(occurrenceItem) // Pass the whole item
        }

        val identifierValue = intent.getStringExtra(EXTRA_IDENTIFIER_VALUE)
        val wordKey = intent.getStringExtra(EXTRA_WORD_KEY_FROM_RECITATION)
        val wordTextForPreselection = intent.getStringExtra(EXTRA_WORD_TEXT_FOR_PRESELECTION)
        Log.d("WordDetailsActivity", "onCreate - IdentifierValue: $identifierValue, WordKey: $wordKey, WordTextForPreselection: $wordTextForPreselection")

        if (identifierValue == null && wordKey == null && wordTextForPreselection == null) {
            Toast.makeText(this, "No identifier, word key, or word text specified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // Pass all to ViewModel, it will decide the priority
        viewModel.loadInitialData(identifierValue, wordKey, wordTextForPreselection)
    }

    private fun initializeViews() {
        // General & Top Bar
        progressBar = findViewById(R.id.progressBar)
        // Assuming the TextView for the page title in your topBar LinearLayout has android:id="@+id/tvActivityTitle"
        // If it's the default one from a Toolbar, you'd set it differently (e.g., supportActionBar?.title = "...")
        // For now, assuming a dedicated TextView in your R.id.topBar LinearLayout:
        val topBarLayout = findViewById<LinearLayout>(R.id.topBar)
        // You need to give the title TextView in topBar an ID. Let's assume it's R.id.tvPageTitleInTopBar
        // tvActivityTitle = topBarLayout.findViewById(R.id.tvPageTitleInTopBar) // Example, replace with actual ID
        // If it's the one directly in the topBar XML with text "Root Word Analysis"
        // you need to give THAT TextView an ID. For example: android:id="@+id/tv_page_title"
        tvActivityTitle = topBarLayout.findViewById(R.id.tvPageTitle) // Update R.id.tv_page_title to your actual ID

        bookmarkButton = findViewById(R.id.bookmarkButton)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { onBackPressed() }

        // Main Identifier Card
        tvIdentifierLabel = findViewById(R.id.tvRootLabel) // XML uses tvRootLabel
        tvIdentifierTotalOccurrences = findViewById(R.id.tvRootTotalOccurrences) // XML uses tvRootTotalOccurrences
        tvTitleContributingMorph = findViewById(R.id.tvContributingMorphFormsLabel) // XML uses this ID
        flexboxMorphForms = findViewById(R.id.flexboxMorphForms)
        btnShowAllMorphForms = findViewById(R.id.btnShowAllMorphForms)

        // Arabic Form Selection and Details Card
        // The title TextView within this card needs an ID in XML, e.g., android:id="@+id/tvTitleArabicFormsCard"
        tvTitleArabicForms = findViewById(R.id.tvTitleArabicFormsCard) // Update R.id.tvTitleArabicFormsCard to your actual ID
        spinnerArabicForms = findViewById(R.id.spinnerArabicForms)
        tvSelectedFormArabicText = findViewById(R.id.tvSelectedFormArabicText)
        btnPlaySelectedFormAudio = findViewById(R.id.btnPlaySelectedFormAudio)
        tvSelectedFormTranslation = findViewById(R.id.tvSelectedFormTranslation_main)
        ivMoreTranslations = findViewById(R.id.ivMoreTranslations)
        layoutTranslationSection = findViewById(R.id.layoutTranslationSection)
        tvSelectedFormTransliteration = findViewById(R.id.tvSelectedFormTransliteration)
        tvSelectedFormCharacters = findViewById(R.id.tvSelectedFormCharacters)

        // First Occurrence Details Card
        // The title TextView within this card needs an ID in XML, e.g., android:id="@+id/tvTitleFirstOccurrenceCard"
        tvTitleFirstOccurrence = findViewById(R.id.tvTitleFirstOccurrenceCard) // Update R.id.tvTitleFirstOccurrenceCard to your actual ID
        tvFirstOccSurahName = findViewById(R.id.tvFirstOccSurahName)
        tvFirstOccVerseText = findViewById(R.id.tvFirstOccVerseText)
        tvFirstOccAyahKey = findViewById(R.id.tvFirstOccAyahKey)
        tvFirstOccPageId = findViewById(R.id.tvFirstOccPageId)
        tvFirstOccJuzId = findViewById(R.id.tvFirstOccJuzId)
        tvFirstOccCharacters = findViewById(R.id.tvFirstOccCharacters)

        // Distribution Analysis Card
        // The title TextView within this card needs an ID in XML, e.g., android:id="@+id/tvTitleDistributionCard"
        tvTitleDistribution = findViewById(R.id.tvTitleDistributionAnalysisCard) // Update R.id.tvTitleDistributionCard to your actual ID
        pieChart = findViewById(R.id.pieChart)
        tvChartTotalOccurrences = findViewById(R.id.tvChartTotalOccurrences)
        tvChartMostLeastOccurrences = findViewById(R.id.tvChartMostLeastOccurrences)

        // Setup Listeners that are static
        bookmarkButton.setOnClickListener {
            viewModel.toggleSelectedFormBookmark()
        }
        // Setup Spinner for Arabic Forms
        spinnerArabicForms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Get the list of ArabicFormWithFullDetails from the ViewModel
                // that was used to populate the spinner adapter's display strings.
                viewModel.arabicForms.value?.getOrNull(position)?.let { selectedFormObject ->
                    // Now 'selectedFormObject' is the actual ArabicFormWithFullDetails object
                    viewModel.selectArabicForm(selectedFormObject)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(this, Observer { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        })

        viewModel.analysisEntry.observe(this, Observer { analysisEntry ->
            updateAnalysisEntryInfoUI(analysisEntry) // New method to handle dynamic labels
            updateFirstOccurrenceUI(analysisEntry) // Pass AnalysisEntryEntity
            if (analysisEntry == null) {
                bookmarkButton.setImageResource(R.drawable.ic_heart)
            }
        })

        // This observer populates the spinner adapter.
        viewModel.arabicForms.observe(this) { forms ->
            Log.d("ActivityObserver", "arabicForms emitted with ${forms.size} forms. Updating spinner adapter.")
            updateArabicFormsSpinner(forms)
            // After updating the adapter, try to set the selection based on the *current*
            // selectedArabicForm value, as it might have been set by loadInitialData.
            viewModel.selectedArabicForm.value?.let { currentSelection ->
                val position = forms.indexOfFirst { it.entryArabicFormEntity.arabicFormId == currentSelection.entryArabicFormEntity.arabicFormId }
                if (position != -1 && spinnerArabicForms.selectedItemPosition != position) {
                    spinnerArabicForms.setSelection(position, false)
                }
            }
        }

        // This observer reacts to changes in the selected form (either by user or initial load)
        // and updates the UI accordingly. It also attempts to set the spinner selection.
        viewModel.selectedArabicForm.observe(this) { selectedForm ->
            Log.d("ActivityObserver", "selectedArabicForm emitted: ${selectedForm?.entryArabicFormEntity?.arabicText}")
            updateSelectedArabicFormUI(selectedForm) // This updates the TextViews
            // When selected form changes, check its bookmark status
            viewModel.checkSelectedFormBookmarkStatus(selectedForm?.entryArabicFormEntity?.arabicText)

            // Ensure spinner reflects this selection if the adapter is ready
            if (selectedForm != null && spinnerArabicForms.adapter != null && spinnerArabicForms.adapter.count > 0) {
                viewModel.arabicForms.value?.let { currentFormsList -> // Get the list used by adapter
                    val position = currentFormsList.indexOfFirst { it.entryArabicFormEntity.arabicFormId == selectedForm.entryArabicFormEntity.arabicFormId }
                    if (position != -1 && spinnerArabicForms.selectedItemPosition != position) {
                        Log.d("ActivityObserver", "[selectedArabicForm obs] Updating spinner selection to: $position for ${selectedForm.entryArabicFormEntity.arabicText}")
                        spinnerArabicForms.setSelection(position, true) // true to trigger onItemSelected if it's a *new* programmatic change that should behave like a user click. Or false if you want to avoid it. `false` is safer to prevent loops if onItemSelected also modifies ViewModel.
                    } else if (position != -1) {
                        Log.d("ActivityObserver", "[selectedArabicForm obs] Spinner already at correct position: $position")
                    } else {
                        Log.w("ActivityObserver", "[selectedArabicForm obs] Selected form ${selectedForm.entryArabicFormEntity.arabicText} not found in spinner's current list.")
                    }
                }
            }
        }

        // Observe isBookmarked LiveData from ViewModel
        viewModel.isBookmarked.observe(this) { isBookmarked ->
            Log.d("WordDetailsActivity", "Bookmark status LiveData from VM: $isBookmarked")
            bookmarkButton.setImageResource(
                if (isBookmarked) R.drawable.ic_heart_bookmarked// Ensure you have this drawable
                else R.drawable.ic_heart // Ensure you have this drawable
            )
        }

        // Observe toast messages from ViewModel
        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastMessageShown() // Notify ViewModel that toast was shown
            }
        }

        viewModel.juzDistribution.observe(this, Observer { distribution ->
            updateJuzDistributionChart(distribution)
            // updateChartStatisticsText needs to be dynamic now based on identifierType
            updateChartStatisticsText(distribution, viewModel.analysisEntry.value?.identifierType ?: "Identifier")
        })

        viewModel.occurrencesInJuz.observe(this, Observer { occurrences ->
            showOccurrencesInJuzBottomSheet(occurrences)
        })

        viewModel.isLoadingMoreOccurrences.observe(this, Observer { isLoadingMore ->
            occurrencesAdapter.setLoading(isLoadingMore && viewModel.hasMoreOccurrences.value == true)
        })
        viewModel.hasMoreOccurrences.observe(this) { /* Handled by adapter setLoading */ }

        viewModel.contributingMorphForms.observe(this) { morphForms ->
            // ... (Flexbox population code as before) ...
            val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxMorphForms)
            val morphFormsLabel = findViewById<TextView>(R.id.tvContributingMorphFormsLabel)
            val btnShowAllMorphForms = findViewById<Button>(R.id.btnShowAllMorphForms) // Make sure this ID exists
            // Dynamically update the label for contributing morph forms
            val identifierType = viewModel.analysisEntry.value?.identifierType ?: "Entry"
            val identifierValue = viewModel.analysisEntry.value?.identifierValue ?: ""

            morphFormsLabel.text = when(identifierType) {
                "ROOT" -> "Associated Morphological Forms (Root Level)"
                "LEMMA" -> "Observed Morphological Variations (Lemma)"
                "FORM" -> "Morphological Form Details"
                else -> "Associated Forms"
            }

            flexboxLayout.removeAllViews()

            if (morphForms.isNotEmpty()) {
                morphFormsLabel.visibility = View.VISIBLE
                flexboxLayout.visibility = View.VISIBLE

                val maxInitialForms = 6 // Example
                val formsToShowInitially = if (morphForms.size > maxInitialForms) morphForms.take(maxInitialForms) else morphForms

                formsToShowInitially.forEach { formText ->
                    val chipView = LayoutInflater.from(this).inflate(R.layout.item_morph_form_chip, flexboxLayout, false) as TextView
                    chipView.text = formText
                    flexboxLayout.addView(chipView)
                }

                if (morphForms.size > maxInitialForms) {
                    btnShowAllMorphForms.visibility = View.VISIBLE
                    btnShowAllMorphForms.text = "View All ${morphForms.size} Forms" // Or just "View All"
                    btnShowAllMorphForms.setOnClickListener {
                        showAllMorphFormsBottomSheet(
                            "${viewModel.analysisEntry.value?.identifierType ?: "Entry"}: ${viewModel.analysisEntry.value?.identifierValue ?: ""}",
                            morphForms
                        )
                    }
                } else {
                    btnShowAllMorphForms.visibility = View.GONE
                }
            } else {
                morphFormsLabel.visibility = View.GONE
                flexboxLayout.visibility = View.GONE
                btnShowAllMorphForms.visibility = View.GONE
            }
        }
    }
    private fun showAllMorphFormsBottomSheet(identifierDisplayString: String, morphForms: List<String>) {
        if (morphFormsBottomSheetDialog == null) {
            morphFormsBottomSheetDialog = BottomSheetDialog(this)
            // Reuse generic list bottom sheet layout if you created one, or use translations one and adapt
            val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_list_generic, null)
            morphFormsBottomSheetDialog!!.setContentView(bottomSheetView)
        }

        val titleTextView = morphFormsBottomSheetDialog!!.findViewById<TextView>(R.id.tvBottomSheetGenericTitle)
        val recyclerView = morphFormsBottomSheetDialog!!.findViewById<RecyclerView>(R.id.rvGenericList)

        titleTextView?.text = "All Morphological Forms for Root: $identifierDisplayString"
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = MorphFormsAdapter(morphForms)

        morphFormsBottomSheetDialog!!.show()
    }
    // NEW: Method to update UI elements based on the AnalysisEntryEntity
    private fun updateAnalysisEntryInfoUI(analysisEntry: AnalysisEntryEntity?) {
        val typeLabel = when (analysisEntry?.identifierType) {
            "ROOT" -> "Root"
            "LEMMA" -> "Lemma"
            "FORM" -> "Form"
            else -> "Word" // Default label
        }
        // Update the main title of the page
        tvActivityTitle.text = "Analysis of $typeLabel " // CORRECTED LINE

        tvIdentifierLabel.text = "$typeLabel: ${analysisEntry?.identifierValue ?: "N/A"}"
        tvIdentifierTotalOccurrences.text = "Total $typeLabel Occurrences: ${analysisEntry?.totalOccurrences ?: 0}"

        // Update Card Titles Dynamically
        tvTitleArabicForms.text = "Arabic Forms of this $typeLabel"
        tvTitleFirstOccurrence.text = "First Occurrence of $typeLabel"
        tvTitleDistribution.text = "$typeLabel Distribution in Juz"
        // tvTitleContributingMorph is updated in its own observer block based on identifierType
        // Also update the label in the chart statistics
        updateChartStatisticsText(viewModel.juzDistribution.value ?: emptyMap(), typeLabel)
    }

    private fun updateArabicFormsSpinner(forms: List<EntryArabicFormWithFullDetails>) {
        if (forms.isEmpty()) {
            spinnerArabicForms.adapter = null
            Log.d("SpinnerUpdate", "Forms list is empty, clearing adapter.")
            return
        }
        val displayTexts = forms.map { it.entryArabicFormEntity.arabicText ?: "Unknown Form" }
        val adapter = ArrayAdapter(
            this,
            R.layout.custom_arabic_form_spinner_item,
            displayTexts
        )
        adapter.setDropDownViewResource(R.layout.custom_arabic_form_spinner_dropdown_item)
        spinnerArabicForms.adapter = adapter
        Log.d("SpinnerUpdate", "Spinner adapter populated with ${forms.size} items.")
        // The initial selection will be handled by the _selectedArabicForm observer
        // or by the _arabicForms observer's attempt after adapter is set.
    }

    private fun updateSelectedArabicFormUI(formWithDetails: EntryArabicFormWithFullDetails?) {
        if (formWithDetails == null) {
            tvSelectedFormArabicText.text = "" // Clear it
            // ... (rest of the clearing logic from your code)
            findViewById<TextView>(R.id.tvSelectedFormTransliteration).text = "Transliteration: N/A"
            findViewById<TextView>(R.id.tvSelectedFormCharacters).text = "Characters: N/A"
            return
        }

        val formEntity = formWithDetails.entryArabicFormEntity
        tvSelectedFormArabicText.text = formEntity.arabicText ?: "N/A" // Should be visible


        // Translation section (as implemented previously with BottomSheet)
        val mainTranslationTV = findViewById<TextView>(R.id.tvSelectedFormTranslation_main)
        val moreTranslationsIcon = findViewById<ImageView>(R.id.ivMoreTranslations)
        val translationSectionLayout = findViewById<LinearLayout>(R.id.layoutTranslationSection)
        val translations = formWithDetails.translations
        if (translations.isNotEmpty()) {
            mainTranslationTV.text = translations.first()
            if (translations.size > 1) {
                mainTranslationTV.append(" (+${translations.size - 1} more...)") // Append to first
                moreTranslationsIcon.visibility = View.VISIBLE
                translationSectionLayout.setOnClickListener {
                    showAllTranslationsBottomSheet(formWithDetails.entryArabicFormEntity.arabicText ?: "Word", translations)
                }
            } else {
                moreTranslationsIcon.visibility = View.GONE
                translationSectionLayout.setOnClickListener(null)
            }
        } else {
            mainTranslationTV.text = "N/A"
            moreTranslationsIcon.visibility = View.GONE
            translationSectionLayout.setOnClickListener(null)
        }


        val transliterationText = "Transliteration: ${formWithDetails.transliterations.joinToString(" / ")}"
        findViewById<TextView>(R.id.tvSelectedFormTransliteration).text = transliterationText
        Log.d("UpdateUI", "Transliteration set to: $transliterationText")

        val characters = viewModel.getCharactersForSelectedForm()
        val charactersText = "Characters: ${characters.joinToString(" ")}" // Join with space for Arabic
        findViewById<TextView>(R.id.tvSelectedFormCharacters).text = charactersText
        Log.d("UpdateUI", "Characters set to: $charactersText")


        if (formEntity.audioUrl != null) {
            val relativeAudioPath = formWithDetails.entryArabicFormEntity .audioUrl
            if (!relativeAudioPath.isNullOrEmpty()) {
                btnPlaySelectedFormAudio.visibility = View.VISIBLE
                btnPlaySelectedFormAudio.setOnClickListener {
                    val fullUrl = BASE_AUDIO_URL + relativeAudioPath
                    playAudioUrl(fullUrl)
                }
                // Optional: Change button text or icon
                // btnPlaySelectedFormAudio.text = "Play Form Audio"
            } else {
                btnPlaySelectedFormAudio.visibility = View.GONE
            }
        } else {
            btnPlaySelectedFormAudio.visibility = View.GONE
        }
    }
    private fun showAllTranslationsBottomSheet(arabicFormText: String, translations: List<String>) {
        if (translationsBottomSheetDialog == null) {
            translationsBottomSheetDialog = BottomSheetDialog(this)
            // Inflate custom layout
            val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_translations, null)
            translationsBottomSheetDialog!!.setContentView(bottomSheetView)
        }

        val titleTextView = translationsBottomSheetDialog!!.findViewById<TextView>(R.id.tvBottomSheetTranslationTitle)
        val recyclerView = translationsBottomSheetDialog!!.findViewById<RecyclerView>(R.id.rvTranslations)

        titleTextView?.text = "All Translations for $arabicFormText"
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = TranslationsAdapter(translations)

        translationsBottomSheetDialog!!.show()
    }
    private fun updateFirstOccurrenceUI(analysisEntryEntity: AnalysisEntryEntity?) {
        if (analysisEntryEntity == null) {
            tvFirstOccSurahName.text = "Surah: N/A"
            tvFirstOccVerseText.text = "Word in First Verse: N/A"
            tvFirstOccAyahKey.text = "Ayah Key: N/A"
            tvFirstOccPageId.text = "Page ID: N/A"
            tvFirstOccJuzId.text = "Juz: N/A"
            tvFirstOccCharacters.text = "Word Characters: N/A"
            return
        }

        val surahDetails = analysisEntryEntity.firstOccurrenceSurahId?.let {
            com.example.qurannexus.core.utils.QuranMetadata.getInstance().getSurahDetails(it)
        }

        tvFirstOccSurahName.text = "Surah: ${surahDetails?.arabicName ?: ""} (${surahDetails?.englishName ?: "N/A"})"

        val wrappedArabicText = bidiFormatter.unicodeWrap(
            analysisEntryEntity.firstOccurrenceArabicText ?: "N/A",
            TextDirectionHeuristicsCompat.ANYRTL_LTR, true
        )
        tvFirstOccVerseText.text = "Word in First Verse: $wrappedArabicText"

        tvFirstOccAyahKey.text = "Ayah Key: ${analysisEntryEntity.firstOccurrenceWordKey ?: "N/A"}"
        tvFirstOccPageId.text = "Page ID: ${analysisEntryEntity.firstOccurrencePageId ?: "N/A"}"
        tvFirstOccJuzId.text = "Juz: ${analysisEntryEntity.firstOccurrenceJuzId ?: "N/A"}"

        val firstOccChars = viewModel.getFirstOccurrenceCharacters()
        tvFirstOccCharacters.text = "Word Characters: ${firstOccChars.joinToString(" ")}" // Join with space

    }
    private fun playAudioUrl(url: String) {
        Log.d(AUDIO_PLAYBACK_TAG, "Attempting to play audio from URL: $url")
        releaseMediaPlayer() // Release any existing player first

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(url)
                setOnPreparedListener {
                    Log.d(AUDIO_PLAYBACK_TAG, "MediaPlayer prepared, starting playback.")
                    start()
                    // Optionally, disable play button while playing and re-enable on completion
                    // You can also update UI to show a "playing" state
                }
                setOnCompletionListener {
                    Log.d(AUDIO_PLAYBACK_TAG, "MediaPlayer playback completed.")
                    releaseMediaPlayer()
                    // Re-enable play button or reset UI state
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(AUDIO_PLAYBACK_TAG, "MediaPlayer Error: what: $what, extra: $extra for URL: $url")
                    Toast.makeText(this@WordDetailsActivity, "Error playing audio.", Toast.LENGTH_SHORT).show()
                    releaseMediaPlayer()
                    true // True if the error has been handled
                }
                prepareAsync() // Prepare asynchronously to avoid blocking UI thread
                Log.d(AUDIO_PLAYBACK_TAG, "MediaPlayer prepareAsync called.")
                // You could show a small loading indicator here until onPrepared is called
            } catch (e: IOException) {
                Log.e(AUDIO_PLAYBACK_TAG, "IOException setting data source for $url", e)
                Toast.makeText(this@WordDetailsActivity, "Failed to set up audio.", Toast.LENGTH_SHORT).show()
                releaseMediaPlayer()
            } catch (e: IllegalStateException) {
                Log.e(AUDIO_PLAYBACK_TAG, "IllegalStateException for $url", e)
                Toast.makeText(this@WordDetailsActivity, "Audio player state error.", Toast.LENGTH_SHORT).show()
                releaseMediaPlayer()
            }
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset() // Resets the MediaPlayer to its uninitialized state.
            it.release() // Releases resources associated with this MediaPlayer object.
            Log.d(AUDIO_PLAYBACK_TAG, "MediaPlayer released.")
        }
        mediaPlayer = null
    }

    private fun setupChartListeners() {
        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e is PieEntry) {
                    val juzStr = e.label?.replace("Juz ", "")?.trim()
                    try {
                        val juzNumber = juzStr?.toInt()
                        if (juzNumber != null) {
                            currentJuzForBottomSheet = juzNumber
                            // Load occurrences for this Juz, initial load
                            viewModel.loadOccurrencesForJuz(juzNumber, isInitialLoad = true)
                            // Open bottom sheet (it will be populated by observer)
                        }
                    } catch (ex: NumberFormatException) {
                        Log.e("PieChart", "Error parsing juz number from: $juzStr", ex)
                    }
                }
            }
            override fun onNothingSelected() {}
        })
        // Add BarChart listener if you re-add it
    }

    private fun updateJuzDistributionChart(juzDistribution: Map<String, Int>) {
        // juzDistribution is Map<JuzNumber (String), Count (Int)>
        if (juzDistribution.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            // barChart.clear() // if using
            return
        }
        setupPieChart(juzDistribution) // Reusing your existing pie chart setup
        // setupBarChart(juzDistribution) // If you re-add barchart
    }

    private fun updateChartStatisticsText(juzDistribution: Map<String, Int>, identifierTypeLabel : String) {
        val total = juzDistribution.values.sum()
        val maxEntry = juzDistribution.maxByOrNull { it.value }
        val minEntry = juzDistribution.filterValues { it > 0 }.minByOrNull { it.value }

        tvChartTotalOccurrences.text = "Total Occurrences ($identifierTypeLabel): $total" // Dynamic label

        val mostText = if (maxEntry != null) "Most: Juz ${maxEntry.key} (${maxEntry.value})" else "Most: N/A"
        val leastText = if (minEntry != null) "Least: Juz ${minEntry.key} (${minEntry.value})" else "Least: N/A"
        tvChartMostLeastOccurrences.text = "$mostText, $leastText"
    }

    // Re-using your PieChart setup method (ensure it's compatible with Map<String, Int>)
    private fun setupPieChart(juzDistribution: Map<String, Int>) {
        try {
            val total = juzDistribution.values.sum().toFloat()
            if (total == 0f) {
                pieChart.clear() // Clear if no data
                pieChart.setNoDataText("No distribution data available.")
                pieChart.invalidate()
                return
            }

            val entries = juzDistribution
                .filter { (_, count) -> count > 0 }
                .map { (juz, count) ->
                    // val percentage = (count.toFloat() / total) * 100f // Percentage is calculated by formatter
                    PieEntry(count.toFloat(), "Juz $juz", count.toFloat()) // Store raw count in data field for formatter
                }
                .sortedByDescending { it.value }

            val dataSet = PieDataSet(entries, "").apply {
                val colorList = mutableListOf<Int>()
                val baseColors = listOf(
                    Color.parseColor("#4CAF50"), // Green
                    Color.parseColor("#2196F3"), // Blue
                    Color.parseColor("#FFC107"), // Yellow
                    Color.parseColor("#F44336"), // Red
                    Color.parseColor("#9C27B0"), // Purple
                    Color.parseColor("#FF9800"), // Orange
                    Color.parseColor("#00BCD4"), // Cyan
                    Color.parseColor("#E91E63")  // Pink
                )
                // Dynamically assign colors if more entries than base colors
                entries.forEachIndexed { index, entry ->
                    val percentage = (entry.value / total) * 100f
                    colorList.add(
                        when {
                            percentage > 10f -> Color.parseColor("#4CAF50")
                            percentage > 5f -> Color.parseColor("#FFC107")
                            percentage > 2f -> Color.parseColor("#F44336")
                            else -> baseColors[index % baseColors.size] // Cycle through base colors
                        }
                    )
                }
                colors = colorList

                setValueTextColors(listOf(Color.BLACK))
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String { // value is the raw count here
                        val percentage = (value / total) * 100f
                        return if (percentage >= 1.5f) String.format("%.1f%%", percentage) else ""
                    }
                }
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1Length = 0.5f
                valueLinePart2Length = 0.2f
                valueLineWidth = 1f
                valueLineColor = Color.GRAY
                sliceSpace = 1f
            }

            pieChart.apply {
                setExtraOffsets(20f, 5f, 20f, 80f) // Adjust offsets
                data = PieData(dataSet)
                description.isEnabled = false
                isRotationEnabled = true
                isDrawHoleEnabled = true
                setHoleColor(Color.WHITE)
                holeRadius = 40f
                transparentCircleRadius = 45f
                setTransparentCircleColor(Color.WHITE)
                setTransparentCircleAlpha(110)
                centerText = "Juz\nDistribution"
                setCenterTextSize(12f)
                setUsePercentValues(false) // Important if formatter calculates percent from raw values
                legend.apply {
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false); yOffset = 10f; xEntrySpace = 7f; yEntrySpace = 5f
                    textSize = 10f; formSize = 10f; form = Legend.LegendForm.CIRCLE
                    textColor = Color.BLACK; isWordWrapEnabled = true

                    val customLegendEntries = mutableListOf<LegendEntry>()
                    if (entries.any { (it.value / total * 100f) > 10f })
                        customLegendEntries.add(LegendEntry("Frequent (>10%)", Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, Color.parseColor("#4CAF50")))
                    if (entries.any { val p = (it.value / total * 100f); p > 5f && p <= 10f })
                        customLegendEntries.add(LegendEntry("Common (5-10%)", Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, Color.parseColor("#FFC107")))
                    if (entries.any { val p = (it.value / total * 100f); p > 2f && p <= 5f})
                        customLegendEntries.add(LegendEntry("Occasional (2-5%)", Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, Color.parseColor("#F44336")))
                    if (entries.any { (it.value / total * 100f) <= 2f}) // A more generic "Other" or cycle through baseColors for legend too
                        customLegendEntries.add(LegendEntry("Rare (<2%)", Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, Color.parseColor("#9C27B0"))) // Example rare color


                    if (customLegendEntries.isNotEmpty()) setCustom(customLegendEntries)
                    else setCustom(arrayOf(LegendEntry("Distribution", Legend.LegendForm.NONE, Float.NaN, Float.NaN, null, Color.TRANSPARENT)))
                }
                minimumHeight = 500 // Adjust as needed
                animateY(800, Easing.EaseInOutQuad)
                invalidate()
            }

        } catch (e: Exception) {
            Log.e("PieChart", "Error setting up pie chart", e)
            pieChart.clear()
            pieChart.setNoDataText("Error displaying chart.")
            pieChart.invalidate()
        }
    }

    private fun showOccurrencesInJuzBottomSheet(occurrences: List<WordOccurrenceDisplayItem>) {
        if (isFinishing) return
        // Only show/refresh if there are occurrences or if it's meant to clear an existing list
        if (occurrences.isEmpty() && (occurrencesBottomSheetDialog == null || occurrencesBottomSheetDialog?.isShowing != true)) {
            // If list is empty and dialog not showing, do nothing
            // If dialog is showing and list becomes empty, adapter will handle empty state
            if (occurrencesBottomSheetDialog?.isShowing == true) occurrencesAdapter.submitList(emptyList())
            return
        }


        if (occurrencesBottomSheetDialog == null) {
            occurrencesBottomSheetDialog = BottomSheetDialog(this).apply {
                setContentView(R.layout.layout_word_occurrences_bottom_sheet)
                findViewById<RecyclerView>(R.id.occurrencesRecyclerView)?.apply {
                    layoutManager = LinearLayoutManager(this@WordDetailsActivity)
                    adapter = occurrencesAdapter // Already initialized
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                            val visibleItemCount = layoutManager.childCount
                            val totalItemCount = layoutManager.itemCount // Use adapter's item count
                            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                            if (viewModel.isLoadingMoreOccurrences.value == false &&
                                viewModel.hasMoreOccurrences.value == true &&
                                (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                                firstVisibleItemPosition >= 0 && totalItemCount > 0) { // Check totalItemCount > 0
                                viewModel.loadMoreOccurrencesForCurrentJuz()
                            }
                        }
                    })
                }
            }
        }
        occurrencesBottomSheetDialog?.findViewById<TextView>(R.id.titleText)?.text =
            "Occurrences in Juz $currentJuzForBottomSheet"

        occurrencesAdapter.submitList(occurrences)

        if (!occurrencesBottomSheetDialog!!.isShowing && occurrences.isNotEmpty()) {
            occurrencesBottomSheetDialog?.show()
        }
    }

    private fun navigateToVerse(occurrenceItem: WordOccurrenceDisplayItem) {
        occurrencesBottomSheetDialog?.dismiss()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isByPage = sharedPreferences.getBoolean("recitation_layout_by_page", false)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("NAVIGATE_TO_RECITATION", true)
            putExtra("CHAPTER_ID", occurrenceItem.chapterIdString)
            putExtra("VERSE_NUMBER", occurrenceItem.verseNumberString)
            putExtra("IS_BY_PAGE", isByPage)

            if (isByPage) {
                val targetPage = occurrenceItem.pageId
                if (targetPage != null) {
                    putExtra("TARGET_PAGE_NUMBER", targetPage as Int) // Explicit cast
                    putExtra("SCROLL_TO_VERSE_ON_PAGE", occurrenceItem.ayahIndex) // ayahIndex is Int
                    putExtra("HIGHLIGHT_CHAPTER_ID", occurrenceItem.chapterIdString)
                    Log.d("WordDetailsActivity", "Navigating to Page: $targetPage, Verse: ${occurrenceItem.ayahIndex}")
                } else {
                    Log.e("WordDetailsActivity", "PageId is null for page-based navigation.")
                    Toast.makeText(this@WordDetailsActivity, "Could not determine page for this verse.", Toast.LENGTH_SHORT).show()
                    return@apply // Exit .apply block
                }
            } else {
                // Verse-by-verse navigation (CURRENT_SURAH_INDEX should be 0-based)
                val surahIndex = occurrenceItem.chapterIdString.toIntOrNull()?.minus(1)
                if (surahIndex != null) {
                    putExtra("CURRENT_SURAH_INDEX", surahIndex as Int) // Explicit cast
                } else {
                    putExtra("CURRENT_SURAH_INDEX", 0) // Default or handle error
                    Log.e("WordDetailsActivity", "Could not parse chapterIdString to Int for surahIndex")
                }
                putExtra("SCROLL_TO_VERSE", occurrenceItem.ayahIndex) // ayahIndex is Int
                Log.d("WordDetailsActivity", "Navigating to Surah Index: $surahIndex, Verse: ${occurrenceItem.ayahIndex}")
            }
        }
        // Check if intent setup was aborted (e.g. pageId was null)
        if (intent.extras?.containsKey("TARGET_PAGE_NUMBER") == false && isByPage && occurrenceItem.pageId == null) {
            // Don't start activity if essential extras for page navigation are missing
            return
        }
        startActivity(intent)
        finish()
    }
    override fun onStop() {
        super.onStop()
        // Release media player when activity is stopped to free resources
        // especially if audio might continue playing when app goes to background.
        // If you want audio to stop when activity is merely paused (e.g. dialog comes up), use onPause.
        // For short clips, stopping on onStop is usually fine.
        releaseMediaPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final cleanup, though onStop should have handled it.
        releaseMediaPlayer()
    }
}
class CustomMarkerView(
    context: Context,
    layoutResource: Int,
    private val occurrences: List<Int>
) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is BarEntry) {
            val index = e.x.toInt() - 1 // Adjust for 0-based indexing
            tvContent.text = "Juz ${index + 1}: ${occurrences[index]} Occurrences"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}

class CustomMarkerViewPie(
    context: Context,
    layoutResource: Int,
    private val occurrences: List<Int>
) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is PieEntry) {
            val index = e.label.toInt() - 1 // Convert label back to index
            tvContent.text = "Juz ${index + 1}: ${occurrences[index]} Occurrences"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}




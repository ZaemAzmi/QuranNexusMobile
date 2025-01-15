package com.example.qurannexus.features.words.services

import android.content.Context
import android.util.Log
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.words.models.WordDetails
import com.example.qurannexus.features.words.models.WordOccurrence
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jakarta.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class WordJsonService @Inject constructor(
    private val context: Context
) {
    private var uniqueWords: Map<String, WordDetails>? = null
    private var wordOccurrences: Map<String, Map<String, List<WordOccurrence>>>? = null

    private fun loadJsonFromAssets(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun getWordDetails(wordId: String): WordDetails? {
        if (uniqueWords == null) {
            val jsonString = loadJsonFromAssets("unique_words.json")
            val type = object : TypeToken<Map<String, Map<String, WordDetails>>>() {}.type
            val response = Gson().fromJson<Map<String, Map<String, WordDetails>>>(jsonString, type)
            uniqueWords = response["words"]
        }

        // Format the wordId to match JSON structure
        val formattedWordId = if (!wordId.startsWith("word_")) "word_$wordId" else wordId

        return uniqueWords?.get(formattedWordId)?.let { word ->
            word.copy(
                first_occurrence = word.first_occurrence.let { occurrence ->
                    occurrence.copy(
                        surah_name = QuranMetadata.getInstance().getSurahDetails(occurrence.chapter_id.toInt())?.englishName ?: ""
                    )
                }
            )
        }
    }

    fun getWordOccurrencesInJuz(wordId: String, juzNumber: Int): List<WordOccurrence> {
        if (wordOccurrences == null) {
            val jsonString = loadJsonFromAssets("word_occurrences.json")
            val type = object : TypeToken<Map<String, Map<String, Map<Int, List<WordOccurrence>>>>>() {}.type
            val response = Gson().fromJson<Map<String, Map<String, Map<String, List<WordOccurrence>>>>>(jsonString, type)
            wordOccurrences = response["occurrences"]
        }

        // Format the wordId to match JSON structure
        val formattedWordId = if (!wordId.startsWith("word_")) "word_$wordId" else wordId

        return wordOccurrences?.get(formattedWordId)?.get(juzNumber.toString())?.map { occurrence ->
            occurrence.copy(
                surah_name = QuranMetadata.getInstance().getSurahDetails(occurrence.chapter_id.toInt())?.englishName ?: ""
            )
        } ?: emptyList()
    }


    fun getDailyWord(userId: String): WordDetails? {
        if (uniqueWords == null) {
            getWordDetails("") // Load JSON
        }

        uniqueWords?.let { words ->
            // Get current date in format YYYYMMDD
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)

            // Create a deterministic seed using userId and date
            val seed = (userId + today).hashCode()

            // Get deterministic index using the seed
            val wordsList = words.values.toList()
            val index = abs(seed) % wordsList.size

            return wordsList[index]
        }
        return null
    }

}
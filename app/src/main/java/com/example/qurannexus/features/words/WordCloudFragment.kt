package com.example.qurannexus.features.words

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import android.content.Intent
import com.example.qurannexus.features.words.views.WordCloudView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WordCloudFragment : Fragment() {

    private lateinit var wordCloudView: WordCloudView
    private lateinit var quranApi: QuranApi
    private var authToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_cloud, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wordCloudView = view.findViewById(R.id.wordCloudView)

        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        // Set up word click listener
        wordCloudView.onWordClickListener = { word ->
            val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
                // Basic word info
                putExtra("WORD_TEXT", word.word_text)
                putExtra("TRANSLATION", word.translation)
                putExtra("TRANSLITERATION", word.transliteration)
                putExtra("TOTAL_OCCURRENCES", word.total_occurrences)

                // First occurrence details
                putExtra("CHAPTER_ID", word.first_occurrence.chapter_id)
                putExtra("VERSE_NUMBER", word.first_occurrence.verse_number)
                putExtra("SURAH_NAME", word.first_occurrence.surah_name)
                putExtra("PAGE_ID", word.first_occurrence.page_id)
                putExtra("JUZ_NUMBER", word.first_occurrence.juz_id)
                putExtra("VERSE_TEXT", word.first_occurrence.verse_text)
                putExtra("AUDIO_URL", word.first_occurrence.audio_url)
            }
            startActivity(intent)
        }

        fetchBookmarkedWords()


    }

    private fun fetchBookmarkedWords() {
        if (authToken == null) {
            Toast.makeText(context, "Please login to view bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        quranApi.getBookmarks("Bearer $authToken").enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(
                call: Call<BookmarksResponse>,
                response: Response<BookmarksResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val bookmarksResponse = response.body()!!
                    if (bookmarksResponse.status == "success") {
                        val words = bookmarksResponse.bookmarks.words
                        if (words.isEmpty()) {
                            Toast.makeText(context, "No bookmarked words found", Toast.LENGTH_SHORT).show()
                        } else {
                            wordCloudView.setWords(words)
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
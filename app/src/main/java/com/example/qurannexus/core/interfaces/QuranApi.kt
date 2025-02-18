package com.example.qurannexus.core.interfaces

import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.example.qurannexus.features.home.achievement.AchievementStatusResponse
import com.example.qurannexus.features.home.achievement.BaseResponse
import com.example.qurannexus.features.home.achievement.StreakResponse
import com.example.qurannexus.features.home.achievement.UnlockAchievementRequest
import com.example.qurannexus.features.home.models.WordDetailsResponse
import com.example.qurannexus.features.recitation.audio.models.AudioRecitationResponse
import com.example.qurannexus.features.recitation.models.AyahRecitationModel
import com.example.qurannexus.features.recitation.models.PageVerseResponse
import com.example.qurannexus.features.recitation.models.SurahListResponse
import com.example.qurannexus.features.words.models.DailyQuoteResponse
import com.example.qurannexus.features.words.models.DailyWordResponse
import com.example.qurannexus.features.words.models.WordsChaptersDistributionResponse
import com.example.qurannexus.features.words.models.WordDistributionResponse
import com.example.qurannexus.features.words.models.WordOccurrenceResponse
import com.example.qurannexus.features.words.models.WordSearchResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface QuranApi {
    @GET("surahs")
    fun getAllSurahs(): Call<SurahListResponse?>?
    @GET("chapters/{surahId}/verses")
    fun getVersesBySurah(@Path("surahId") surahId: Int?): Call<AyahRecitationModel?>?

    @GET("pages/{page_id}")
    fun getPageVerses(
        @Path("page_id") pageId: Int,
        @Query("ayahs") ayahs: Boolean = true,
        @Query("words") words: Boolean = true
    ): Call<PageVerseResponse?>?

    //words
    @GET("words/{word_key}")
    fun getWordDetails(@Path("word_key") wordKey: String?): Call<WordDetailsResponse?>?

    @GET("quotes/daily")
    fun getDailyQuote(): Call<DailyQuoteResponse>

    // bookmarks
    @POST("bookmarks")
    fun addBookmark(
    @Header("Authorization") token: String,
    @Body request: BookmarkRequest
    ): Call<BookmarkResponse>

    @DELETE("bookmarks/{type}/{itemId}")
    fun removeBookmark(
        @Header("Authorization") token: String,
        @Path("type") type: String,
        @Path("itemId") itemId: String
    ): Call<RemoveBookmarkResponse>
    @GET("bookmarks")
    fun getBookmarks(
        @Header("Authorization") token: String
    ): Call<BookmarksResponse>
    @GET("audio_recitations/{ayah_key}")
    fun getAudioRecitation(@Path("ayah_key") ayahKey: String): Call<AudioRecitationResponse>

    // For searching words (used in search feature)
    @GET("words/search")
    fun searchWords(
        @Query("q") query: String? = null,
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Call<WordOccurrenceResponse>

    // For getting word occurrences (used in WordDetailsActivity)
    @GET("words/search")
    fun getWordOccurrences(
        @Query("word_text") wordText: String,
        @Query("juz") juzNumber: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Call<WordOccurrenceResponse>

    @GET("words/distribution")
    fun getWordDistribution(
        @Query("word_text") wordText: String
    ): Call<WordDistributionResponse>
    @GET("words/chapters-distribution")
    fun getWordsChaptersDistribution(
        @Query("words[]") words: List<String>
    ): Call<WordsChaptersDistributionResponse>
    @GET("achievements/status")
    fun getAchievementStatus(
        @Header("Authorization") token: String
    ): Call<AchievementStatusResponse>

    @POST("achievements/unlock")
    fun unlockAchievement(
        @Header("Authorization") token: String,
        @Body request: UnlockAchievementRequest
    ): Call<BaseResponse>

    @GET("achievements/check-streak")
    fun checkStreakAchievement(
        @Header("Authorization") token: String
    ): Call<StreakResponse>
}
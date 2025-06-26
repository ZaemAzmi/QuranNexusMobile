package com.example.qurannexus.features.recitation.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecitationDao {
    @Query("SELECT * FROM quran_ayah_details WHERE page_id = :pageId ORDER BY ayah_index ASC")
    fun getAyahsForPage(pageId: Int): Flow<List<QuranAyahDetailEntity>>

    // We'll use this later for the SurahListFragment refactor
    @Query("SELECT surah_id, MIN(ayah_text_uthmani) as arabic_name, MIN(page_id) as starting_page, COUNT(DISTINCT ayah_index) as num_ayahs FROM quran_ayah_details GROUP BY surah_id ORDER BY surah_id ASC")
    fun getAllSurahsInfo(): Flow<List<SurahInfo>>
}

// A simple data class to hold the result of the surah list query
data class SurahInfo(
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "arabic_name") val arabicName: String,
    @ColumnInfo(name = "starting_page") val startingPage: Int,
    @ColumnInfo(name = "num_ayahs") val numAyahs: Int
)
package com.example.qurannexus.core.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.qurannexus.core.database.converters.ListConverter
import com.example.qurannexus.core.database.entities.*
import com.example.qurannexus.features.analysis.data.WordAnalysisDao // Updated DAO name
import com.example.qurannexus.features.recitation.data.RecitationDao


@Database(
    entities = [
        AnalysisEntryEntity::class,
        EntryArabicFormEntity::class, // Renamed
        ArabicFormTransliterationEntity::class, // Stays same
        ArabicFormTranslationEntity::class,     // Stays same
        EntryContributingMorphFormEntity::class, // Renamed
        EntrySurahDistributionEntity::class,    // Renamed
        EntryJuzDistributionEntity::class,      // Renamed
        EntryPageDistributionEntity::class,     // Renamed
        AllWordOccurrenceEntity::class,   // Renamed
        QuranAyahDetailEntity::class // <<<< ADD THIS NEW ENTITY
    ],
    version = 7, // <<<< IMPORTANT: Increment version number!
    exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class QuranNexusDatabase : RoomDatabase() {

    abstract fun wordAnalysisDao(): WordAnalysisDao
    abstract fun recitationDao(): RecitationDao
    companion object {
        @Volatile
        private var INSTANCE: QuranNexusDatabase? = null
        // NEW Database Filename
        private const val DATABASE_NAME = "quran_analysis_unified.db"

        fun getDatabase(context: Context): QuranNexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranNexusDatabase::class.java,
                    DATABASE_NAME
                )
                    .createFromAsset("database/$DATABASE_NAME") // Ensure this path is correct
                    // Destructive migration for now during development.
                    // For production, you'd implement proper Migration objects.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.example.qurannexus.core.database.entities


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.qurannexus.core.database.converters.ListConverter

// --- Main Root Table ---
@Entity(tableName = "roots")
@TypeConverters(ListConverter::class) // For first_occurrence_characters
data class RootEntity(
    @PrimaryKey
    @ColumnInfo(name = "root_label")
    val rootLabel: String,

    @ColumnInfo(name = "total_occurrences")
    val totalOccurrences: Int?,

    @ColumnInfo(name = "total_number_of_unique_arabic_forms")
    val totalNumberOfUniqueArabicForms: Int?,

    // First Occurrence Details
    @ColumnInfo(name = "first_occurrence_word_key")
    val firstOccurrenceWordKey: String?,
    @ColumnInfo(name = "first_occurrence_surah_id")
    val firstOccurrenceSurahId: Int?,
    @ColumnInfo(name = "first_occurrence_ayah_index")
    val firstOccurrenceAyahIndex: Int?,
    @ColumnInfo(name = "first_occurrence_word_index_in_ayah")
    val firstOccurrenceWordIndexInAyah: Int?,
    @ColumnInfo(name = "first_occurrence_arabic_text")
    val firstOccurrenceArabicText: String?,
    @ColumnInfo(name = "first_occurrence_transliteration")
    val firstOccurrenceTransliteration: String?,
    @ColumnInfo(name = "first_occurrence_translation")
    val firstOccurrenceTranslation: String?,
    @ColumnInfo(name = "first_occurrence_characters") // Stored as JSON String
    val firstOccurrenceCharactersJson: String?,
    @ColumnInfo(name = "first_occurrence_audio_url")
    val firstOccurrenceAudioUrl: String?,
    @ColumnInfo(name = "first_occurrence_page_id")
    val firstOccurrencePageId: Int?,
    @ColumnInfo(name = "first_occurrence_juz_id")
    val firstOccurrenceJuzId: Int?,
    @ColumnInfo(name = "first_occurrence_line_number")
    val firstOccurrenceLineNumber: Int?,
    @ColumnInfo(name = "page_positions_json") // Optional detailed page/line breakdown
    val pagePositionsJson: String?
)

// --- Arabic Forms Table ---
@Entity(
    tableName = "root_arabic_forms",
    foreignKeys = [ForeignKey(
        entity = RootEntity::class,
        parentColumns = ["root_label"],
        childColumns = ["root_label"],
        onDelete = ForeignKey.CASCADE
    )],
//    indices = [Index(value = ["root_label"])]
)
@TypeConverters(ListConverter::class) // For charactersJson
data class ArabicFormEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "arabic_form_id")
    val arabicFormId: Long = 0,

    @ColumnInfo(name = "root_label") // Index for faster lookups
    val rootLabel: String,

    @ColumnInfo(name = "arabic_text")
    val arabicText: String?,

    @ColumnInfo(name = "characters") // Stored as JSON String
    val charactersJson: String?,

    @ColumnInfo(name = "audio_url")
    val audioUrl: String?,

    @ColumnInfo(name = "occurrences_of_this_specific_arabic_form")
    val occurrencesOfThisSpecificArabicForm: Int?
)

// --- Transliterations for Arabic Forms ---
@Entity(
    tableName = "arabic_form_transliterations",
    foreignKeys = [ForeignKey(
        entity = ArabicFormEntity::class,
        parentColumns = ["arabic_form_id"],
        childColumns = ["arabic_form_id"],
        onDelete = ForeignKey.CASCADE
    )],
//    indices = [Index(value = ["arabic_form_id"])]
)
data class ArabicFormTransliterationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transliteration_id")
    val transliterationId: Long = 0,

    @ColumnInfo(name = "arabic_form_id")
    val arabicFormId: Long,

    val transliteration: String?
)

// --- Translations for Arabic Forms ---
@Entity(
    tableName = "arabic_form_translations",
    foreignKeys = [ForeignKey(
        entity = ArabicFormEntity::class,
        parentColumns = ["arabic_form_id"],
        childColumns = ["arabic_form_id"],
        onDelete = ForeignKey.CASCADE
    )],
//    indices = [Index(value = ["arabic_form_id"])]
)
data class ArabicFormTranslationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "translation_id")
    val translationId: Long = 0,

    @ColumnInfo(name = "arabic_form_id")
    val arabicFormId: Long,

    val translation: String?
)

// --- Contributing Morphological Forms for Roots ---
@Entity(
    tableName = "root_contributing_morph_forms",
    // Using autoGenerate = true for id simplifies Room's handling of composite keys
    // while still enforcing uniqueness with the UNIQUE constraint in the DB.
    // Room can work with @PrimaryKey(["root_label", "morph_form"]), but explicit ID is often easier.
//    primaryKeys = ["id"], // Explicit PK for Room's preference
    foreignKeys = [ForeignKey(
        entity = RootEntity::class,
        parentColumns = ["root_label"],
        childColumns = ["root_label"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["root_label", "morph_form"], unique = true), Index(value = ["root_label"])]
)
data class RootContributingMorphFormEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "root_label") val rootLabel: String,
    @ColumnInfo(name = "morph_form") val morphForm: String
)


// --- Distribution Tables (Summary) ---
@Entity(
    tableName = "root_occurrences_by_surah",
//    primaryKeys = ["id"],
    foreignKeys = [ForeignKey(
        entity = RootEntity::class,
        parentColumns = ["root_label"],
        childColumns = ["root_label"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["root_label", "surah_id"], unique = true), Index(value =["root_label"])]
)
data class RootSurahDistributionEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "root_label") val rootLabel: String,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    val count: Int?
)

@Entity(
    tableName = "root_occurrences_by_juz",
//    primaryKeys = ["id"],
    foreignKeys = [ForeignKey(
        entity = RootEntity::class,
        parentColumns = ["root_label"],
        childColumns = ["root_label"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["root_label", "juz_id"], unique = true), Index(value =["root_label"])]
)
data class RootJuzDistributionEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "root_label") val rootLabel: String,
    @ColumnInfo(name = "juz_id") val juzId: Int,
    val count: Int?
)

@Entity(
    tableName = "root_occurrences_by_page",
//    primaryKeys = ["id"],
    foreignKeys = [ForeignKey(
        entity = RootEntity::class,
        parentColumns = ["root_label"],
        childColumns = ["root_label"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["root_label", "page_id"], unique = true), Index(value =["root_label"])]
)
data class RootPageDistributionEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "root_label") val rootLabel: String,
    @ColumnInfo(name = "page_id") val pageId: Int,
    val count: Int?
)


// --- Detailed Word Occurrences Table ---
@Entity(
    tableName = "root_word_occurrences",
//    primaryKeys = ["occurrence_id"], // Explicit PK
    foreignKeys = [ForeignKey(
        entity = RootEntity::class,
        parentColumns = ["root_label"],
        childColumns = ["root_label"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["root_label", "word_key"], unique = true), // Original unique constraint
//        Index(value = ["root_label"]), // For FK
        Index(value = ["juz_id"]),    // For filtering by Juz
        Index(value = ["surah_id"])   // For filtering by Surah
    ]
)
@TypeConverters(ListConverter::class) // For charactersJson
data class RootWordOccurrenceEntity(
    @ColumnInfo(name = "occurrence_id") @PrimaryKey(autoGenerate = true)
    val occurrenceId: Long = 0,

    @ColumnInfo(name = "root_label")
    val rootLabel: String,
    @ColumnInfo(name = "word_key") // "S:A:W"
    val wordKey: String,

    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_index")
    val ayahIndex: Int,
    @ColumnInfo(name = "word_index_in_ayah")
    val wordIndexInAyah: Int,

    @ColumnInfo(name = "arabic_text")
    val arabicText: String?, // Specific Arabic text of this occurrence
    @ColumnInfo(name = "transliteration")
    val transliteration: String?,
    @ColumnInfo(name = "translation")
    val translation: String?,
    @ColumnInfo(name = "characters") // JSON string of characters list
    val charactersJson: String?,
    @ColumnInfo(name = "audio_url")
    val audioUrl: String?,

    @ColumnInfo(name = "page_id")
    val pageId: Int?,
    @ColumnInfo(name = "juz_id")
    val juzId: Int?,
    @ColumnInfo(name = "line_number")
    val lineNumber: Int?
)

// --- Helper Data Classes for Queries (Not Entities) ---
data class ArabicFormWithFullDetails(
    val arabicFormEntity: ArabicFormEntity,
    val transliterations: List<String>,
    val translations: List<String>
)

data class RootFullDetails(
    val rootEntity: RootEntity,
    val arabicForms: List<ArabicFormWithFullDetails>,
    val contributingMorphForms: List<String>,
    val juzDistribution: List<RootJuzDistributionEntity>
    // Add other distributions if needed directly, or derive from occurrences
)
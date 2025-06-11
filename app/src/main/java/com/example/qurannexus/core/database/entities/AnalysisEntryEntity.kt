package com.example.qurannexus.core.database.entities


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.qurannexus.core.database.converters.ListConverter
// --- Main Analysis Entry Table ---
@Entity(
    tableName = "analysis_entries", // Changed from "roots"
)
@TypeConverters(ListConverter::class)
data class AnalysisEntryEntity(
    @PrimaryKey // No autoGenerate if it's the string identifier
    @ColumnInfo(name = "identifier_value") // Changed from "root_label"
    val identifierValue: String,

    @ColumnInfo(name = "identifier_type") // NEW field
    val identifierType: String, // "ROOT", "LEMMA", "FORM"

    @ColumnInfo(name = "total_occurrences")
    val totalOccurrences: Int?,

    @ColumnInfo(name = "total_number_of_unique_arabic_forms")
    val totalNumberOfUniqueArabicForms: Int?,

    // First Occurrence Details (field names remain same as in DB)
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
    val firstOccurrenceCharactersJson: String?, // Keep _json suffix if converter expects it
    @ColumnInfo(name = "first_occurrence_audio_url")
    val firstOccurrenceAudioUrl: String?,
    @ColumnInfo(name = "first_occurrence_page_id")
    val firstOccurrencePageId: Int?,
    @ColumnInfo(name = "first_occurrence_juz_id")
    val firstOccurrenceJuzId: Int?,
    @ColumnInfo(name = "first_occurrence_line_number")
    val firstOccurrenceLineNumber: Int?,
    @ColumnInfo(name = "page_positions_json")
    val pagePositionsJson: String?
)

// --- Arabic Forms (derived from an AnalysisEntry) ---
@Entity(
    tableName = "entry_arabic_forms", // Changed
    foreignKeys = [ForeignKey(
        entity = AnalysisEntryEntity::class,
        parentColumns = ["identifier_value"], // Changed
        childColumns = ["parent_identifier_value"], // Changed
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["parent_identifier_value"])] // Index on FK
)
@TypeConverters(ListConverter::class)
data class EntryArabicFormEntity( // Renamed
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "arabic_form_id")
    val arabicFormId: Long = 0,

    @ColumnInfo(name = "parent_identifier_value") // Changed from "root_label"
    val parentIdentifierValue: String,

    @ColumnInfo(name = "arabic_text")
    val arabicText: String?,

    @ColumnInfo(name = "characters")
    val charactersJson: String?, // Keep _json suffix

    @ColumnInfo(name = "audio_url")
    val audioUrl: String?,

    @ColumnInfo(name = "occurrences_of_this_specific_arabic_form")
    val occurrencesOfThisSpecificArabicForm: Int?
)

// --- Transliterations for Arabic Forms (No change in structure) ---
@Entity(
    tableName = "arabic_form_transliterations",
    foreignKeys = [ForeignKey(
        entity = EntryArabicFormEntity::class, // Parent entity name changed
        parentColumns = ["arabic_form_id"],
        childColumns = ["arabic_form_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["arabic_form_id"])]
)
data class ArabicFormTransliterationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transliteration_id")
    val transliterationId: Long = 0,

    @ColumnInfo(name = "arabic_form_id")
    val arabicFormId: Long,

    val transliteration: String?
)

// --- Translations for Arabic Forms (No change in structure) ---
@Entity(
    tableName = "arabic_form_translations",
    foreignKeys = [ForeignKey(
        entity = EntryArabicFormEntity::class, // Parent entity name changed
        parentColumns = ["arabic_form_id"],
        childColumns = ["arabic_form_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["arabic_form_id"])]
)
data class ArabicFormTranslationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "translation_id")
    val translationId: Long = 0,

    @ColumnInfo(name = "arabic_form_id")
    val arabicFormId: Long,

    val translation: String?
)

// --- Contributing Morphological Forms for Analysis Entries ---
@Entity(
    tableName = "entry_contributing_morph_forms", // Changed
    foreignKeys = [ForeignKey(
        entity = AnalysisEntryEntity::class,
        parentColumns = ["identifier_value"], // Changed
        childColumns = ["parent_identifier_value"], // Changed
        onDelete = ForeignKey.CASCADE
    )],
    // Composite unique constraint defined in DB, Room uses PK for identity
    indices = [Index(value = ["parent_identifier_value", "morph_form"], unique = true), Index(value = ["parent_identifier_value"])]
)
data class EntryContributingMorphFormEntity( // Renamed
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "parent_identifier_value") // Changed from "root_label"
    val parentIdentifierValue: String,
    @ColumnInfo(name = "morph_form") val morphForm: String
)


// --- Distribution Tables (Summary) ---
@Entity(
    tableName = "entry_occurrences_by_surah", // Changed
    foreignKeys = [ForeignKey(
        entity = AnalysisEntryEntity::class,
        parentColumns = ["identifier_value"], // Changed
        childColumns = ["parent_identifier_value"], // Changed
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["parent_identifier_value", "surah_id"], unique = true), Index(value =["parent_identifier_value"])]
)
data class EntrySurahDistributionEntity( // Renamed
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "parent_identifier_value") // Changed from "root_label"
    val parentIdentifierValue: String,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    val count: Int?
)

@Entity(
    tableName = "entry_occurrences_by_juz", // Changed
    foreignKeys = [ForeignKey(
        entity = AnalysisEntryEntity::class,
        parentColumns = ["identifier_value"], // Changed
        childColumns = ["parent_identifier_value"], // Changed
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["parent_identifier_value", "juz_id"], unique = true), Index(value =["parent_identifier_value"])]
)
data class EntryJuzDistributionEntity( // Renamed
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "parent_identifier_value") // Changed from "root_label"
    val parentIdentifierValue: String,
    @ColumnInfo(name = "juz_id") val juzId: Int,
    val count: Int?
)

@Entity(
    tableName = "entry_occurrences_by_page", // Changed
    foreignKeys = [ForeignKey(
        entity = AnalysisEntryEntity::class,
        parentColumns = ["identifier_value"], // Changed
        childColumns = ["parent_identifier_value"], // Changed
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["parent_identifier_value", "page_id"], unique = true), Index(value =["parent_identifier_value"])]
)
data class EntryPageDistributionEntity( // Renamed
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "parent_identifier_value") // Changed from "root_label"
    val parentIdentifierValue: String,
    @ColumnInfo(name = "page_id") val pageId: Int,
    val count: Int?
)


// --- All Individual Word Occurrences Table ---
@Entity(
    tableName = "all_word_occurrences", // Changed
    foreignKeys = [ForeignKey(
        entity = AnalysisEntryEntity::class,
        parentColumns = ["identifier_value"], // Changed
        childColumns = ["mapped_identifier_value"], // New FK column name
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["word_key"], unique = true), // word_key is S:A:W and must be unique
        Index(value = ["mapped_identifier_value"]), // For FK relationship
        Index(value = ["juz_id"]),
        Index(value = ["surah_id"]),
        Index(value = ["arabic_text"]) // Index for looking up by Arabic text
    ]
)
@TypeConverters(ListConverter::class)
data class AllWordOccurrenceEntity( // Renamed
    @ColumnInfo(name = "occurrence_id") @PrimaryKey(autoGenerate = true)
    val occurrenceId: Long = 0,

    @ColumnInfo(name = "mapped_identifier_value") // NEW FK to AnalysisEntryEntity
    val mappedIdentifierValue: String,
    @ColumnInfo(name = "mapped_identifier_type") // NEW: Type of the entry it maps to
    val mappedIdentifierType: String,

    @ColumnInfo(name = "word_key") // "S:A:W"
    val wordKey: String,

    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_index")
    val ayahIndex: Int,
    @ColumnInfo(name = "word_index_in_ayah")
    val wordIndexInAyah: Int,

    @ColumnInfo(name = "arabic_text")
    val arabicText: String?,
    @ColumnInfo(name = "transliteration")
    val transliteration: String?,
    @ColumnInfo(name = "translation")
    val translation: String?,
    @ColumnInfo(name = "characters")
    val charactersJson: String?, // Keep _json suffix
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
data class EntryArabicFormWithFullDetails( // Renamed
    val entryArabicFormEntity: EntryArabicFormEntity, // Changed
    val transliterations: List<String>,
    val translations: List<String>
)

data class AnalysisEntryFullDetails( // Renamed
    val analysisEntryEntity: AnalysisEntryEntity,
    val arabicForms: List<EntryArabicFormWithFullDetails>, // Changed
    val contributingMorphForms: List<String>,
    val juzDistribution: List<EntryJuzDistributionEntity> // Changed
)

// New simple data class for identity lookups
data class WordIdentity(
    @ColumnInfo(name = "mapped_identifier_value") // Matches column name from query
    val value: String,
    @ColumnInfo(name = "mapped_identifier_type") // Matches column name from query
    val type: String
)
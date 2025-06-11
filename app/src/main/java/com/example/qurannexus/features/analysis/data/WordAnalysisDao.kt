package com.example.qurannexus.features.analysis.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.qurannexus.core.database.entities.*

@Dao
interface WordAnalysisDao {

    // --- AnalysisEntryEntity Operations ---
    @Query("SELECT * FROM analysis_entries WHERE identifier_value = :identifierValue")
    suspend fun getAnalysisEntry(identifierValue: String): AnalysisEntryEntity?

    @Query("SELECT * FROM analysis_entries ORDER BY total_occurrences DESC LIMIT :limit")
    suspend fun getMostFrequentEntries(limit: Int): List<AnalysisEntryEntity>

    @Query("SELECT * FROM analysis_entries WHERE identifier_value LIKE :query || '%' ORDER BY total_occurrences DESC LIMIT :limit")
    suspend fun searchEntriesByIdentifierValue(query: String, limit: Int = 20): List<AnalysisEntryEntity>

    @Query("""
        SELECT DISTINCT ae.* FROM analysis_entries ae
        LEFT JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value
        LEFT JOIN arabic_form_translations aft ON eaf.arabic_form_id = aft.arabic_form_id
        WHERE ae.identifier_value LIKE :query || '%'
        OR eaf.arabic_text LIKE :query || '%'
        OR aft.translation LIKE '%' || :query || '%'
        ORDER BY ae.total_occurrences DESC
        LIMIT :limit
    """)
    suspend fun searchEntriesByGenericQuery(query: String, limit: Int = 30): List<AnalysisEntryEntity>

    @Query("""
        SELECT DISTINCT ae.* FROM analysis_entries ae
        JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value
        WHERE eaf.arabic_text = :exactArabicText
    """)
    suspend fun findAnalysisEntriesByExactArabicForm(exactArabicText: String): List<AnalysisEntryEntity>

    @Query("""
        SELECT DISTINCT ae.* FROM analysis_entries ae
        JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value
        JOIN arabic_form_translations aft ON eaf.arabic_form_id = aft.arabic_form_id
        WHERE aft.translation LIKE '%' || :translationQuery || '%'
        ORDER BY ae.total_occurrences DESC
        LIMIT :limit
    """)
    suspend fun findAnalysisEntriesByTranslation(translationQuery: String, limit: Int = 30): List<AnalysisEntryEntity>

    // --- EntryArabicFormEntity Operations ---
    @Query("SELECT * FROM entry_arabic_forms WHERE parent_identifier_value = :identifierValue ORDER BY occurrences_of_this_specific_arabic_form DESC, arabic_text ASC")
    suspend fun getArabicFormsForEntry(identifierValue: String): List<EntryArabicFormEntity>

    @Query("SELECT * FROM arabic_form_transliterations WHERE arabic_form_id = :arabicFormId")
    suspend fun getTransliterationsForForm(arabicFormId: Long): List<ArabicFormTransliterationEntity>

    @Query("SELECT * FROM arabic_form_translations WHERE arabic_form_id = :arabicFormId")
    suspend fun getTranslationsForForm(arabicFormId: Long): List<ArabicFormTranslationEntity>

    @Transaction
    suspend fun getArabicFormsWithDetails(identifierValue: String): List<EntryArabicFormWithFullDetails> {
        val forms = getArabicFormsForEntry(identifierValue)
        return forms.map { formEntity ->
            EntryArabicFormWithFullDetails(
                entryArabicFormEntity = formEntity,
                transliterations = getTransliterationsForForm(formEntity.arabicFormId).mapNotNull { it.transliteration },
                translations = getTranslationsForForm(formEntity.arabicFormId).mapNotNull { it.translation }
            )
        }
    }

    // --- EntryContributingMorphFormEntity Operations ---
    @Query("SELECT morph_form FROM entry_contributing_morph_forms WHERE parent_identifier_value = :identifierValue ORDER BY morph_form ASC")
    suspend fun getContributingMorphFormsForEntry(identifierValue: String): List<String>

    // --- DistributionEntity Operations ---
    @Query("SELECT * FROM entry_occurrences_by_juz WHERE parent_identifier_value = :identifierValue ORDER BY juz_id ASC")
    suspend fun getJuzDistributionForEntry(identifierValue: String): List<EntryJuzDistributionEntity>

    @Query("SELECT * FROM entry_occurrences_by_surah WHERE parent_identifier_value = :identifierValue ORDER BY surah_id ASC")
    suspend fun getSurahDistributionForEntry(identifierValue: String): List<EntrySurahDistributionEntity>

    @Query("SELECT * FROM entry_occurrences_by_page WHERE parent_identifier_value = :identifierValue ORDER BY page_id ASC")
    suspend fun getPageDistributionForEntry(identifierValue: String): List<EntryPageDistributionEntity>


    // --- AllWordOccurrenceEntity Operations ---
    // Get occurrences for a specific entry (Root/Lemma/Form) in a Juz
    @Query("SELECT * FROM all_word_occurrences WHERE mapped_identifier_value = :identifierValue AND juz_id = :juzNumber ORDER BY surah_id ASC, ayah_index ASC, word_index_in_ayah ASC LIMIT :limit OFFSET :offset")
    suspend fun getOccurrencesForEntryInJuz(identifierValue: String, juzNumber: Int, limit: Int, offset: Int): List<AllWordOccurrenceEntity>

    @Query("SELECT COUNT(*) FROM all_word_occurrences WHERE mapped_identifier_value = :identifierValue AND juz_id = :juzNumber")
    suspend fun countOccurrencesForEntryInJuz(identifierValue: String, juzNumber: Int): Int

    // Get the specific AllWordOccurrenceEntity by S:A:W key
    @Query("SELECT * FROM all_word_occurrences WHERE word_key = :wordKey LIMIT 1")
    suspend fun getWordOccurrenceByWordKey(wordKey: String): AllWordOccurrenceEntity?

    // Get the identity (value and type) for a given S:A:W wordKey
    @Query("SELECT mapped_identifier_value, mapped_identifier_type FROM all_word_occurrences WHERE word_key = :wordKey LIMIT 1")
    suspend fun getIdentityForWordKey(wordKey: String): WordIdentity?

    // Get the identity for a given Arabic text (takes the first one found)
    @Query("SELECT mapped_identifier_value, mapped_identifier_type FROM all_word_occurrences WHERE arabic_text = :arabicText LIMIT 1")
    suspend fun getIdentityForArabicText(arabicText: String): WordIdentity?


    // --- Transaction for Full AnalysisEntry Details ---
    @Transaction
    suspend fun getAnalysisEntryFullDetails(identifierValue: String): AnalysisEntryFullDetails? {
        val entry = getAnalysisEntry(identifierValue) ?: return null
        return AnalysisEntryFullDetails(
            analysisEntryEntity = entry,
            arabicForms = getArabicFormsWithDetails(identifierValue),
            contributingMorphForms = getContributingMorphFormsForEntry(identifierValue),
            juzDistribution = getJuzDistributionForEntry(identifierValue)
            // Add other distributions if needed
        )
    }
}
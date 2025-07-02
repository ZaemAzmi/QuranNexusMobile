package com.example.qurannexus.features.analysis.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.qurannexus.core.database.entities.*

@Dao
interface WordAnalysisDao {

    companion object {
        const val SEARCH_PAGE_SIZE = 30
    }
    @Query("""
        SELECT * FROM analysis_entries
        WHERE 
            identifier_value IN (
                -- This subquery finds all matching entry identifiers
                -- using UNION to combine results from different search criteria
                -- and remove duplicates automatically.
                SELECT identifier_value FROM analysis_entries
                WHERE identifier_value LIKE :query || '%'
                
                UNION
                
                SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1
                WHERE T1.arabic_text LIKE :query || '%'
                
                UNION
                
                SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1
                JOIN arabic_form_translations AS T2 ON T1.arabic_form_id = T2.arabic_form_id
                WHERE T2.translation LIKE '%' || :query || '%'
            )
        ORDER BY total_occurrences DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchAllPaginated(query: String, limit: Int, offset: Int): List<AnalysisEntryEntity>

    @Query("SELECT * FROM analysis_entries WHERE identifier_value = :identifierValue")
    suspend fun getAnalysisEntry(identifierValue: String): AnalysisEntryEntity?

    @Query("SELECT * FROM analysis_entries ORDER BY total_occurrences DESC LIMIT :limit")
    suspend fun getMostFrequentEntries(limit: Int): List<AnalysisEntryEntity>

    @Query("""
        SELECT DISTINCT ae.* FROM analysis_entries ae
        JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value
        WHERE eaf.arabic_text = :exactArabicText
        ORDER BY ae.total_occurrences DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findAnalysisEntriesByExactArabicFormPaginated(exactArabicText: String, limit: Int, offset: Int): List<AnalysisEntryEntity>

    // NEW: Paginated search by translation
    @Query("""
        SELECT DISTINCT ae.* FROM analysis_entries ae
        JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value
        JOIN arabic_form_translations aft ON eaf.arabic_form_id = aft.arabic_form_id
        WHERE aft.translation LIKE '%' || :translationQuery || '%'
        ORDER BY ae.total_occurrences DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findAnalysisEntriesByTranslationPaginated(translationQuery: String, limit: Int, offset: Int): List<AnalysisEntryEntity>

    @Query("""
        SELECT COUNT(*) FROM analysis_entries WHERE identifier_value IN (
            SELECT identifier_value FROM analysis_entries WHERE identifier_value LIKE :query || '%'
            UNION
            SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1 WHERE T1.arabic_text LIKE :query || '%'
            UNION
            SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1 JOIN arabic_form_translations AS T2 ON T1.arabic_form_id = T2.arabic_form_id WHERE T2.translation LIKE '%' || :query || '%'
        )
    """)
    suspend fun countAll(query: String): Int
    @Query("""
    SELECT * FROM analysis_entries
    WHERE 
        identifier_type = :type AND  -- Filter by the specific type
        identifier_value IN (
            -- This subquery finds all matching entry identifiers regardless of type
            SELECT identifier_value FROM analysis_entries
            WHERE identifier_value LIKE :query || '%'
            
            UNION
            
            SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1
            WHERE T1.arabic_text LIKE :query || '%'
            
            UNION
            
            SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1
            JOIN arabic_form_translations AS T2 ON T1.arabic_form_id = T2.arabic_form_id
            WHERE T2.translation LIKE '%' || :query || '%'
        )
    ORDER BY total_occurrences DESC
    LIMIT :limit OFFSET :offset
    """)
    suspend fun searchAllAndFilterByTypePaginated(query: String, type: String, limit: Int, offset: Int): List<AnalysisEntryEntity>

    // NEW: The corresponding count query
    @Query("""
    SELECT COUNT(*) FROM analysis_entries
    WHERE 
        identifier_type = :type AND
        identifier_value IN (
            -- This subquery finds all matching entry identifiers regardless of type
            SELECT identifier_value FROM analysis_entries WHERE identifier_value LIKE :query || '%'
            UNION
            SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1 WHERE T1.arabic_text LIKE :query || '%'
            UNION
            SELECT T1.parent_identifier_value FROM entry_arabic_forms AS T1 JOIN arabic_form_translations AS T2 ON T1.arabic_form_id = T2.arabic_form_id WHERE T2.translation LIKE '%' || :query || '%'
        )
    """)
    suspend fun countAllAndFilterByType(query: String, type: String): Int
    @Query("SELECT COUNT(*) FROM analysis_entries WHERE identifier_value LIKE :query || '%' AND identifier_type = :type")
    suspend fun countByIdentifierAndType(query: String, type: String): Int

    @Query("SELECT COUNT(DISTINCT ae.identifier_value) FROM analysis_entries ae JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value WHERE eaf.arabic_text = :exactArabicText")
    suspend fun countByExactArabicForm(exactArabicText: String): Int

    @Query("SELECT COUNT(DISTINCT ae.identifier_value) FROM analysis_entries ae JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value JOIN arabic_form_translations aft ON eaf.arabic_form_id = aft.arabic_form_id WHERE aft.translation LIKE '%' || :translationQuery || '%'")
    suspend fun countByTranslation(translationQuery: String): Int
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
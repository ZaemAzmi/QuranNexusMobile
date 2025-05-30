package com.example.qurannexus.features.analysis.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.qurannexus.core.database.entities.*

@Dao
interface WordRootDao {

    // --- RootEntity Operations ---
    @Query("SELECT * FROM roots WHERE root_label = :rootLabel")
    suspend fun getRoot(rootLabel: String): RootEntity?

    @Query("SELECT * FROM roots ORDER BY total_occurrences DESC LIMIT :limit")
    suspend fun getMostFrequentRoots(limit: Int): List<RootEntity>

    @Query("SELECT * FROM roots WHERE root_label LIKE :query || '%' ORDER BY total_occurrences DESC LIMIT :limit")
    suspend fun searchRootsByLabel(query: String, limit: Int = 20): List<RootEntity>

    // Search for roots where the rootLabel OR any of its ArabicFormEntity.arabicText OR
    // any of its ArabicFormTranslationEntity.translation match the query.
    // This is a more complex query and might be split or refined for performance.
    // Returns RootEntity objects.
    @Query("""
        SELECT DISTINCT r.* FROM roots r
        LEFT JOIN root_arabic_forms raf ON r.root_label = raf.root_label
        LEFT JOIN arabic_form_translations rft ON raf.arabic_form_id = rft.arabic_form_id
        WHERE r.root_label LIKE :query || '%'
        OR raf.arabic_text LIKE :query || '%'  -- Match start of Arabic form
        OR rft.translation LIKE '%' || :query || '%' -- Match anywhere in translation
        ORDER BY r.total_occurrences DESC
        LIMIT :limit
    """)
    suspend fun searchRootsByGenericQuery(query: String, limit: Int = 30): List<RootEntity>

    // If you want to search for specific Arabic forms and then get their root:
    @Query("""
        SELECT DISTINCT r.* FROM roots r
        JOIN root_arabic_forms raf ON r.root_label = raf.root_label
        WHERE raf.arabic_text = :exactArabicText
    """)
    suspend fun findRootsByExactArabicForm(exactArabicText: String): List<RootEntity>

    // Search for roots by matching translation of any of its forms
    @Query("""
        SELECT DISTINCT r.* FROM roots r
        JOIN root_arabic_forms raf ON r.root_label = raf.root_label
        JOIN arabic_form_translations aft ON raf.arabic_form_id = aft.arabic_form_id
        WHERE aft.translation LIKE '%' || :translationQuery || '%'
        ORDER BY r.total_occurrences DESC
        LIMIT :limit
    """)
    suspend fun findRootsByTranslation(translationQuery: String, limit: Int = 30): List<RootEntity>
    // --- ArabicFormEntity Operations ---
    @Query("SELECT * FROM root_arabic_forms WHERE root_label = :rootLabel ORDER BY occurrences_of_this_specific_arabic_form DESC, arabic_text ASC")
    suspend fun getArabicFormsForRoot(rootLabel: String): List<ArabicFormEntity>

    @Query("SELECT * FROM arabic_form_transliterations WHERE arabic_form_id = :arabicFormId")
    suspend fun getTransliterationsForForm(arabicFormId: Long): List<ArabicFormTransliterationEntity>

    @Query("SELECT * FROM arabic_form_translations WHERE arabic_form_id = :arabicFormId")
    suspend fun getTranslationsForForm(arabicFormId: Long): List<ArabicFormTranslationEntity>

    @Transaction
    suspend fun getArabicFormsWithDetails(rootLabel: String): List<ArabicFormWithFullDetails> {
        val forms = getArabicFormsForRoot(rootLabel)
        return forms.map { formEntity ->
            ArabicFormWithFullDetails(
                arabicFormEntity = formEntity,
                transliterations = getTransliterationsForForm(formEntity.arabicFormId).mapNotNull { it.transliteration },
                translations = getTranslationsForForm(formEntity.arabicFormId).mapNotNull { it.translation }
            )
        }
    }
    // Query to find roots based on arabic_text of one of its forms
    @Query("""
        SELECT r.* FROM roots r
        INNER JOIN root_arabic_forms raf ON r.root_label = raf.root_label
        WHERE raf.arabic_text = :arabicText
    """)
    suspend fun findRootsByArabicFormText(arabicText: String): List<RootEntity>


    // --- RootContributingMorphFormEntity Operations ---
    @Query("SELECT morph_form FROM root_contributing_morph_forms WHERE root_label = :rootLabel ORDER BY morph_form ASC")
    suspend fun getContributingMorphFormsForRoot(rootLabel: String): List<String>

    // --- DistributionEntity Operations ---
    @Query("SELECT * FROM root_occurrences_by_juz WHERE root_label = :rootLabel ORDER BY juz_id ASC")
    suspend fun getJuzDistributionForRoot(rootLabel: String): List<RootJuzDistributionEntity>

    @Query("SELECT * FROM root_occurrences_by_surah WHERE root_label = :rootLabel ORDER BY surah_id ASC")
    suspend fun getSurahDistributionForRoot(rootLabel: String): List<RootSurahDistributionEntity>
    // Add getPageDistributionForRoot if needed

    // --- RootWordOccurrenceEntity Operations ---
    @Query("SELECT * FROM root_word_occurrences WHERE root_label = :rootLabel AND juz_id = :juzNumber ORDER BY surah_id ASC, ayah_index ASC, word_index_in_ayah ASC LIMIT :limit OFFSET :offset")
    suspend fun getOccurrencesForRootInJuz(rootLabel: String, juzNumber: Int, limit: Int, offset: Int): List<RootWordOccurrenceEntity>

    @Query("SELECT COUNT(*) FROM root_word_occurrences WHERE root_label = :rootLabel AND juz_id = :juzNumber")
    suspend fun countOccurrencesForRootInJuz(rootLabel: String, juzNumber: Int): Int

    @Query("SELECT * FROM root_word_occurrences WHERE word_key = :wordKey LIMIT 1")
    suspend fun getOccurrenceByWordKey(wordKey: String): RootWordOccurrenceEntity?

    // --- Transaction for Full Root Details (Example) ---
    @Transaction
    suspend fun getRootFullDetails(rootLabel: String): RootFullDetails? {
        val root = getRoot(rootLabel) ?: return null
        return RootFullDetails(
            rootEntity = root,
            arabicForms = getArabicFormsWithDetails(rootLabel),
            contributingMorphForms = getContributingMorphFormsForRoot(rootLabel),
            juzDistribution = getJuzDistributionForRoot(rootLabel)
        )
    }
}
package com.example.qurannexus.features.recitation.repository
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity
import com.example.qurannexus.features.recitation.data.RecitationDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecitationRepository @Inject constructor(private val recitationDao: RecitationDao) {
    fun getAyahsForPage(pageId: Int): Flow<List<QuranAyahDetailEntity>> {
        return recitationDao.getAyahsForPage(pageId)
    }
}
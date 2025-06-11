package com.example.qurannexus.core.di


import android.content.Context
import com.example.qurannexus.core.database.QuranNexusDatabase
import com.example.qurannexus.features.analysis.data.WordAnalysisDao // Updated DAO name
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuranNexusDatabase(@ApplicationContext context: Context): QuranNexusDatabase {
        return QuranNexusDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWordRootDao(database: QuranNexusDatabase): WordAnalysisDao { // Updated DAO name
        return database.wordAnalysisDao() // Updated DAO method name
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
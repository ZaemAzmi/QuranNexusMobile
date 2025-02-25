package com.example.qurannexus.features.bookmark.modules

import android.content.Context
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.TokenManager
import com.example.qurannexus.features.bookmark.interfaces.BookmarkApi
import com.example.qurannexus.features.bookmark.repositories.RecentlyReadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecentlyReadModule {

    @Provides
    @Singleton
    fun provideBookmarkApi(): BookmarkApi {
        return ApiService.getQuranClient().create(BookmarkApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager(context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE))
    }

    @Provides
    @Singleton
    fun provideRecentlyReadRepository(
        bookmarkApi: BookmarkApi,
        tokenManager: TokenManager
    ): RecentlyReadRepository {
        return RecentlyReadRepository(bookmarkApi, tokenManager)
    }
}
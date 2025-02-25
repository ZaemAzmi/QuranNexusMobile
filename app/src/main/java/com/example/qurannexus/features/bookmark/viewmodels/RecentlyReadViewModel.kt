package com.example.qurannexus.features.bookmark.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.exceptions.AuthException
import com.example.qurannexus.features.bookmark.enums.RecentlyReadType
import com.example.qurannexus.features.bookmark.models.RecentlyRead
import javax.inject.Inject
import com.example.qurannexus.core.utils.Result
import com.example.qurannexus.features.bookmark.repositories.RecentlyReadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel
class RecentlyReadViewModel @Inject constructor(
    private val recentlyReadRepository: RecentlyReadRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    // Current type of recently read items being displayed
    private val _currentType = MutableLiveData<RecentlyReadType>(RecentlyReadType.CHAPTER)
    val currentType: LiveData<RecentlyReadType> = _currentType

    // Navigation events
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    init {
        fetchRecentlyRead()
    }

    fun fetchRecentlyRead() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = recentlyReadRepository.getRecentlyRead(_currentType.value!!)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Content(result.data)
                    }
                }
                is Result.Error -> {
                    val errorMessage = when (result.exception) {
                        is AuthException -> result.exception.message ?: "Authentication error"
                        else -> "Failed to load recently read items"
                    }
                    _uiState.value = UiState.Error(errorMessage)
                }
            }
        }
    }

    fun updateType(type: RecentlyReadType) {
        if (_currentType.value != type) {
            _currentType.value = type
            fetchRecentlyRead()
        }
    }

    fun deleteRecentlyRead(item: RecentlyRead, type: RecentlyReadType) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = recentlyReadRepository.removeRecentlyRead(item, type)) {
                is Result.Success -> fetchRecentlyRead() // Refresh list after successful deletion
                is Result.Error -> {
                    val errorMessage = when (result.exception) {
                        is AuthException -> result.exception.message ?: "Authentication error"
                        else -> "Failed to delete item"
                    }
                    _uiState.value = UiState.Error(errorMessage)
                }
            }
        }
    }

    fun handleItemClick(item: RecentlyRead, type: RecentlyReadType) {
        when (type) {
            RecentlyReadType.CHAPTER -> navigateToChapter(item.itemId.toInt())
            RecentlyReadType.PAGE -> navigateToPage(item.itemId.toInt())
            RecentlyReadType.JUZ -> navigateToJuz(item.itemId.toInt())
        }
    }

    private fun navigateToChapter(chapterNumber: Int) {
        _navigationEvent.value = NavigationEvent.ToChapter(chapterNumber)
    }

    private fun navigateToPage(pageNumber: Int) {
        _navigationEvent.value = NavigationEvent.ToPage(pageNumber)
    }

    private fun navigateToJuz(juzNumber: Int) {
        _navigationEvent.value = NavigationEvent.ToJuz(juzNumber)
    }

    // Sealed classes for state management
    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        data class Content(val items: List<RecentlyRead>) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class NavigationEvent {
        data class ToChapter(val chapterNumber: Int) : NavigationEvent()
        data class ToPage(val pageNumber: Int) : NavigationEvent()
        data class ToJuz(val juzNumber: Int) : NavigationEvent()
    }
}
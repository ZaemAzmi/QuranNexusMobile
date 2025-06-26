package com.example.qurannexus.features.recitation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity
import com.example.qurannexus.features.recitation.repository.RecitationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecitationViewModel @Inject constructor(
    private val repository: RecitationRepository
) : ViewModel() {

    private val _pageData = MutableStateFlow<PageDataState>(PageDataState.Loading)
    val pageData: StateFlow<PageDataState> = _pageData.asStateFlow()

    fun loadPageData(pageId: Int) {
        viewModelScope.launch {
            _pageData.value = PageDataState.Loading
            repository.getAyahsForPage(pageId)
                .catch { e ->
                    _pageData.value = PageDataState.Error(e.message ?: "An unknown error occurred")
                }
                .collect { ayahs ->
                    if (ayahs.isEmpty()) {
                        _pageData.value = PageDataState.Error("No data found for page $pageId")
                    } else {
                        _pageData.value = PageDataState.Success(ayahs)
                    }
                }
        }
    }
}

// Sealed class to represent UI state
sealed class PageDataState {
    data class Success(val ayahs: List<QuranAyahDetailEntity>) : PageDataState()
    data class Error(val message: String) : PageDataState()
    object Loading : PageDataState()
}
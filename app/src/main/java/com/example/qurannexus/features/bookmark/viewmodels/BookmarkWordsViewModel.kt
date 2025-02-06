package com.example.qurannexus.features.bookmark.viewmodels

//class BookmarkWordsViewModel : ViewModel() {
//
//    // List of English and Arabic alphabets
//    val englishAlphabets = ('A'..'Z').map { it.toString() }
//    val arabicAlphabets = listOf("أ", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "ه", "و", "ي")
//
//    // LiveData to track selected filter type
//    private val _selectedFilterType = MutableLiveData<String>("Arabic") // Default to Arabic
//    val selectedFilterType: LiveData<String> get() = _selectedFilterType
//
//    // LiveData to track filtered characters
//    private val _filteredCharacters = MutableLiveData<List<String>>()
//    val filteredCharacters: LiveData<List<String>> get() = _filteredCharacters
//
//    // Set the filter type
//    fun setFilterType(filterType: String) {
//        _selectedFilterType.value = filterType
//        _filteredCharacters.value = if (filterType == "Arabic") arabicAlphabets else englishAlphabets
//    }
//
//    fun getCategorizedWords(): LiveData<List<AccordionSection>> {
//        val sections = when (_selectedFilterType.value) {
//            "English" -> categorizeByEnglish()
//            "Arabic" -> categorizeByArabic()
//            else -> emptyList()
//        }
//        return MutableLiveData(sections)
//    }
//
//    private fun categorizeByEnglish(): List<AccordionSection> {
//        // Example logic to map Arabic words to English categories
//        val map = mutableMapOf<String, MutableList<String>>()
//        arabicWords.forEach { word ->
//            val category = mapArabicToEnglishCategory(word)
//            map.getOrPut(category) { mutableListOf() }.add(word)
//        }
//        return map.map { AccordionSection(it.key, it.value.size, false, it.value) }
//    }
//
//    private fun categorizeByArabic(): List<AccordionSection> {
//        return arabicAlphabets.map { letter ->
//            val words = arabicWords.filter { it.startsWith(letter) }
//            AccordionSection(letter, words.size, false, words)
//        }
//    }
//
//}

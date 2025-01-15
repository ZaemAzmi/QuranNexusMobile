package com.example.qurannexus.features.words

import com.example.qurannexus.features.bookmark.models.BookmarkWord
import com.example.qurannexus.features.home.models.WordDetails

data class AccordionSection(val title: String, val words: List<BookmarkWord>)
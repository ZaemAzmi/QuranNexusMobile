package com.example.qurannexus.core.enums

enum class BottomMenuItemId(val  id: Int) {
    HOME(1),
    SURAHLIST(2),
    ANALYSIS(3),
    BOOKMARK(4),
    QUIZ(5);

    companion object {
        @JvmStatic
        fun fromId(id: Int): BottomMenuItemId? {
            return entries.find { it.id == id }
        }
    }
}
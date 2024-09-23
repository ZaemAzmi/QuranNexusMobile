package com.example.qurannexus.enums

enum class BottomMenuItemId(val  id: Int) {
    HOME(1),
    IRAB(2),
    TAJWEED(3),
    TEST(4);

    companion object {
        @JvmStatic
        fun fromId(id: Int): BottomMenuItemId? {
            return entries.find { it.id == id }
        }
    }
}
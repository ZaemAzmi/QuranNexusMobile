package com.example.qurannexus.core.utils

import android.R
import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import java.lang.String
import kotlin.Exception
import kotlin.Int
import kotlin.arrayOf


class UtilityService {
    fun convertToArabicNumber(number : Int): kotlin.String {
        val arabicNumbers = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        val arabicNumber = StringBuilder()
        val numStr = String.valueOf(number)

        for (digit in numStr.toCharArray()) {
            arabicNumber.append(arabicNumbers[Character.getNumericValue(digit)])
        }

        return arabicNumber.toString()
    }

    // Add new method for handling bottom navigation insets
    fun setupBottomNavPadding(fragment: Fragment?, contentView: View?) {
        if (fragment == null || contentView == null) return

        try {
            val context: Context = fragment.getContext() ?: return

            ViewCompat.setOnApplyWindowInsetsListener(contentView) { v: View?, windowInsets: WindowInsetsCompat ->
                val navBarHeight =
                    windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val meowNavHeight =
                    fragment.resources.getDimensionPixelSize(com.example.qurannexus.R.dimen.meow_bottom_nav_height)

                // Set bottom padding to accommodate both the navigation bar and bottom nav
                val totalBottomPadding = meowNavHeight + navBarHeight
                contentView.setPadding(
                    contentView.paddingLeft,
                    contentView.paddingTop,
                    contentView.paddingRight,
                    totalBottomPadding
                )
                windowInsets
            }
        } catch (e: Exception) {
            Log.e("UtilityService", "Error setting up bottom nav padding", e)
        }
    }
}
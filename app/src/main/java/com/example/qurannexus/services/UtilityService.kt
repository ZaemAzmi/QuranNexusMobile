package com.example.qurannexus.services

import java.lang.String
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
}
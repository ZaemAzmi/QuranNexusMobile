package com.example.qurannexus.core.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListConverter {
    private val gson = Gson()

    // This single pair of converters will handle any List<String> <-> String conversion.
    // Room will apply this wherever it sees a field of type List<String>
    // that needs to be stored as TEXT in the database, provided this
    // converter is registered with the @Database or @Entity.

    @TypeConverter
    fun fromStringListToJsonString(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun fromJsonStringToStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }
    }
}
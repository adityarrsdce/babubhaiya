package com.babu.appp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

suspend fun fetchSemesterLinks(jsonUrl: String): Map<String, String>? {
    return withContext(Dispatchers.IO) {
        try {
            val jsonText = URL(jsonUrl).readText()
            val json = JSONObject(jsonText)
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key ->
                map[key] = json.getString(key)
            }
            map
        } catch (e: Exception) {
            null
        }
    }
}

fun convertSemesterToRoman(sem: String): String {
    return when (sem) {
        "1" -> "I"
        "2" -> "II"
        "3" -> "III"
        "4" -> "IV"
        "5" -> "V"
        "6" -> "VI"
        "7" -> "VII"
        "8" -> "VIII"
        else -> sem
    }
}

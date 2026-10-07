package com.armeasure.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * MeasurementData — একটি measurement record store করে।
 */
data class MeasurementData(
    val distance: Double,
    val unit: String,
    val timestamp: Long,
    val label: String = ""
) {
    /**
     * Human-readable format-এ measurement দেখায়।
     */
    fun getFormattedDistance(): String {
        return String.format("%.1f %s", distance, unit)
    }

    /**
     * Timestamp কে readable date-time string-এ convert করে।
     */
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

/**
 * MeasurementStore — Measurement history SharedPreferences-এ save করে।
 * যাতে app বন্ধ করেও history দেখা যায়।
 */
object MeasurementStore {

    private const val PREF_NAME = "ar_measure_history"
    private const val KEY_MEASUREMENTS = "measurements"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * নতুন measurement save করে history-তে।
     */
    fun addMeasurement(context: Context, data: MeasurementData) {
        val list = getAllMeasurements(context).toMutableList()
        list.add(0, data) // নতুনটা সবার উপরে

        // Maximum 100 measurements রাখো
        if (list.size > 100) {
            list.removeAt(list.size - 1)
        }

        saveMeasurements(context, list)
    }

    /**
     * সব measurements return করে।
     */
    fun getAllMeasurements(context: Context): List<MeasurementData> {
        val json = getPrefs(context).getString(KEY_MEASUREMENTS, null) ?: return emptyList()
        val type = object : TypeToken<List<MeasurementData>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * সব measurements save করে।
     */
    private fun saveMeasurements(context: Context, list: List<MeasurementData>) {
        val json = gson.toJson(list)
        getPrefs(context).edit().putString(KEY_MEASUREMENTS, json).apply()
    }

    /**
     * সব history মুছে ফেলে।
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

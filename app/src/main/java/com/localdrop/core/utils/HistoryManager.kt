package com.localdrop.core.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val fileName: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val isSent: Boolean
)

object HistoryManager {
    private const val PREF_NAME = "localdrop_history_pref"
    private const val KEY_HISTORY = "history_list"

    fun saveTransfer(context: Context, fileName: String, sizeBytes: Long, isSent: Boolean = true) {
        val list = getHistory(context).toMutableList()
        list.add(0, HistoryItem(fileName, sizeBytes, System.currentTimeMillis(), isSent))
        val array = JSONArray()
        list.take(50).forEach { item ->
            val obj = JSONObject()
            obj.put("fileName", item.fileName)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("timestamp", item.timestamp)
            obj.put("isSent", item.isSent)
            array.put(obj)
        }
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<HistoryItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        fileName = obj.optString("fileName", "Unknown"),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isSent = obj.optBoolean("isSent", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun clearHistory(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().remove(KEY_HISTORY).apply()
    }
}

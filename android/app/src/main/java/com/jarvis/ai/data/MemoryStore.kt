package com.jarvis.ai.data

import android.content.Context
import org.json.JSONArray

/** Sirf user ki di hui memories. TODO: release se pehle EncryptedSharedPreferences. */
class MemoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)

    fun all(): List<String> {
        val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        return List(arr.length()) { arr.getString(it) }
    }

    fun add(text: String) {
        save(all() + text.trim())
    }

    fun delete(index: Int) {
        val list = all().toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            save(list)
        }
    }

    private fun save(list: List<String>) {
        prefs.edit().putString(KEY, JSONArray(list).toString()).apply()
    }

    companion object {
        private const val KEY = "items"
    }
}

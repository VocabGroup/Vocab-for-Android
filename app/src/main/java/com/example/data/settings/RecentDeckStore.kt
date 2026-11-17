package com.example.data.settings

import android.content.Context

class RecentDeckStore(context: Context) {
    private val prefs = context.getSharedPreferences("recent_decks", Context.MODE_PRIVATE)
    fun ids(): List<Long> = prefs.getString("ids", "").orEmpty().split(',').mapNotNull(String::toLongOrNull).distinct().take(5)
    fun open(id: Long) { prefs.edit().putString("ids", (listOf(id) + ids().filter { it != id }).take(5).joinToString(",")).apply() }
    fun clear() { prefs.edit().clear().apply() }
}

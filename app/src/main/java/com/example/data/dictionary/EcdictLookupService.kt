package com.example.data.dictionary

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/** Pinned, bundled MIT ECDICT. Reads one small shard on the IO dispatcher. */
object EcdictLookupService {
    fun key(query: String): String {
        val key = query.trim().lowercase(Locale.ROOT).replace('’', '\'').replace('‘', '\'')
        require(key.length in 1..80 && key.matches(Regex("[a-z '-]+"))) {
            "ECDICT 支援英文單字或短語；中文請選擇 Google 翻譯"
        }
        return key
    }

    fun shard(key: String): String {
        var hash = 2166136261L
        key.forEach { hash = ((hash xor it.code.toLong()) * 16777619L) and 0xffffffffL }
        return (hash and 255).toString(16).padStart(2, '0')
    }

    suspend fun lookup(context: Context, query: String): DictionaryEntry = withContext(Dispatchers.IO) {
        val key = key(query)
        val data = context.assets.open("ecdict/${shard(key)}.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        val row = data.optJSONArray(key) ?: error("ECDICT 查無此單字；可改用其他來源")
        val phonetic = row.getString(1)
        DictionaryEntry(row.getString(0), if (phonetic.isBlank()) "" else "/$phonetic/",
            row.getString(2), row.getString(3), "", "", "ECDICT 英漢字典（繁體中文）")
    }
}

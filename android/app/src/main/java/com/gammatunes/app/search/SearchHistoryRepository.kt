package com.gammatunes.app.search

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Most-recent-first list of search queries, persisted in SharedPreferences. */
class SearchHistory(private val key: String) {
    private val gson = Gson()
    private var prefs: android.content.SharedPreferences? = null
    private val _items = MutableStateFlow<List<String>>(emptyList())
    val items: StateFlow<List<String>> = _items.asStateFlow()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("search_history", Context.MODE_PRIVATE)
        prefs = p
        val json = p.getString(key, null) ?: return
        _items.value = runCatching {
            gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun add(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        update((listOf(q) + _items.value.filterNot { it.equals(q, ignoreCase = true) }).take(MAX))
    }

    fun remove(query: String) = update(_items.value - query)

    fun clear() = update(emptyList())

    private fun update(list: List<String>) {
        _items.value = list
        prefs?.edit()?.putString(key, gson.toJson(list))?.apply()
    }

    private companion object {
        const val MAX = 20
    }
}

object SearchHistoryRepository {
    /** Online search (YouTube Music / SoundCloud). */
    val online = SearchHistory("online")

    /** Searches over downloaded tracks (offline mode and the cached-tracks screen). */
    val cached = SearchHistory("cached")

    fun init(context: Context) {
        online.init(context)
        cached.init(context)
    }
}

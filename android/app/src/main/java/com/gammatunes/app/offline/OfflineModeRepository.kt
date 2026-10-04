package com.gammatunes.app.offline

import com.gammatunes.app.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Offline mode: search and recommendations only use downloaded (cached) tracks.
 * Deliberately not persisted, so the app always starts online and offers the
 * switch again if there is still no connection.
 */
object OfflineModeRepository {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun set(value: Boolean) {
        _enabled.value = value
    }
}

/** Case-insensitive match: every word of [query] must occur in title, artist or album. */
fun List<Track>.filterByQuery(query: String): List<Track> {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return this
    return filter { t ->
        val hay = "${t.title} ${t.artist} ${t.album.orEmpty()}".lowercase()
        words.all { hay.contains(it) }
    }
}

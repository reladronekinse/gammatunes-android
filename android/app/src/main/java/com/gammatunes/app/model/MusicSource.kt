package com.gammatunes.app.model

/** Where search looks for music. [apiValue] is sent as the `source` query param. */
enum class MusicSource(val apiValue: String) {
    YTM("ytm"),
    SOUNDCLOUD("soundcloud"),
}

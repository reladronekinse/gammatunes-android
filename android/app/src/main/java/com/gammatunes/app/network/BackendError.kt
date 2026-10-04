package com.gammatunes.app.network

import com.google.gson.JsonParser
import retrofit2.HttpException

/**
 * Human-readable text for a failed backend call. The embedded backend puts the
 * real reason into the JSON `detail` field of non-2xx responses; Retrofit's own
 * message is just "HTTP 502 Bad Gateway".
 */
fun Throwable.backendMessage(): String {
    if (this is HttpException) {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val detail = runCatching {
            JsonParser.parseString(body).asJsonObject.get("detail").asString
        }.getOrNull()
        if (!detail.isNullOrBlank()) return detail.take(300)
    }
    return message ?: toString()
}

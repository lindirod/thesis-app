package com.example.thesis.data.remote

import android.util.Log
import com.example.thesis.data.model.TrackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.text.Charsets

object LastFmService {
    private const val LASTFM_API_KEY = com.example.thesis.BuildConfig.LASTFM_API_KEY
    private const val LASTFM_BASE_URL = "https://ws.audioscrobbler.com/2.0/"


    /**
     * Fetches track tags.
     */
    suspend fun getTrackTags(artist: String, track: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val urlString = "$LASTFM_BASE_URL?" +
                        "method=track.getInfo" +
                        "&artist=${URLEncoder.encode(artist.trim(), "UTF-8")}" +
                        "&track=${URLEncoder.encode(track.trim(), "UTF-8")}" +
                        "&api_key=$LASTFM_API_KEY" +
                        "&format=json"

                val json = fetchJson(urlString) ?: return@withContext emptyList()
                parseTags(json)
            } catch (_: Exception) {
                Log.e("LastFm", "Error getting track tags for $track")
                emptyList()
            }
        }

    /**
     * Fetches artist tags as fallback.
     */
    suspend fun getArtistTags(artist: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val urlString = "$LASTFM_BASE_URL?" +
                        "method=artist.getTopTags" +
                        "&artist=${URLEncoder.encode(artist.trim(), "UTF-8")}" +
                        "&api_key=$LASTFM_API_KEY" +
                        "&format=json"

                val json = fetchJson(urlString) ?: return@withContext emptyList()
                parseTags(json)
            } catch (_: Exception) {
                Log.e("LastFm", "Error getting artist tags for $artist")
                emptyList()
            }
        }

    /**
     * Fetches top tracks for a specific tag.
     */
    suspend fun getTagTopTracks(tag: String, limit: Int = 10): List<TrackResult> =
        withContext(Dispatchers.IO) {
            try {
                val urlString = "$LASTFM_BASE_URL?" +
                        "method=tag.getTopTracks" +
                        "&tag=${URLEncoder.encode(tag.trim(), "UTF-8")}" +
                        "&api_key=$LASTFM_API_KEY" +
                        "&limit=$limit" +
                        "&format=json"

                val json = fetchJson(urlString) ?: return@withContext emptyList()
                parseTopTracks(json)
            } catch (_: Exception) {
                Log.e("LastFm", "Error getting top tracks for tag $tag")
                emptyList()
            }
        }

    /**
     * Fetches similar tracks for a specific track.
     */
    suspend fun getSimilarTracks(artist: String, track: String, limit: Int = 10): List<TrackResult> =
        withContext(Dispatchers.IO) {
            try {
                val urlString = "$LASTFM_BASE_URL?" +
                        "method=track.getSimilar" +
                        "&artist=${URLEncoder.encode(artist.trim(), "UTF-8")}" +
                        "&track=${URLEncoder.encode(track.trim(), "UTF-8")}" +
                        "&api_key=$LASTFM_API_KEY" +
                        "&limit=$limit" +
                        "&format=json"

                val json = fetchJson(urlString) ?: return@withContext emptyList()
                parseSimilarTracks(json)
            } catch (_: Exception) {
                Log.e("LastFm", "Error getting similar tracks for $track")
                emptyList()
            }
        }

    private fun fetchJson(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            if (connection.responseCode != 200) return null
            // Explicitly use UTF-8
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Unescapes Unicode sequences like \u00e9 and common JSON escapes.
     */
    private fun unescapeJson(str: String): String {
        val unicodeRegex = Regex("""\\u([0-9a-fA-F]{4})""")
        val decoded = unicodeRegex.replace(str) { matchResult ->
            matchResult.groupValues[1].toInt(16).toChar().toString()
        }
        return decoded.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun parseString(json: String, key: String): String {
        val pattern = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val value = pattern.find(json)?.groupValues?.get(1) ?: ""
        return unescapeJson(value)
    }

    private fun splitTrackBlocks(json: String): List<String> {
        val tracksStart = json.indexOf("\"track\":[")
        if (tracksStart == -1) return emptyList()

        val arrayStart = json.indexOf("[", tracksStart)
        val blocks = mutableListOf<String>()
        var depth = 0
        var blockStart = -1
        var i = arrayStart

        while (i < json.length) {
            when (json[i]) {
                '[', '{' -> {
                    if (json[i] == '{' && depth == 1) blockStart = i
                    depth++
                }
                '}', ']' -> {
                    depth--
                    if (json[i] == '}' && depth == 1 && blockStart != -1) {
                        blocks.add(json.substring(blockStart, i + 1))
                        blockStart = -1
                    }
                    if (depth == 0) return blocks
                }
            }
            i++
        }
        return blocks
    }

    private fun parseTopTracks(json: String): List<TrackResult> {
        return splitTrackBlocks(json).map { block ->
            val artistBlock = Regex(""""artist"\s*:\s*\{([^}]+)\}""")
                .find(block)?.groupValues?.get(1) ?: ""
            TrackResult(
                name = parseString(block, "name"),
                artist = parseString(artistBlock, "name"),
                url = parseString(block, "url")
            )
        }
    }

    private fun parseSimilarTracks(json: String): List<TrackResult> {
        val rootStart = json.indexOf("\"similartracks\":")
        if (rootStart == -1) return emptyList()
        return parseTopTracks(json.substring(rootStart))
    }

    private fun parseTags(json: String): List<String> {
        val tagsSection = Regex(""""toptags"\s*:\s*\{.*?"tag"\s*:\s*\[(.+?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(json)?.groupValues?.get(1) ?: return emptyList()

        return Regex(""""name"\s*:\s*"([^"]+)"""")
            .findAll(tagsSection)
            .map { unescapeJson(it.groupValues[1]).lowercase().trim() }
            .toList()
    }
}

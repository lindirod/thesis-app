package com.example.thesis.data.remote

import android.util.Log
import com.example.thesis.data.model.TrackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.text.Charsets

object DeezerService {
    /**
     * Searches for tracks on Deezer using artist and track name.
     */
    suspend fun searchTracks(artist: String, track: String): List<TrackResult> =
        withContext(Dispatchers.IO) {
            try {
                val query = "${artist.trim()} ${track.trim()}"

                //Limit set to 5 to save bandwith and processing time
                val url = "https://api.deezer.com/search?q=${URLEncoder.encode(query, "UTF-8")}&limit=5&output=json"

                val json = fetchUrl(url) ?: return@withContext emptyList()

                // Turn the raw text into a list of TrackResult objects
                parseSearchJson(json)
            } catch (e: Exception) {
                Log.e("Deezer", "Search error for $artist - $track: ${e.message}")
                emptyList()
            }
        }

    /**
     * Extracts the BPM, the Preview and the Duration of track.
     * If a deezerId is provided, it fetches that specific track directly.
     * Otherwise, it searches and uses the first result.
     */
    suspend fun getTrackDetails(artist: String, track: String, deezerId: String? = null): Triple<String, Int, Int> =
        withContext(Dispatchers.IO) {
            try {
                val idToUse = if (!deezerId.isNullOrBlank()) {
                    deezerId
                } else {
                    val results = searchTracks(artist, track)
                    if (results.isEmpty()) return@withContext Triple("", 0, 0)
                    results[0].deezerId
                }

                val trackUrl = "https://api.deezer.com/track/$idToUse"
                val trackJson = fetchUrl(trackUrl) ?: return@withContext Triple("", 0, 0)

                val previewRegex = Regex(""""preview"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                val bpmRegex = Regex(""""bpm"\s*:\s*([\d.]+)""")
                val durationRegex = Regex(""""duration"\s*:\s*(\d+)""")

                val preview = previewRegex.find(trackJson)?.groupValues?.get(1) ?: ""
                val bpmStr = bpmRegex.find(trackJson)?.groupValues?.get(1) ?: "0"
                val bpm = try { bpmStr.toDouble().toInt() } catch (_: Exception) { 0 }
                val duration = durationRegex.find(trackJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                val finalPreview = unescapeJson(preview)
                val safePreview = if (finalPreview == "null") "" else finalPreview

                Triple(safePreview, bpm, duration)
            } catch (e: Exception) {
                Log.e("Deezer", "Details fetch error for $track: ${e.message}")
                Triple("", 0, 0)
            }
        }

    /**
     * Fetches details for a track, specifically the Preview URL.
     */

    private fun fetchUrl(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            if (connection.responseCode != 200) {
                Log.e("Deezer", "HTTP Error ${connection.responseCode} for $urlString")
                null
            } else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e("Deezer", "Network error: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Cleans up the text returned by the API
     * Uses Regex to find Unicode sequences and converts them back into readable characters
     * */
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

    /**
     * Converts the raw JSON text from a search into Kotlin objects
     * */
    private fun parseSearchJson(json: String): List<TrackResult> {
        val dataStart = json.indexOf("\"data\"")
        if (dataStart == -1) return emptyList()

        val arrStart = json.indexOf("[", dataStart)
        if (arrStart == -1) return emptyList()

        val blocks = mutableListOf<String>()
        var depth = 0
        var blockStart = -1
        var i = arrStart

        while (i < json.length) {
            when (json[i]) {
                '[' -> depth++
                '{' -> { if (depth == 1) blockStart = i; depth++ }
                '}' -> {
                    depth--
                    if (depth == 1 && blockStart != -1) {
                        blocks.add(json.substring(blockStart, i + 1))
                        blockStart = -1
                    }
                }
                ']' -> { depth--; if (depth == 0) break }
            }
            i++
        }

        return blocks.mapNotNull { block ->
            val titleRegex = Regex(""""title"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val artistNameRegex = Regex(""""name"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val previewRegex = Regex(""""preview"\s*:\s*"((?:[^"\\]|\\.)*)"""")

            val title = titleRegex.find(block)?.groupValues?.get(1)
            val artistObj = Regex(""""artist"\s*:\s*\{([^}]+)\}""").find(block)?.groupValues?.get(1) ?: ""
            val artistName = artistNameRegex.find(artistObj)?.groupValues?.get(1)
            val link = Regex(""""link"\s*:\s*"([^"]+)"""").find(block)?.groupValues?.get(1) ?: ""
            val id = Regex(""""id"\s*:\s*(\d+)""").find(block)?.groupValues?.get(1) ?: ""
            val preview = previewRegex.find(block)?.groupValues?.get(1) ?: ""
            val duration = Regex(""""duration"\s*:\s*(\d+)""").find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            
            val finalPreview = unescapeJson(preview)
            val safePreview = if (finalPreview == "null") "" else finalPreview
            
            if (title != null && artistName != null) {
                TrackResult(
                    unescapeJson(title), 
                    unescapeJson(artistName), 
                    link, 
                    deezerId = id,
                    previewUrl = safePreview,
                    durationSeconds = duration
                )
            } else null
        }
    }
}

package com.example.thesis.data

import android.content.Context
import com.example.thesis.data.model.SavedPlaylist
import com.example.thesis.data.model.TrackResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

data class UserProfile(
    val age: String,
    val gender: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ResearchData(
    val sessions: List<UserSession>? = emptyList()
)

data class UserSession(
    val sessionId: Long = System.currentTimeMillis(),
    val userProfile: UserProfile? = null,
    val energeticSeeds: List<TrackResult>? = emptyList(),
    val calmSeeds: List<TrackResult>? = emptyList(),
    val playlists: List<SavedPlaylist>? = emptyList(),
    val measuredAverageBpm: Double? = 0.0,
    val initialCalibrationReadings: List<Int>? = emptyList()
)

class ResearchDataManager(context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File(context.filesDir, "research_data.json")
    private val prefs = context.getSharedPreferences("research_prefs", Context.MODE_PRIVATE)

    private var currentSessionId: Long?
        get() = if (prefs.contains("current_session_id")) prefs.getLong("current_session_id", -1) else null
        set(value) {
            if (value == null) prefs.edit().remove("current_session_id").apply()
            else prefs.edit().putLong("current_session_id", value).apply()
        }

    fun createNewSession(age: String, gender: String): Long {
        val currentData = loadData()
        val newProfile = UserProfile(age, gender)
        val newSession = UserSession(userProfile = newProfile)
        
        val updatedSessions = currentData.sessions.orEmpty() + newSession
        val newData = ResearchData(sessions = updatedSessions)
        
        val json = gson.toJson(newData)
        file.writeText(json)
        
        currentSessionId = newSession.sessionId
        return newSession.sessionId
    }

    fun loadData(): ResearchData {
        if (!file.exists()) return ResearchData()
        return try {
            val json = file.readText()
            val type = object : TypeToken<ResearchData>() {}.type
            gson.fromJson(json, type) ?: ResearchData()
        } catch (_: Exception) {
            ResearchData()
        }
    }

    fun updateCurrentSessionPlaylists(newPlaylists: List<SavedPlaylist>) {
        val sessionId = currentSessionId ?: return
        val currentData = loadData()
        
        val updatedSessions = currentData.sessions.orEmpty().map { session ->
            if (session.sessionId == sessionId) {
                // Merge new/updated playlists with existing ones, replacing by ID
                val newIds = newPlaylists.map { it.id }.toSet()
                val merged = session.playlists.orEmpty().filter { it.id !in newIds } + newPlaylists
                session.copy(playlists = merged)
            } else session
        }
        
        val newData = ResearchData(sessions = updatedSessions)
        file.writeText(gson.toJson(newData))
    }

    fun updateCurrentSessionSeeds(
        energetic: List<TrackResult>,
        calm: List<TrackResult>,
        avgBpm: Double? = null,
        calibrationReadings: List<Int>? = null
    ) {
        val sessionId = currentSessionId ?: return
        val currentData = loadData()
        
        val updatedSessions = currentData.sessions.orEmpty().map { session ->
            if (session.sessionId == sessionId) {
                session.copy(
                    energeticSeeds = energetic, 
                    calmSeeds = calm,
                    measuredAverageBpm = avgBpm ?: session.measuredAverageBpm,
                    initialCalibrationReadings = calibrationReadings ?: session.initialCalibrationReadings
                )
            } else session
        }
        
        val newData = ResearchData(sessions = updatedSessions)
        file.writeText(gson.toJson(newData))
    }

    fun deleteSession(sessionId: Long) {
        val currentData = loadData()
        val updatedSessions = currentData.sessions.orEmpty().filter { it.sessionId != sessionId }
        val newData = ResearchData(sessions = updatedSessions)
        file.writeText(gson.toJson(newData))
    }

    // Get playlists ONLY for the current active session
    fun getCurrentSessionPlaylists(): List<SavedPlaylist> {
        val sessionId = currentSessionId ?: return emptyList()
        return loadData().sessions.orEmpty().find { it.sessionId == sessionId }?.playlists.orEmpty()
    }

    fun getCurrentSessionSeeds(): Triple<List<TrackResult>, List<TrackResult>, Double> {
        val sessionId = currentSessionId ?: return Triple(emptyList(), emptyList(), 0.0)
        val session = loadData().sessions.orEmpty().find { it.sessionId == sessionId }
        return Triple(
            session?.energeticSeeds.orEmpty(),
            session?.calmSeeds.orEmpty(),
            session?.measuredAverageBpm ?: 0.0
        )
    }

    fun getCurrentSessionInitialReadings(): List<Int> {
        val sessionId = currentSessionId ?: return emptyList()
        return loadData().sessions.orEmpty().find { it.sessionId == sessionId }?.initialCalibrationReadings.orEmpty()
    }

    fun clearCurrentSession() {
        currentSessionId = null
    }
}

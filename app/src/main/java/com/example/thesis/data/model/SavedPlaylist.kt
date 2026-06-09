package com.example.thesis.data.model

/**
 *  Links the real-time heart rate data with the music that was selected
 * */
data class SavedPlaylist(
    val id: Long = System.currentTimeMillis(), // based on creation time
    val timestamp: Long = System.currentTimeMillis(), // when the playlist was created
    val averageBpm: Int? = 0, // stores the baseline calculated during calibration
    val calibrationReadings: List<Int>? = emptyList(), // stores the 30-second calibration readings
    val category: String? = "", // "Energetic" or "Calm"
    val tracks: List<TrackResult>? = emptyList()  // the list (seeds + recs) that make up the playlist
)

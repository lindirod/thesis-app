package com.example.thesis.data.model

/**
 * Each music track along with physiological and subjective metrics collected in the questionnaire
 * */

data class TrackResult(
    val name: String? = "",  // identify the song
    val artist: String? = "",  // identify the song
    val url: String? = "",  // link to the song on deezer
    val bpm: Int? = 0, // tempo of the track from the Deezer API
    val deezerId: String? = "", // used to fetch specific track details
    val category: String? = "", // Energetic or Calm, organizes the songs into the correct playlists
    val previewUrl: String? = "", // MP3 link to play the 30-second audio clip
    val durationSeconds: Int? = null, // The total duration of the track in seconds
    val fromSeed: String? = null, // If this is not null, then it is known that the track was recommended based on a specific Seed song
    val measuredBpm: Int? = null, // The average HR calculated over the duration of the song playback
    val duringTrackReadings: List<Int>? = emptyList(), // HR readings throughout the song playback
    val preTrackAvgBpm: Int? = null, // The 10-second resting HR captured immediately before the music starts
    val preTrackReadings: List<Int>? = emptyList(), // HR readings during the 10-second calibration
    val rating: Int? = null, // Questionnaire: Used for the vibe/mood score
    val arousalScale: Int? = null, // Questionnaire: 1 (Very calm) to 5 (Very Energetic)
    val familiarity: String? = null,  // Questionnaire: whether the user knew the song
    val clipEvaluation: String? = null, // Questionnaire: opinion on whether 30 seconds was enough time to evaluate
    val tags: List<String>? = emptyList(), // Fetched from Last.fm, displayed in the final Data Export Screen
    val recommendationMethod: String? = null // How this track was found: "Similar Tracks" or "Tags: [tag1, tag2]"
)

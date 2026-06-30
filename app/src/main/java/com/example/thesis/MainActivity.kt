package com.example.thesis

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.thesis.data.model.TrackResult
import com.example.thesis.data.model.SavedPlaylist
import com.example.thesis.ui.screens.*
import com.example.thesis.ui.theme.ThesisTheme
import com.example.thesis.data.ResearchDataManager
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private var rateReceived by mutableDoubleStateOf(0.0)
    private var isConnected  by mutableStateOf(false)
    private var isInternetConnected by mutableStateOf(true)
    private var isDarkMode by mutableStateOf(false)
    
    // Counter to force state updates in screens even if BPM value is the same
    private var heartRateSampleCounter by mutableIntStateOf(0)

    // Média estável calculada nos 30 segundos
    private var measuredAverageBpm by mutableDoubleStateOf(0.0)

    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val energeticSeeds = mutableStateListOf<TrackResult>()
    private val calmSeeds = mutableStateListOf<TrackResult>()
    
    // Lista de todas as playlists guardadas pelo utilizador
    private val savedPlaylists = mutableStateListOf<SavedPlaylist>()
    private lateinit var dataManager: ResearchDataManager

    // State for pending feedback
    private var pendingFeedbackTrack by mutableStateOf<TrackResult?>(null)
    private var pendingAverageHeartRate by mutableDoubleStateOf(0.0)
    private var isPendingSeed by mutableStateOf(false)
    private var pendingPreReadings by mutableStateOf<List<Int>>(emptyList())
    private var pendingDuringReadings by mutableStateOf<List<Int>>(emptyList())

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingFeedbackTrack?.let {
            outState.putString("pending_track", com.google.gson.Gson().toJson(it))
        }
        outState.putDouble("pending_avg_hr", pendingAverageHeartRate)
        outState.putBoolean("is_pending_seed", isPendingSeed)
    }

    private fun sendTheme(isDark: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                val themeBytes = if (isDark) "dark".toByteArray() else "light".toByteArray()
                for (node in nodes) {
                    Wearable.getMessageClient(this@MainActivity).sendMessage(node.id, "/theme", themeBytes).await()
                }
                Log.d("ThemeSync", "Theme message sent: ${if (isDark) "dark" else "light"}")
            } catch (e: Exception) {
                Log.e("ThemeSync", "Error sending theme: ${e.message}")
            }
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 3000L
        private const val MAX_SEEDS_PER_TYPE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dataManager = ResearchDataManager(this)

        // Monitor internet connection
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isInternetConnected = true
            }
            override fun onLost(network: android.net.Network) {
                isInternetConnected = false
            }
        })
        
        // Load session data to survive process death
        val (eSeeds, cSeeds, avgBpm) = dataManager.getCurrentSessionSeeds()
        energeticSeeds.addAll(eSeeds)
        calmSeeds.addAll(cSeeds)
        measuredAverageBpm = avgBpm
        savedPlaylists.addAll(dataManager.getCurrentSessionPlaylists())

        // Keep the screen on while the app is in the foreground
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore pending feedback if process was killed
        if (savedInstanceState != null) {
            val trackJson = savedInstanceState.getString("pending_track")
            if (trackJson != null) {
                pendingFeedbackTrack = com.google.gson.Gson().fromJson(trackJson, TrackResult::class.java)
            }
            pendingAverageHeartRate = savedInstanceState.getDouble("pending_avg_hr")
            isPendingSeed = savedInstanceState.getBoolean("is_pending_seed")
        }

        isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        setContent {
            ThesisTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                val onNavigateHome = {
                    // Sync the global lists from storage before going home
                    // to ensure it reflects the latest session data
                    val (eSeeds, cSeeds, avgBpm) = dataManager.getCurrentSessionSeeds()
                    energeticSeeds.clear()
                    energeticSeeds.addAll(eSeeds)
                    calmSeeds.clear()
                    calmSeeds.addAll(cSeeds)
                    measuredAverageBpm = avgBpm
                    
                    savedPlaylists.clear()
                    savedPlaylists.addAll(dataManager.getCurrentSessionPlaylists())
                    
                    navController.navigate("welcome") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }

                val handleTrackFinished = { track: TrackResult, avg: Double, rating: Int, arousal: Int, familiarity: String, clipEval: String, preReadings: List<Int>, duringReadings: List<Int> ->
                    val avgInt = avg.toInt()
                    
                    // 1. Update the original seed lists so the UI in RecommendationsScreen updates
                    energeticSeeds.forEachIndexed { index, s ->
                        if (s.name == track.name && s.artist == track.artist) {
                            energeticSeeds[index] = s.copy(
                                measuredBpm = avgInt,
                                preTrackAvgBpm = track.preTrackAvgBpm,
                                preTrackReadings = preReadings,
                                duringTrackReadings = duringReadings,
                                rating = rating,
                                arousalScale = arousal,
                                familiarity = familiarity,
                                clipEvaluation = clipEval
                            )
                        }
                    }
                    calmSeeds.forEachIndexed { index, s ->
                        if (s.name == track.name && s.artist == track.artist) {
                            calmSeeds[index] = s.copy(
                                measuredBpm = avgInt,
                                preTrackAvgBpm = track.preTrackAvgBpm,
                                preTrackReadings = preReadings,
                                duringTrackReadings = duringReadings,
                                rating = rating,
                                arousalScale = arousal,
                                familiarity = familiarity,
                                clipEvaluation = clipEval
                            )
                        }
                    }

                    // 1b. NEW: Explicitly update the recommendations if this is a recommended track
                    // This ensures the BPM appears immediately on the RecommendationsScreen UI
                    // (The RecomendationsScreen uses its own local state which is updated via side effects usually, 
                    // but we need to ensure the source of truth or the screen's local list is refreshed)
                    var sessionTimestamp = 0L
                    savedPlaylists.forEachIndexed { index, playlist ->
                        val updatedTracks = playlist.tracks.orEmpty().map {
                            if (it.name == track.name && it.artist == track.artist) {
                                sessionTimestamp = playlist.timestamp
                                it.copy(
                                    measuredBpm = avgInt,
                                    preTrackAvgBpm = track.preTrackAvgBpm,
                                    preTrackReadings = preReadings,
                                    duringTrackReadings = duringReadings,
                                    rating = rating,
                                    arousalScale = arousal,
                                    familiarity = familiarity,
                                    clipEvaluation = clipEval
                                )
                            } else it
                        }
                        savedPlaylists[index] = playlist.copy(tracks = updatedTracks)
                    }
                    
                    // Persist both seeds and playlists to ensure all copies of the track are updated
                    dataManager.updateCurrentSessionSeeds(energeticSeeds, calmSeeds)
                    dataManager.updateCurrentSessionPlaylists(savedPlaylists)
                    
                    savedPlaylists.clear()
                    savedPlaylists.addAll(dataManager.getCurrentSessionPlaylists())
                    Unit
                }

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            hasSeeds = energeticSeeds.isNotEmpty() || calmSeeds.isNotEmpty(),
                            hasPlaylists = savedPlaylists.isNotEmpty(),
                            onViewSeeds = { navController.navigate("seeds") },
                            onViewPlaylists = { navController.navigate("playlists") },
                            onResetSeeds = {
                                energeticSeeds.clear()
                                calmSeeds.clear()
                                savedPlaylists.clear()
                                dataManager.clearCurrentSession()
                                measuredAverageBpm = 0.0
                            },
                            onViewData = { navController.navigate("dataExport") },
                            onStartFlow = { 
                                // This is for a COMPLETELY NEW participant
                                energeticSeeds.clear()
                                calmSeeds.clear()
                                savedPlaylists.clear()
                                dataManager.clearCurrentSession()
                                measuredAverageBpm = 0.0
                                navController.navigate("userInfo") 
                            },
                            onResumeSession = {
                                // If they already have seeds but no playlists yet, go to seeds
                                // If they have playlists, go to recommendations or playlists
                                if (savedPlaylists.isNotEmpty()) {
                                    navController.navigate("recommendations")
                                } else {
                                    navController.navigate("seeds")
                                }
                            },
                            onExitApp = { finish() }
                        )
                    }

                    composable("dataExport") {
                        DataExportScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("userInfo") {
                        UserInfoScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onCreateSession = { age, gender ->
                                dataManager.createNewSession(age, gender)
                                navController.navigate("seedSelection")
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("seedSelection") {
                        SeedSelectionScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onNavigateHome = onNavigateHome,
                            onSelectCategory = { category ->
                                navController.navigate("search/$category")
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "search/{type}",
                        arguments = listOf(navArgument("type") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: "Energetic"
                        val currentList = if (type == "Energetic") energeticSeeds else calmSeeds
                        
                        SearchScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onNavigateHome = onNavigateHome,
                            categoryLabel = type,
                            selectedSeeds = currentList,
                            maxSeeds = MAX_SEEDS_PER_TYPE,
                            onNavigateBack = { 
                                dataManager.updateCurrentSessionSeeds(energeticSeeds, calmSeeds)
                                navController.popBackStack() 
                            },
                            onNavigateToSeeds = {
                                dataManager.updateCurrentSessionSeeds(energeticSeeds, calmSeeds)
                                if (type == "Energetic" && calmSeeds.isEmpty()) {
                                    navController.navigate("search/Calm")
                                } else if (type == "Calm" && energeticSeeds.isEmpty()) {
                                    navController.navigate("search/Energetic")
                                } else {
                                    navController.navigate("seeds")
                                }
                            }
                        )
                    }

                    composable("seeds") {
                        SeedsScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onNavigateHome = onNavigateHome,
                            energeticSeeds = energeticSeeds,
                            calmSeeds = calmSeeds,
                            maxSeedsPerType = MAX_SEEDS_PER_TYPE,
                            onRemoveEnergetic = { energeticSeeds.remove(it) },
                            onRemoveCalm = { calmSeeds.remove(it) },
                            onAddMore = { type -> navController.navigate("search/$type") },
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRecommendations = { 
                                if (measuredAverageBpm > 0) {
                                    navController.navigate("recommendations")
                                } else {
                                    navController.navigate("calibration")
                                }
                            }
                        )
                    }

                    composable("calibration") {
                        CalibrationScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onNavigateHome = onNavigateHome,
                            currentBpm = rateReceived,
                            heartRateSampleCounter = heartRateSampleCounter,
                            isConnected = isConnected,
                            onCalibrationComplete = { average, readings ->
                                measuredAverageBpm = average
                                dataManager.updateCurrentSessionSeeds(energeticSeeds, calmSeeds, average, readings)
                                navController.navigate("recommendations") {
                                    popUpTo("calibration") { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    composable("recommendations") {
                        RecommendationsScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onNavigateHome = onNavigateHome,
                            seeds = energeticSeeds + calmSeeds,
                            savedPlaylists = savedPlaylists,
                            userHeartRate = measuredAverageBpm,
                            liveHeartRate = rateReceived,
                            heartRateSampleCounter = heartRateSampleCounter,
                            isConnected = isConnected,
                            isInternetConnected = isInternetConnected,
                            onNavigateBack = { 
                                navController.popBackStack()
                            },
                            onSavePlaylist = { energeticRecs, calmRecs, energeticSeedsBatch, calmSeedsBatch ->
                                val now = System.currentTimeMillis()
                                val newPlaylists = mutableListOf<SavedPlaylist>()

                                // Save Energetic Playlist
                                if (energeticRecs.isNotEmpty()) {
                                    newPlaylists.add(
                                        SavedPlaylist(
                                            id = now,
                                            averageBpm = measuredAverageBpm.toInt(),
                                            calibrationReadings = emptyList(), // Not needed here, stored in Session
                                            category = "Energetic",
                                            tracks = energeticSeedsBatch + energeticRecs,
                                            timestamp = now
                                        )
                                    )
                                }
                                
                                // Save Calm Playlist
                                if (calmRecs.isNotEmpty()) {
                                    newPlaylists.add(
                                        SavedPlaylist(
                                            id = now + 1,
                                            averageBpm = measuredAverageBpm.toInt(),
                                            calibrationReadings = emptyList(), // Not needed here, stored in Session
                                            category = "Calm",
                                            tracks = calmSeedsBatch + calmRecs,
                                            timestamp = now
                                        )
                                    )
                                }
                                
                                if (newPlaylists.isNotEmpty()) {
                                    Log.d("StorageLog", "Auto-saving ${newPlaylists.size} playlists...")
                                    
                                    // 1. Update the local list for immediate UI feedback
                                    savedPlaylists.addAll(newPlaylists)
                                    
                                    // 2. Persist to storage for the current session
                                    // We filter by the timestamp we just created to identify this batch
                                    dataManager.updateCurrentSessionPlaylists(newPlaylists)
                                    
                                    // 3. Refresh from source of truth to ensure all IDs/states are aligned
                                    savedPlaylists.clear()
                                    savedPlaylists.addAll(dataManager.getCurrentSessionPlaylists())
                                }
                            },
                            onNavigateToFeedback = { track, avg, pre, during ->
                                Log.d("RecsLog", "Navigating to Feedback Screen for: ${track.name}")
                                pendingFeedbackTrack = track
                                pendingAverageHeartRate = avg
                                pendingPreReadings = pre
                                pendingDuringReadings = during
                                isPendingSeed = track.fromSeed == null
                                navController.navigate("feedback")
                            }
                        )
                    }

                    composable("playlists") {
                        PlaylistsScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { 
                                isDarkMode = !isDarkMode 
                                sendTheme(isDarkMode)
                            },
                            onNavigateHome = onNavigateHome,
                            playlists = savedPlaylists,
                            liveHeartRate = rateReceived,
                            heartRateSampleCounter = heartRateSampleCounter,
                            isConnected = isConnected,
                            isInternetConnected = isInternetConnected,
                            onNavigateBack = { navController.popBackStack() },
                            onTrackFinished = { track, avg, rating, arousal, familiarity, clipEval, pre, during ->
                                handleTrackFinished(track, avg, rating, arousal, familiarity, clipEval, pre, during)
                            },
                            onNavigateToFeedback = { track, avg, pre, during ->
                                Log.d("RecsLog", "Navigating to Feedback Screen for: ${track.name}")
                                pendingFeedbackTrack = track
                                pendingAverageHeartRate = avg
                                pendingPreReadings = pre
                                pendingDuringReadings = during
                                isPendingSeed = track.fromSeed == null
                                navController.navigate("feedback")
                            }
                        )
                    }

                    composable("feedback") {
                        pendingFeedbackTrack?.let { track ->
                            FeedbackScreen(
                                track = track,
                                isDarkMode = isDarkMode,
                                isSeed = isPendingSeed,
                                onToggleTheme = {
                                    isDarkMode = !isDarkMode
                                    sendTheme(isDarkMode)
                                },
                                onSubmit = { rating, arousal, familiarity, clipEval ->
                                    handleTrackFinished(
                                        track,
                                        pendingAverageHeartRate,
                                        rating,
                                        arousal,
                                        familiarity,
                                        clipEval,
                                        pendingPreReadings,
                                        pendingDuringReadings
                                    )
                                    navController.popBackStack()
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        sendTheme(isDarkMode)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        timeoutJob?.cancel()
        isConnected  = false
        rateReceived = 0.0
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/beat") {
            val heartRate = String(messageEvent.data).toDoubleOrNull() ?: 0.0

            if (heartRate > 0) {
                rateReceived = heartRate
                isConnected = true
                heartRateSampleCounter++
            }

            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                isConnected  = false
                rateReceived = 0.0
            }
        }
    }
}

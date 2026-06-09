package com.example.thesis.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.unregisterMeasureCallback
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.thesis.presentation.theme.ThesisTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    /* Creates a client of the API Health Services
    * Register sensors
    * Receive physiological data */
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }

    /* Keeps current value of BPM
    * When changed, UI updates automatically */
    private var currentRate by mutableDoubleStateOf(0.0)

    /* Theme state */
    private var isDarkMode by mutableStateOf(true) // Default to dark

    /*
    * false -> sensor stop
    * true -> sensor read
    * */
    private var readData by mutableStateOf(false)

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Thesis:SensorWakeLock")
        }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            toggleReading()
        } else {
            Log.e("Smartwatch", "Permission denied")
        }
    }

    /*
    * It's called everytime new data arrives from the sensors
    * */
    private val heartRateCallback = object : MeasureCallback {

        // Indicates if sensor is available or not
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            Log.d("Smartwatch", "Availability changed: $availability")
        }

        /* Starts when data arrives from sensor
        * BPM data
        * Last value
        * Updates
        * Sends to mobile */
        override fun onDataReceived(data: DataPointContainer) {
            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
            if (heartRateData.isNotEmpty()) {
                currentRate = heartRateData.last().value
                Log.d("Smartwatch", "BPM: $currentRate")
                sendRate(currentRate)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep the screen on while the app is in the foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            WearApp(
                bpm = currentRate,
                readData = readData,
                isDarkMode = isDarkMode,
                onButtonClick = {
                    checkPermissionsAndToggle()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        if (wakeLock.isHeld) wakeLock.release()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/theme") {
            val theme = String(messageEvent.data)
            isDarkMode = (theme == "dark")
            Log.d("ThemeSync", "Theme received: $theme")
        }
    }

    private fun checkPermissionsAndToggle() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            toggleReading()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }

    private fun toggleReading() {
        if (readData) {
            lifecycleScope.launch {
                try {
                    measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, heartRateCallback)
                    readData = false
                    currentRate = 0.0
                    if (wakeLock.isHeld) wakeLock.release()
                    Log.d("Smartwatch", "Sensor OFF")
                } catch (e: Exception) {
                    Log.e("Smartwatch", "Error stopping: ${e.message}")
                }
            }
        } else {
            lifecycleScope.launch {
                try {
                    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, heartRateCallback)
                    readData = true
                    wakeLock.acquire(30 * 60 * 1000L /*30 minutes max*/)
                    Log.d("Smartwatch", "Sensor ON")
                } catch (e: Exception) {
                    Log.e("Smartwatch", "Error starting: ${e.message}")
                }
            }
        }
    }

    private fun sendRate(bpm: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                val bpmBytes = bpm.toString().toByteArray()

                for (node in nodes) {
                    Wearable.getMessageClient(this@MainActivity).sendMessage(
                        node.id,
                        "/beat",
                        bpmBytes
                    ).await()
                }
            } catch (e: Exception) {
                Log.e("Smartwatch", "Error sending: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (readData) {
            lifecycleScope.launch {
                try {
                    measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, heartRateCallback)
                } catch (e: Exception) {
                    Log.e("Smartwatch", "Error removing callback: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun WearApp(bpm: Double, readData: Boolean, isDarkMode: Boolean, onButtonClick: () -> Unit) {
    ThesisTheme(darkTheme = isDarkMode) {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            ScreenScaffold(scrollState = listState) { contentPadding ->
                TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(text = "Thesis")
                        }
                    }
                    item {
                        Text(
                            text = if (readData && bpm == 0.0) "Reading..."
                            else if (!readData) "-- BPM"
                            else "$bpm BPM",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    item {
                        androidx.wear.compose.material3.Button(
                            onClick = onButtonClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (readData) "Stop reading" else "Initiate reading")
                        }
                    }
                }
            }
        }
    }
}

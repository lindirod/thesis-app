package com.example.thesis.data.sensor

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.unregisterMeasureCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class HeartRateSensor(context: Context) {
    private val measureClient = HealthServices.getClient(context).measureClient

    fun heartRateFlow(): Flow<Double> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                Log.d("HeartRateSensor", "Availability changed: $availability")
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRateData = data.getData(DataType.HEART_RATE_BPM)
                if (heartRateData.isNotEmpty()) {
                    val lastValue = heartRateData.last().value
                    trySend(lastValue)
                }
            }
        }

        launch {
            try {
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
                Log.d("HeartRateSensor", "Registered callback")
            } catch (e: Exception) {
                Log.e("HeartRateSensor", "Failed to register callback: ${e.message}")
                close(e)
            }
        }

        awaitClose {
            launch {
                try {
                    measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
                    Log.d("HeartRateSensor", "Unregistered callback")
                } catch (e: Exception) {
                    Log.e("HeartRateSensor", "Failed to unregister callback: ${e.message}")
                }
            }
        }
    }

    suspend fun start(callback: MeasureCallback) {
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
    }

    suspend fun stop(callback: MeasureCallback) {
        try {
            measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
        } catch (e: Exception) {
            Log.e("HeartRateSensor", "Error unregistering: ${e.message}")
        }
    }
}

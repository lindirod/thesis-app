package com.example.thesis.data.messaging

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MessageSender(private val context: Context) {
    suspend fun sendRate(bpm: Double) = withContext(Dispatchers.IO) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val bpmBytes = bpm.toString().toByteArray()

            for (node in nodes) {
                Wearable.getMessageClient(context).sendMessage(
                    node.id,
                    "/beat",
                    bpmBytes
                ).await()
                Log.d("MessageSender", "Sent $bpm to ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e("MessageSender", "Error sending message: ${e.message}")
        }
    }
}

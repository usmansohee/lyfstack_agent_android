package com.example.network

import android.os.Build
import android.util.Log
import com.example.data.model.SyncRange
import com.example.data.model.WsConnectionState
import com.example.data.model.WsHelloFrame
import com.example.data.model.WsIncomingFrame
import com.example.data.model.WsOutgoingFrame
import com.example.data.repository.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class LyfStackWebSocketClient(
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val scope: CoroutineScope
) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val incomingAdapter = moshi.adapter(WsIncomingFrame::class.java)
    private val outgoingAdapter = moshi.adapter(WsOutgoingFrame::class.java)
    private val helloAdapter = moshi.adapter(WsHelloFrame::class.java)

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var currentBackoffMs = 2000L

    private val _connectionState = MutableStateFlow(WsConnectionState.OFF)
    val connectionState: StateFlow<WsConnectionState> = _connectionState

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive WS
        .build()

    fun start() {
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                if (settings.wsEnabled) {
                    if (webSocket == null && _connectionState.value == WsConnectionState.OFF) {
                        connect(settings.wsUrl, settings.deviceId, settings.wsToken)
                    }
                } else {
                    disconnect()
                }
            }
        }
    }

    private fun connect(wsUrl: String, deviceId: String, token: String) {
        if (_connectionState.value == WsConnectionState.ONLINE || _connectionState.value == WsConnectionState.CONNECTING) {
            return
        }

        _connectionState.value = WsConnectionState.CONNECTING
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        val delimiter = if (wsUrl.contains("?")) "&" else "?"
        var fullUrl = "$wsUrl${delimiter}deviceId=$deviceId&platform=android"
        if (token.isNotBlank()) {
            fullUrl += "&token=$token"
        }

        val request = Request.Builder()
            .url(fullUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened")
                _connectionState.value = WsConnectionState.ONLINE
                currentBackoffMs = 2000L

                // Send HELLO Frame
                val hello = WsHelloFrame(
                    type = "HELLO",
                    deviceId = deviceId,
                    device = deviceName,
                    platform = "android",
                    agentVersion = "1.0.0"
                )
                webSocket.send(helloAdapter.toJson(hello))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket Message Received: $text")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.value = WsConnectionState.OFF
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}")
                this@LyfStackWebSocketClient.webSocket = null
                scheduleReconnect(wsUrl, deviceId, token)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@LyfStackWebSocketClient.webSocket = null
                scheduleReconnect(wsUrl, deviceId, token)
            }
        })
    }

    private fun handleMessage(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val frame = incomingAdapter.fromJson(text) ?: return@launch
                when (frame.type.uppercase()) {
                    "SYNC_NOW" -> {
                        val range = SyncRange.fromString(frame.range)
                        val result = syncManager.performSync(
                            range = range,
                            customFromIso = frame.from,
                            customToIso = frame.to
                        )
                        val reply = WsOutgoingFrame(
                            type = "SYNC_RESULT",
                            requestId = frame.requestId,
                            success = result.isSuccess,
                            sessionCount = result.getOrDefault(0),
                            message = result.exceptionOrNull()?.message
                        )
                        sendFrame(reply)
                    }
                    "PING" -> {
                        sendFrame(WsOutgoingFrame(type = "PONG"))
                    }
                    "PAUSE" -> {
                        settingsRepository.setTrackingPaused(true)
                        sendFrame(WsOutgoingFrame(type = "STATUS", isTrackingActive = false))
                    }
                    "RESUME" -> {
                        settingsRepository.setTrackingPaused(false)
                        sendFrame(WsOutgoingFrame(type = "STATUS", isTrackingActive = true))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling WS message", e)
            }
        }
    }

    private fun sendFrame(frame: WsOutgoingFrame) {
        val json = outgoingAdapter.toJson(frame)
        webSocket?.send(json)
    }

    private fun scheduleReconnect(wsUrl: String, deviceId: String, token: String) {
        _connectionState.value = WsConnectionState.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(currentBackoffMs)
            currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(60000L) // Max 1 minute
            val settings = settingsRepository.getSettings()
            if (settings.wsEnabled) {
                connect(wsUrl, deviceId, token)
            } else {
                _connectionState.value = WsConnectionState.OFF
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = WsConnectionState.OFF
    }

    companion object {
        private const val TAG = "LyfStackWS"
    }
}

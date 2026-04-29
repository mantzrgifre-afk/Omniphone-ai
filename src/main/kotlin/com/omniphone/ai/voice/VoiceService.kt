package com.omniphone.ai.voice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.omniphone.ai.controller.OmniController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * VoiceService: Background service for voice recording and command processing
 *
 * Features:
 * - Listens for wake word ("Hey Phone")
 * - Records voice commands
 * - Converts speech to text
 * - Sends commands to OmniController
 * - Provides text-to-speech feedback
 */
class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val WAKE_WORD = "Hey Phone"
    }

    private val voiceScope = CoroutineScope(Dispatchers.Main)
    private val omniController = OmniController.getInstance()
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Voice Service started")
        startListening()
        return START_STICKY
    }

    /**
     * Start listening for wake word
     */
    fun startListening() {
        if (isListening) return

        voiceScope.launch {
            try {
                isListening = true
                Log.d(TAG, "Started listening for wake word: $WAKE_WORD")
                // TODO: Integrate Whisper API or Vosk for actual speech recognition
                // For now this is a framework
            } catch (e: Exception) {
                Log.e(TAG, "Error starting listening", e)
                isListening = false
            }
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        isListening = false
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Process recorded audio data
     * @param audioData Raw audio bytes
     */
    private fun processAudio(audioData: ByteArray) {
        voiceScope.launch {
            try {\n                Log.d(TAG, \"Processing audio: ${audioData.size} bytes\")\n                // TODO: Convert speech to text using Whisper API\n                val transcript = \"\" // convertSpeechToText(audioData)\n\n                if (transcript.isNotEmpty()) {\n                    Log.d(TAG, \"Transcript: $transcript\")\n                    omniController.executeCommand(transcript) { result ->\n                        if (result.isSuccess) {\n                            Log.d(TAG, \"Command executed: $result\")\n                        } else {\n                            Log.e(TAG, \"Command failed: ${result.exceptionOrNull()}\")\n                        }\n                    }\n                }\n            } catch (e: Exception) {\n                Log.e(TAG, \"Error processing audio\", e)\n            }\n        }\n    }\n\n    override fun onDestroy() {\n        super.onDestroy()\n        stopListening()\n        Log.d(TAG, \"Voice Service destroyed\")\n    }\n}\n"

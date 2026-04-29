package com.omniphone.ai.api

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.suspendCancellableCoroutine

/**
 * AnthropicAPI: Integration with Anthropic Claude models
 *
 * Features:
 * - Claude 3 (Opus, Sonnet, Haiku) models
 * - Extended context windows (up to 200K tokens)
 * - Vision capabilities
 * - Streaming support
 */
class AnthropicAPI(private val apiKey: String = "") {

    companion object {
        private const val TAG = "AnthropicAPI"
        private const val BASE_URL = "https://api.anthropic.com/v1"
        private const val API_VERSION = "2024-01-15"
    }

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Send a prompt to Claude model
     * @param prompt User prompt
     * @param model Model name (default: claude-3-opus-20240229)
     * @param systemPrompt System instruction
     * @return Model response text
     */
    suspend fun sendRequest(
        prompt: String,
        model: String = "claude-3-opus-20240229",
        systemPrompt: String = ""
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "Sending request to Claude ($model): $prompt")

            val requestBody = mapOf(
                "model" to model,
                "max_tokens" to 2048,
                "system" to (if (systemPrompt.isNotEmpty()) systemPrompt else "You are a helpful AI assistant."),
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to prompt
                    )
                )
            )

            val jsonBody = gson.toJson(requestBody)
                .toRequestBody(okhttp3.MediaType.get("application/json"))

            val request = Request.Builder()
                .url("$BASE_URL/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "Response received from Claude")

                val responseData = gson.fromJson(body, Map::class.java)
                val content = responseData["content"] as? List<*>
                val text = ((content?.firstOrNull() as? Map<*, *>)?.get("text") as? String) ?: ""

                continuation.resume(text)
            } else {
                Log.e(TAG, "Claude API error: ${response.code()} ${response.message()}")
                continuation.resumeWith(Result.failure(Exception("Claude error: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request", e)
            continuation.resumeWith(Result.failure(e))
        }
    }

    /**
     * Send a streaming request to Claude
     */
    suspend fun sendStreamingRequest(
        prompt: String,
        model: String = "claude-3-opus-20240229",
        onChunk: (String) -> Unit
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            val requestBody = mapOf(
                "model" to model,
                "max_tokens" to 2048,
                "stream" to true,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to prompt
                    )
                )
            )

            val jsonBody = gson.toJson(requestBody)
                .toRequestBody(okhttp3.MediaType.get("application/json"))

            val request = Request.Builder()
                .url("$BASE_URL/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()
            val fullResponse = StringBuilder()

            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                reader.forEachLine { line ->
                    if (line.startsWith("data:")) {
                        val data = line.substring(5).trim()
                        if (data.isNotEmpty()) {
                            try {
                                val json = gson.fromJson(data, Map::class.java)
                                if (json["type"] == "content_block_delta") {
                                    val delta = json["delta"] as? Map<*, *>
                                    val content = delta?.get("text") as? String
                                    if (!content.isNullOrEmpty()) {\n                                        fullResponse.append(content)\n                                        onChunk(content)\n                                    }\n                                }\n                            } catch (e: Exception) {\n                                Log.w(TAG, \"Error parsing stream: $data\")\n                            }\n                        }\n                    }\n                }\n            }\n\n            continuation.resume(fullResponse.toString())\n        } catch (e: Exception) {\n            Log.e(TAG, \"Streaming error\", e)\n            continuation.resumeWith(Result.failure(e))\n        }\n    }\n\n    /**\n     * Use Claude Vision to analyze an image\n     * @param imageUrl URL of the image\n     * @param question Question about the image\n     */\n    suspend fun analyzeImage(\n        imageUrl: String,\n        question: String\n    ): String = suspendCancellableCoroutine { continuation ->\n        try {\n            val requestBody = mapOf(\n                \"model\" to \"claude-3-opus-20240229\",\n                \"max_tokens\" to 1024,\n                \"messages\" to listOf(\n                    mapOf(\n                        \"role\" to \"user\",\n                        \"content\" to listOf(\n                            mapOf(\n                                \"type\" to \"image\",\n                                \"source\" to mapOf(\n                                    \"type\" to \"url\",\n                                    \"url\" to imageUrl\n                                )\n                            ),\n                            mapOf(\n                                \"type\" to \"text\",\n                                \"text\" to question\n                            )\n                        )\n                    )\n                )\n            )\n\n            val jsonBody = gson.toJson(requestBody)\n                .toRequestBody(okhttp3.MediaType.get(\"application/json\"))\n\n            val request = Request.Builder()\n                .url(\"$BASE_URL/messages\")\n                .addHeader(\"x-api-key\", apiKey)\n                .addHeader(\"anthropic-version\", API_VERSION)\n                .post(jsonBody)\n                .build()\n\n            val response = client.newCall(request).execute()\n\n            if (response.isSuccessful) {\n                val body = response.body?.string() ?: \"\"\n                val responseData = gson.fromJson(body, Map::class.java)\n                val content = responseData[\"content\"] as? List<*>\n                val text = ((content?.firstOrNull() as? Map<*, *>)?.get(\"text\") as? String) ?: \"\"\n                continuation.resume(text)\n            } else {\n                continuation.resumeWith(Result.failure(Exception(\"Vision analysis failed\")))\n            }\n        } catch (e: Exception) {\n            continuation.resumeWith(Result.failure(e))\n        }\n    }\n}\n"

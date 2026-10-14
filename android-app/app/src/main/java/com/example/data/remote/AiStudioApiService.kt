package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String, val isQuotaExhausted: Boolean) : ApiResult<Nothing>()
}

class AiStudioApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Validates an API Key by making a minimal request to Google AI Studio.
     */
    suspend fun validateApiKey(apiKey: String): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ApiResult.Error("EMPTY_KEY", "API Key is empty.", false)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "Ping")
                    }))
                }))
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                when (response.code) {
                    200 -> ApiResult.Success(true)
                    429 -> ApiResult.Error(
                        "QUOTA_EXHAUSTED",
                        "HTTP 429: Rate limit or free quota exhausted. Please update your API key in Settings.",
                        true
                    )
                    400, 401, 403 -> ApiResult.Error(
                        "INVALID_KEY",
                        "HTTP ${response.code}: API Key is invalid or unauthorized. Please verify in Settings.",
                        false
                    )
                    else -> ApiResult.Error(
                        "HTTP_${response.code}",
                        "API returned status ${response.code}: $body",
                        false
                    )
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("NETWORK_ERROR", "Network failure: ${e.message}", false)
        } catch (e: Exception) {
            ApiResult.Error("GENERIC_ERROR", "Error: ${e.localizedMessage ?: "Unknown error"}", false)
        }
    }

    /**
     * Expands a user prompt into a high-production cinematic camera script using Gemini.
     */
    suspend fun enhancePrompt(
        apiKey: String,
        basePrompt: String,
        style: String,
        aspectRatio: String
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val effectiveKey = apiKey.ifBlank { "" }
        if (effectiveKey.isBlank()) {
            // Local fallback enhancement
            val enhanced = "Cinematic $style video shot, $aspectRatio aspect ratio. $basePrompt, volumetric lighting, photorealistic depth of field, 8k resolution, smooth motion."
            return@withContext ApiResult.Success(enhanced)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$effectiveKey"
            val systemInstruction = "You are a professional film director and AI video prompt engineer. Expand the user's brief prompt into a detailed, single-paragraph cinematic prompt optimized for AI video generation. Keep it under 60 words, specify camera motion, lighting, and texture."
            
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "$systemInstruction\nPrompt: $basePrompt\nStyle: $style\nAspect Ratio: $aspectRatio")
                    }))
                }))
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.code == 429) {
                    return@withContext ApiResult.Error("QUOTA_EXHAUSTED", "HTTP 429: API Quota exhausted for Google Video API.", true)
                }
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("HTTP_${response.code}", "API error: ${response.code}", false)
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (!text.isNullOrBlank()) {
                    ApiResult.Success(text.trim())
                } else {
                    ApiResult.Success("Cinematic $style: $basePrompt. 4K, 24fps film stock, anamorphic lens.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Prompt enhance network call failed, falling back to local enhancement", e)
            ApiResult.Success("Cinematic $style: $basePrompt. Masterpiece, high dynamic range, 4K film.")
        }
    }

    companion object {
        private const val TAG = "AiStudioApiService"
    }
}

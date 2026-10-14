package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.model.ApiKeyItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SecureStorageManager private constructor(context: Context) {

    private val prefs: SharedPreferences

    private val _apiKeyPoolFlow = MutableStateFlow<List<ApiKeyItem>>(emptyList())
    val apiKeyPoolFlow: StateFlow<List<ApiKeyItem>> = _apiKeyPoolFlow.asStateFlow()

    private val _voiceApiKeyFlow = MutableStateFlow("")
    val voiceApiKeyFlow: StateFlow<String> = _voiceApiKeyFlow.asStateFlow()

    private val _concurrencyLimitFlow = MutableStateFlow(5)
    val concurrencyLimitFlow: StateFlow<Int> = _concurrencyLimitFlow.asStateFlow()

    init {
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed, falling back to private prefs", e)
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }

        loadApiKeyPool()
        _voiceApiKeyFlow.value = prefs.getString(KEY_VOICE_API_KEY, "") ?: ""
        _concurrencyLimitFlow.value = prefs.getInt(KEY_CONCURRENCY_LIMIT, 5).coerceIn(1, 15)
    }

    private fun loadApiKeyPool() {
        val jsonString = prefs.getString(KEY_API_KEY_POOL, null)
        val list = mutableListOf<ApiKeyItem>()

        if (!jsonString.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ApiKeyItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            key = obj.optString("key", ""),
                            label = obj.optString("label", "Key #${i + 1}"),
                            isExhausted = obj.optBoolean("isExhausted", false),
                            lastUsedAt = obj.optLong("lastUsedAt", 0L),
                            exhaustedAt = obj.optLong("exhaustedAt", 0L)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse API key pool JSON", e)
            }
        }

        // Migrate legacy single key if pool is empty
        val legacyKey = prefs.getString(KEY_VIDEO_API_KEY, "") ?: ""
        if (list.isEmpty() && legacyKey.isNotBlank()) {
            list.add(ApiKeyItem(key = legacyKey, label = "Primary Key (Main)"))
        }

        _apiKeyPoolFlow.value = list
    }

    private fun saveApiKeyPool(list: List<ApiKeyItem>) {
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("key", item.key)
                    put("label", item.label)
                    put("isExhausted", item.isExhausted)
                    put("lastUsedAt", item.lastUsedAt)
                    put("exhaustedAt", item.exhaustedAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_API_KEY_POOL, array.toString()).apply()
            _apiKeyPoolFlow.value = list

            // Also update legacy key
            val active = list.firstOrNull { !it.isExhausted } ?: list.firstOrNull()
            prefs.edit().putString(KEY_VIDEO_API_KEY, active?.key ?: "").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save API key pool", e)
        }
    }

    fun getApiKeyPool(): List<ApiKeyItem> = _apiKeyPoolFlow.value

    fun addApiKeyToPool(key: String, label: String = "") {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return

        val current = _apiKeyPoolFlow.value.toMutableList()
        val index = current.indexOfFirst { it.key == trimmed }
        val finalLabel = if (label.isNotBlank()) label else "API Key #${current.size + 1}"

        if (index >= 0) {
            // Update existing
            current[index] = current[index].copy(label = finalLabel, isExhausted = false)
        } else {
            // Add new
            current.add(ApiKeyItem(key = trimmed, label = finalLabel, isExhausted = false))
        }

        saveApiKeyPool(current)
    }

    fun removeApiKeyFromPool(id: String) {
        val current = _apiKeyPoolFlow.value.filter { it.id != id }
        saveApiKeyPool(current)
    }

    fun markKeyExhausted(key: String) {
        val current = _apiKeyPoolFlow.value.map {
            if (it.key == key) it.copy(isExhausted = true, exhaustedAt = System.currentTimeMillis()) else it
        }
        saveApiKeyPool(current)
    }

    fun resetExhaustedKeys() {
        val current = _apiKeyPoolFlow.value.map {
            it.copy(isExhausted = false, exhaustedAt = 0L)
        }
        saveApiKeyPool(current)
    }

    /**
     * Returns the next available non-exhausted API key from the pool.
     */
    fun getNextAvailableKey(): ApiKeyItem? {
        val pool = _apiKeyPoolFlow.value
        return pool.firstOrNull { !it.isExhausted && it.key.isNotBlank() }
    }

    fun getVideoApiKey(): String {
        return getNextAvailableKey()?.key ?: _apiKeyPoolFlow.value.firstOrNull()?.key ?: prefs.getString(KEY_VIDEO_API_KEY, "") ?: ""
    }

    fun setVideoApiKey(apiKey: String) {
        addApiKeyToPool(apiKey, "Primary Key (Main)")
    }

    fun getVoiceApiKey(): String {
        return prefs.getString(KEY_VOICE_API_KEY, "") ?: ""
    }

    fun setVoiceApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(KEY_VOICE_API_KEY, trimmed).apply()
        _voiceApiKeyFlow.value = trimmed
    }

    fun getConcurrencyLimit(): Int {
        return prefs.getInt(KEY_CONCURRENCY_LIMIT, 5).coerceIn(1, 15)
    }

    fun setConcurrencyLimit(limit: Int) {
        val clamped = limit.coerceIn(1, 15)
        prefs.edit().putInt(KEY_CONCURRENCY_LIMIT, clamped).apply()
        _concurrencyLimitFlow.value = clamped
    }

    fun getActiveProjectId(): String? {
        return prefs.getString(KEY_ACTIVE_PROJECT_ID, null)
    }

    fun setActiveProjectId(projectId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROJECT_ID, projectId).apply()
    }

    fun isAutoDownloadEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_DOWNLOAD, true)
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD, enabled).apply()
    }

    companion object {
        private const val TAG = "SecureStorageManager"
        private const val PREFS_FILE_NAME = "ai_studio_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "ai_studio_prefs_fallback"

        private const val KEY_API_KEY_POOL = "key_api_key_pool"
        private const val KEY_VIDEO_API_KEY = "key_video_api"
        private const val KEY_VOICE_API_KEY = "key_voice_api"
        private const val KEY_CONCURRENCY_LIMIT = "key_concurrency_limit"
        private const val KEY_ACTIVE_PROJECT_ID = "key_active_project_id"
        private const val KEY_AUTO_DOWNLOAD = "key_auto_download"

        @Volatile
        private var INSTANCE: SecureStorageManager? = null

        fun getInstance(context: Context): SecureStorageManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SecureStorageManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    fun play(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file does not exist: $filePath")
            return
        }

        if (_currentPlayingPath.value == filePath && mediaPlayer != null) {
            if (!_isPlaying.value) {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTracker()
            }
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
            }

            mediaPlayer = player
            _currentPlayingPath.value = filePath
            _durationMs.value = player.duration.coerceAtLeast(0)
            _currentPositionMs.value = 0

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0
                progressJob?.cancel()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                stop()
                true
            }

            player.start()
            _isPlaying.value = true
            startProgressTracker()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            }
        }
    }

    fun togglePlayPause(filePath: String) {
        if (_currentPlayingPath.value == filePath && _isPlaying.value) {
            pause()
        } else {
            play(filePath)
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing media player", e)
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (e: Exception) {
                        // ignore state race
                    }
                }
                delay(100)
            }
        }
    }

    companion object {
        private const val TAG = "AudioPlayerManager"
    }
}

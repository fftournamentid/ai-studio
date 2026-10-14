package com.example.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

object WaveformSynthesizer {

    /**
     * Synthesizes audio samples based on speech script, voice style, pitch, and speed,
     * and writes a standard uncompressed PCM 16-bit WAV file that Android's MediaPlayer can play.
     */
    suspend fun synthesizeSpeechToFile(
        context: Context,
        script: String,
        voiceName: String,
        pitch: Float,
        speed: Float,
        outputFile: File
    ): Long = withContext(Dispatchers.IO) {
        val sampleRate = 22050
        val baseDurationSeconds = (script.split("\\s+".toRegex()).size * 0.45f + 1.5f).coerceIn(2.0f, 60.0f)
        val adjustedDuration = baseDurationSeconds / speed.coerceIn(0.5f, 2.0f)
        val totalSamples = (sampleRate * adjustedDuration).toInt()

        // Base frequency determined by voice type + pitch adjustment
        val baseFreq = when {
            voiceName.contains("Deep", ignoreCase = true) || voiceName.contains("Charon", ignoreCase = true) -> 110.0
            voiceName.contains("Female", ignoreCase = true) || voiceName.contains("Kore", ignoreCase = true) || voiceName.contains("Aoede", ignoreCase = true) -> 240.0
            voiceName.contains("Intense", ignoreCase = true) || voiceName.contains("Fenrir", ignoreCase = true) -> 135.0
            else -> 165.0 // Puck / Studio Male
        } * (1.0 + (pitch * 0.05).coerceIn(-0.4, 0.6))

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            // Write 44-byte WAV header placeholder
            fos.write(ByteArray(44))

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize * 2) // 16-bit = 2 bytes per sample
            var sampleIndex = 0

            while (sampleIndex < totalSamples) {
                val samplesToWrite = minOf(bufferSize, totalSamples - sampleIndex)
                var byteIndex = 0

                for (i in 0 until samplesToWrite) {
                    val currentSample = sampleIndex + i
                    val t = currentSample.toDouble() / sampleRate
                    
                    // Modulate pitch slightly over speech phoneme syllables (vowel cadence)
                    val syllableMod = sin(2 * PI * 3.5 * t) * 0.15
                    val freq = baseFreq * (1.0 + syllableMod)

                    // Formant harmonic synthesis (Fundamental + Formants for vowel-like voice timbre)
                    val fundamental = sin(2 * PI * freq * t)
                    val formant1 = 0.5 * sin(2 * PI * (freq * 2.2) * t)
                    val formant2 = 0.25 * sin(2 * PI * (freq * 3.5) * t)
                    val envelope = (sin(PI * (currentSample.toDouble() / totalSamples))).coerceIn(0.0, 1.0)
                    
                    // Add slight cadence pauses between sentence clauses
                    val sentenceMod = (sin(2 * PI * 0.8 * t) + 1.2) * 0.5

                    val sampleValue = ((fundamental + formant1 + formant2) * envelope * sentenceMod * 0.45)
                    val sampleClamped = (sampleValue * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    // Little-endian 16-bit
                    buffer[byteIndex++] = (sampleClamped.toInt() and 0xFF).toByte()
                    buffer[byteIndex++] = ((sampleClamped.toInt() shr 8) and 0xFF).toByte()
                }

                fos.write(buffer, 0, samplesToWrite * 2)
                sampleIndex += samplesToWrite
            }
        }

        // Fill in actual WAV header lengths
        val audioDataLength = totalSamples * 2
        val totalDataLength = audioDataLength + 36
        RandomAccessFile(outputFile, "rw").use { raf ->
            writeWavHeader(raf, totalDataLength, audioDataLength, sampleRate, 1, 16)
        }

        outputFile.length()
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        totalDataLen: Int,
        audioDataLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.write(intToByteArray(totalDataLen))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.write(intToByteArray(16)) // Subchunk1Size for PCM
        raf.write(shortToByteArray(1)) // AudioFormat 1 = PCM
        raf.write(shortToByteArray(channels.toShort()))
        raf.write(intToByteArray(sampleRate))
        raf.write(intToByteArray(byteRate))
        raf.write(shortToByteArray((channels * bitsPerSample / 8).toShort())) // BlockAlign
        raf.write(shortToByteArray(bitsPerSample.toShort()))
        raf.writeBytes("data")
        raf.write(intToByteArray(audioDataLen))
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}

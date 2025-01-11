package com.example.qurannexus.features.recitation.audio

import android.content.Context
import android.media.MediaPlayer
import com.amazonaws.services.s3.AmazonS3Client

class AudioRecitationManager(
    private val context: Context,
    private val s3Client: AmazonS3Client,
    private val bucketName: String
) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioFiles = mutableListOf<String>()
    private var currentIndex = 0

    fun prepareAudioRecitation(ayahs: List<Ayah>) {
        audioFiles.clear()
        currentIndex = 0

        ayahs.forEach { ayah ->
            // Handle bismillah and audhubillah for first ayah
            if (ayah.ayahIndex == 1) {
                when (getUserAudioPreference()) {
                    AudioReciter.ALAFASY -> {
                        audioFiles.add("quran-audio/Alafasy/mp3/audhubillah.mp3")
                        if (ayah.hasBismillah) {
                            audioFiles.add("quran-audio/Alafasy/mp3/bismillah.mp3")
                        }
                    }
                    AudioReciter.ABDUL_BASIT -> {
                        audioFiles.add("quran-audio/AbdulBaset/Murattal/mp3/001000.mp3")
                        if (ayah.hasBismillah) {
                            audioFiles.add("quran-audio/AbdulBaset/Murattal/mp3/bismillah.mp3")
                        }
                    }
                }
            }

            // Add the main ayah recitation
            val audioUrl = getAudioUrlForAyah(ayah)
            audioFiles.add(audioUrl)
        }
    }

    fun startPlayback() {
        if (audioFiles.isEmpty()) return
        playNextFile()
    }

    private fun playNextFile() {
        if (currentIndex >= audioFiles.size) {
            // Playback completed
            return
        }

        val audioKey = audioFiles[currentIndex]

        try {
            // Get pre-signed URL for the audio file
            val preSignedUrl = getPreSignedUrl(audioKey)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(preSignedUrl)
                setOnCompletionListener {
                    it.release()
                    currentIndex++
                    playNextFile()
                }
                setOnPreparedListener { start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioRecitation", "Error playing file: ${e.message}")
            currentIndex++
            playNextFile()
        }
    }

    private fun getPreSignedUrl(key: String): String {
        val expiration = Date(System.currentTimeMillis() + 3600000) // 1 hour
        val generatePresignedUrlRequest = GeneratePresignedUrlRequest(bucketName, key)
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration)

        return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        currentIndex = 0
    }

    private fun getUserAudioPreference(): AudioReciter {
        // Implement based on your user preferences
        return AudioReciter.ALAFASY
    }
}
enum class AudioReciter {
    ALAFASY,
    ABDUL_BASIT
}
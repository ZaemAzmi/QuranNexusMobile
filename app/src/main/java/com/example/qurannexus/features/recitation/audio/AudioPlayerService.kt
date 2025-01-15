package com.example.qurannexus.features.recitation.audio

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioPlayerService : Service() {
    private var player: ExoPlayer? = null
    private val binder = AudioPlayerBinder()
    private var currentAyahInfo: String = ""
    private var completionListener: (() -> Unit)? = null

    inner class AudioPlayerBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
//                    Log.d("AudioDebug", "Playback state changed to: $state")
                    when (state) {
                        Player.STATE_ENDED -> {
//                            Log.d("AudioDebug", "AudioService: Playback ended, triggering completion")
                            completionListener?.invoke()
                        }
                    }
                    updateNotification()
                }
            })
        }
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        completionListener = listener
    }

    fun playAyah(audioUrl: String, ayahInfo: String) {
        val fullUrl = "${AWS_URL}/${audioUrl}"
//        Log.d("AudioDebug", "AudioService: Playing ayah from URL: $fullUrl")
        currentAyahInfo = ayahInfo

        player?.apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(fullUrl)))
            prepare()
            play()
        }
        updateNotification()
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
            updateNotification()
        }
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    fun stopPlayback() {
        player?.apply {
            stop()
            clearMediaItems()
        }
        stopForeground(true)
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackParameters(PlaybackParameters(speed))
    }

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    fun getDuration(): Long = player?.duration ?: 0

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quran recitation playback controls"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val playPauseIcon = if (player?.isPlaying == true) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Quran Recitation")
                .setContentText(currentAyahInfo)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(playPauseIcon,
                    if (player?.isPlaying == true) "Pause" else "Play",
                    createActionIntent(ACTION_PLAY_PAUSE))
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, AudioPlayerService::class.java).setAction(action)
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "quran_audio_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PLAY_PAUSE = "action_play_pause"
        private const val AWS_URL = "https://quran-nexus-bucket.s3.ap-southeast-1.amazonaws.com/quran-audio"
    }
}
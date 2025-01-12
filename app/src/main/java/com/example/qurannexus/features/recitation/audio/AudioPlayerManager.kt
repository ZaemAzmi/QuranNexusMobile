package com.example.qurannexus.features.recitation.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.recitation.audio.models.AudioRecitationResponse
import com.example.qurannexus.features.recitation.models.PageAyah
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AudioPlayerManager(
    private val context: Context,
    private val quranApi: QuranApi
) {
    private data class QueuedAudio(
        val index: Int,
        val audioUrl: String,
        val ayahInfo: String,
        var isReady: Boolean = false
    )

    private enum class PlaybackMode {
        SINGLE_AYAH, PAGE_SEQUENCE
    }

    private var audioService: AudioPlayerService? = null
    private val audioQueue = mutableListOf<QueuedAudio>()
    private var currentQueueIndex = 0
    private var isPlaybackActive = false
    private var shouldContinuePlayback = false
    private var currentMode = PlaybackMode.SINGLE_AYAH

    // LiveData for UI updates
    private val _isPlaying = MutableLiveData<Boolean>().apply { value = false }
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPosition = MutableLiveData<Int>().apply { value = 0 }
    val currentPosition: LiveData<Int> = _currentPosition

    private val _duration = MutableLiveData<Int>().apply { value = 0 }
    val duration: LiveData<Int> = _duration

    private val _isLoadingDuration = MutableLiveData<Boolean>().apply { value = false }
    val isLoadingDuration: LiveData<Boolean> = _isLoadingDuration

    private val _currentTimeText = MutableLiveData<String>().apply { value = "-/-" }
    val currentTimeText: LiveData<String> = _currentTimeText

    private val _shouldShowPlayer = MutableLiveData<Boolean>().apply { value = false }
    val shouldShowPlayer: LiveData<Boolean> = _shouldShowPlayer

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressHandler.postDelayed(this, 1000)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerBinder
            audioService = binder.getService()
            Log.d("AudioDebug", "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            Log.d("AudioDebug", "Service disconnected")
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(context, AudioPlayerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d("AudioDebug", "Binding service")
    }

    fun playPageAyahs(ayahs: List<PageAyah>) {
        Log.d("AudioDebug", "Starting playPageAyahs with ${ayahs.size} ayahs")
        stopPlayback()
        currentMode = PlaybackMode.PAGE_SEQUENCE
        _isLoadingDuration.postValue(true)
        _shouldShowPlayer.postValue(true)

        audioQueue.clear()
        currentQueueIndex = 0

        // Add Audhubillah and Bismillah if needed
        if (shouldAddBismillah(ayahs.firstOrNull())) {
            addPrefixAudios()
        }

        var completedCalls = 0
        val totalCalls = ayahs.size

        ayahs.forEachIndexed { index, ayah ->
            quranApi.getAudioRecitation(ayah.ayahKey).enqueue(object :
                Callback<AudioRecitationResponse> {
                override fun onResponse(
                    call: Call<AudioRecitationResponse>,
                    response: Response<AudioRecitationResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        response.body()!!.data.find { it.audioInfoId == getSelectedReciterId() }?.let {
                            val queueIndex = audioQueue.size
                            audioQueue.add(QueuedAudio(
                                index = queueIndex,
                                audioUrl = it.audioUrl,
                                ayahInfo = "Surah ${ayah.surahId}:${ayah.ayahIndex}",
                                isReady = true
                            ))
                        }
                    }

                    completedCalls++
                    if (completedCalls == totalCalls) {
                        _isLoadingDuration.postValue(false)
                        audioQueue.sortBy { it.index }
                        Log.d("AudioDebug", "Queue prepared with ${audioQueue.size} items")
                    }
                }

                override fun onFailure(call: Call<AudioRecitationResponse>, t: Throwable) {
                    Log.e("AudioDebug", "Failed to fetch audio for ayah: ${ayah.ayahKey}", t)
                    completedCalls++
                    if (completedCalls == totalCalls) {
                        _isLoadingDuration.postValue(false)
                        audioQueue.sortBy { it.index }
                    }
                }
            })
        }
    }

    fun playAyah(ayahKey: String) {
        stopPlayback()
        currentMode = PlaybackMode.SINGLE_AYAH
        _shouldShowPlayer.postValue(true)

        quranApi.getAudioRecitation(ayahKey).enqueue(object : Callback<AudioRecitationResponse> {
            override fun onResponse(
                call: Call<AudioRecitationResponse>,
                response: Response<AudioRecitationResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val audioRecitation = response.body()!!.data.find {
                        it.audioInfoId == getSelectedReciterId()
                    } ?: response.body()!!.data.firstOrNull()

                    audioRecitation?.let {
                        audioService?.playAyah(it.audioUrl, "Surah ${it.surahId}:${it.ayahIndex}")
                        startProgressUpdates()
                        _isPlaying.postValue(true)
                    }
                }
            }

            override fun onFailure(call: Call<AudioRecitationResponse>, t: Throwable) {
                Log.e("AudioDebug", "Failed to fetch audio for ayah: $ayahKey", t)
            }
        })
    }

    private fun playCurrentInQueue() {
        if (!shouldContinuePlayback || currentQueueIndex >= audioQueue.size) {
            isPlaybackActive = false
            _isPlaying.postValue(false)
            return
        }

        val currentAudio = audioQueue[currentQueueIndex]
        Log.d("AudioDebug", "Playing queue item ${currentQueueIndex + 1}/${audioQueue.size}: ${currentAudio.audioUrl}")

        audioService?.apply {
            setOnCompletionListener {
                Log.d("AudioDebug", "Completed playing index: $currentQueueIndex")
                if (shouldContinuePlayback) {
                    currentQueueIndex++
                    playCurrentInQueue()
                }
            }
            playAyah(currentAudio.audioUrl, currentAudio.ayahInfo)
        }

        _isPlaying.postValue(true)
        startProgressUpdates()
    }

    private fun addPrefixAudios() {
        val reciterId = getSelectedReciterId()
        val (audhubillahUrl, bismillahUrl) = when (reciterId) {
            "1" -> Pair("Alafasy/mp3/audhubillah.mp3", "Alafasy/mp3/bismillah.mp3")
            "2" -> Pair("AbdulBaset/Murattal/mp3/001000.mp3", "AbdulBaset/Murattal/mp3/bismillah.mp3")
            else -> return
        }

        audioQueue.add(QueuedAudio(
            index = 0,
            audioUrl = audhubillahUrl,
            ayahInfo = "Audhubillah",
            isReady = true
        ))
        audioQueue.add(QueuedAudio(
            index = 1,
            audioUrl = bismillahUrl,
            ayahInfo = "Bismillah",
            isReady = true
        ))
    }

    fun startPlayback() {
        if (!isPlaybackActive && audioQueue.isNotEmpty()) {
            isPlaybackActive = true
            shouldContinuePlayback = true
            currentQueueIndex = 0
            playCurrentInQueue()
        }
    }

    fun togglePlayPause() {
        when (currentMode) {
            PlaybackMode.PAGE_SEQUENCE -> {
                if (isPlaybackActive) {
                    shouldContinuePlayback = !shouldContinuePlayback
                    if (shouldContinuePlayback) {
                        playCurrentInQueue()
                    } else {
                        audioService?.togglePlayPause()
                    }
                    _isPlaying.postValue(shouldContinuePlayback)
                } else {
                    startPlayback()
                }
            }
            PlaybackMode.SINGLE_AYAH -> {
                audioService?.togglePlayPause()
                _isPlaying.postValue(audioService?.isPlaying() == true)
            }
        }
    }

    fun stopPlayback() {
        isPlaybackActive = false
        shouldContinuePlayback = false
        stopProgressUpdates()
        audioService?.stopPlayback()
        _isPlaying.postValue(false)
        currentQueueIndex = 0
    }

    fun seekTo(position: Int) {
        audioService?.seekTo(position.toLong())
    }

    fun setPlaybackSpeed(speed: Float) {
        audioService?.setPlaybackSpeed(speed)
    }

    private fun updateProgress() {
        audioService?.let { service ->
            val currentPos = service.getCurrentPosition()
            _currentPosition.postValue(currentPos.toInt())
            _currentTimeText.postValue(formatTime(currentPos))

            val duration = service.getDuration()
            if (duration > 0) {
                _duration.postValue(duration.toInt())
            }
        }
    }

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
    }

    private fun formatTime(millis: Long): String {
        if (millis < 0) return "-/-"
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60))
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun shouldAddBismillah(firstAyah: PageAyah?): Boolean {
        return firstAyah?.ayahIndex == "1"
    }

    private fun getSelectedReciterId(): String {
        return context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("selected_reciter", "1") ?: "1"
    }

    fun stopAndHidePlayer() {
        stopPlayback()
        _shouldShowPlayer.postValue(false)
    }

    fun handlePageChange(newPage: Int) {
        stopAndHidePlayer()
    }

    fun release() {
        stopProgressUpdates()
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            Log.e("AudioDebug", "Error unbinding service", e)
        }
        audioService = null
    }
}
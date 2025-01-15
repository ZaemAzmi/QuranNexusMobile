package com.example.qurannexus.features.recitation.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
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
import java.util.Collections

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

    private var totalDuration = 0L
    private var currentItemStartTime = 0L

    private val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "selected_reciter") {
            // If audio is playing, restart with new reciter
            val wasPlaying = isPlaying.value == true
            stopPlayback()
            if (wasPlaying && currentPageAyahs != null) {
                playPageAyahs(currentPageAyahs!!)
                startPlayback()
            }
        }
    }
    private var currentPageAyahs: List<PageAyah>? = null
    init {
        bindService()
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
    }
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressHandler.postDelayed(this, 1000)
        }
    }

    private val serviceConnection by lazy {
        object : ServiceConnection {
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
    }

    init {
        // Start and bind the service
        val intent = Intent(context, AudioPlayerService::class.java)
        context.startService(intent) // First start the service
        bindService() // Then bind to it
    }

    private fun bindService() {
        try {
            val intent = Intent(context, AudioPlayerService::class.java)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("AudioDebug", "Binding service, success: $bound")
        } catch (e: Exception) {
            Log.e("AudioDebug", "Error binding service", e)
        }
    }


    fun playPageAyahs(ayahs: List<PageAyah>) {
//        Log.d("AudioDebug", "Starting playPageAyahs with ${ayahs.size} ayahs")
        currentPageAyahs = ayahs
        stopPlayback()
        currentMode = PlaybackMode.PAGE_SEQUENCE
        _isLoadingDuration.postValue(true)
        _shouldShowPlayer.postValue(true)

        audioQueue.clear()
        currentQueueIndex = 0

        totalDuration = 0L
        currentItemStartTime = 0L
        val estimatedAyahDuration = 15000L
        totalDuration = (ayahs.size * estimatedAyahDuration)
        _duration.postValue(totalDuration.toInt())

        // Add Audhubillah and Bismillah if needed
        if (shouldAddBismillah(ayahs.firstOrNull())) {
            addPrefixAudios()
        }

        // Create a synchronized map to store responses in order
        val audioResponses = Collections.synchronizedMap(LinkedHashMap<String, QueuedAudio>())
        val startingIndex = audioQueue.size // Account for prefix audios if added
        var completedCalls = 0

        // First, create placeholders in the correct order
        ayahs.forEachIndexed { index, ayah ->
            audioResponses[ayah.ayahKey] = QueuedAudio(
                index = startingIndex + index,
                audioUrl = "", // Will be filled when API responds
                ayahInfo = "Surah ${ayah.surahId}:${ayah.ayahIndex}",
                isReady = false
            )
        }

        // Now make API calls
        ayahs.forEachIndexed { index, ayah ->
            quranApi.getAudioRecitation(ayah.ayahKey).enqueue(object : Callback<AudioRecitationResponse> {
                override fun onResponse(
                    call: Call<AudioRecitationResponse>,
                    response: Response<AudioRecitationResponse>
                ) {
                    synchronized(audioResponses) {
                        if (response.isSuccessful && response.body() != null) {
                            response.body()!!.data.find { it.audioInfoId == getSelectedReciterId() }?.let {
                                // Update the placeholder with actual audio URL
                                audioResponses[ayah.ayahKey] = QueuedAudio(
                                    index = startingIndex + index,
                                    audioUrl = it.audioUrl,
                                    ayahInfo = "Surah ${ayah.surahId}:${ayah.ayahIndex}",
                                    isReady = true
                                )
//                                Log.d("AudioDebug", "Received audio for ${ayah.ayahKey} at index $index")
                            }
                        }

                        completedCalls++
                        if (completedCalls == ayahs.size) {
                            // Add to queue in original order
                            ayahs.forEachIndexed { i, ayah ->
                                audioResponses[ayah.ayahKey]?.let { audio ->
                                    if (audio.isReady) {
                                        audioQueue.add(audio)
//                                        Log.d("AudioDebug", "Adding to queue: ${audio.ayahInfo} at position ${audioQueue.size - 1}")
                                    }
                                }
                            }
                            _isLoadingDuration.postValue(false)
//                            Log.d("AudioDebug", "Queue prepared with ${audioQueue.size} items in sequence")
                        }
                    }
                }

                override fun onFailure(call: Call<AudioRecitationResponse>, t: Throwable) {
//                    Log.e("AudioDebug", "Failed to fetch audio for ayah: ${ayah.ayahKey}", t)
                    synchronized(audioResponses) {
                        completedCalls++
                        if (completedCalls == ayahs.size) {
                            _isLoadingDuration.postValue(false)
                        }
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
//        Log.d("AudioDebug", "Playing queue item ${currentQueueIndex + 1}/${audioQueue.size}: ${currentAudio.audioUrl} (${currentAudio.ayahInfo})")

        audioService?.apply {
            setOnCompletionListener {
//                Log.d("AudioDebug", "Completed playing: ${currentAudio.ayahInfo}")
                if (shouldContinuePlayback) {
                    currentQueueIndex++
                    currentItemStartTime += getDuration()
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
        // Calculate which item this position corresponds to
        var accumulatedTime = 0L
        var targetIndex = 0

        for (i in 0 until audioQueue.size) {
            val itemDuration = if (i == currentQueueIndex) {
                audioService?.getDuration() ?: 15000L
            } else {
                15000L // estimated duration for other items
            }

            if (accumulatedTime + itemDuration > position) {
                targetIndex = i
                break
            }
            accumulatedTime += itemDuration
        }

        // Switch to target item and seek within it
        currentQueueIndex = targetIndex
        currentItemStartTime = accumulatedTime
        val seekPositionInItem = position - accumulatedTime
        audioService?.seekTo(seekPositionInItem)
        playCurrentInQueue()
    }

    fun setPlaybackSpeed(speed: Float) {
        audioService?.setPlaybackSpeed(speed)
    }

    private fun updateProgress() {
        audioService?.let { service ->
            val currentProgress = currentItemStartTime + service.getCurrentPosition()
            _currentPosition.postValue(currentProgress.toInt())
            _currentTimeText.postValue(formatTime(currentProgress))

            // Update seekbar max if needed
            if (service.getDuration() > 0 && currentQueueIndex == 0) {
                val newEstimate = (audioQueue.size * 15000L) // Estimate total duration
                if (newEstimate > totalDuration) {
                    totalDuration = newEstimate
                    _duration.postValue(totalDuration.toInt())
                }
            }
        }
    }
    private fun formatTime(millis: Long): String {
        if (millis < 0) return "-/-"
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60))
        return String.format("%d:%02d", minutes, seconds)
    }
    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
    }



    private fun shouldAddBismillah(firstAyah: PageAyah?): Boolean {
        // Special case for Al-Fatihah (Surah 1) - don't add Bismillah
        if (firstAyah != null && firstAyah.surahId == "1") {
            return false;
        }
        // For other surahs, add Bismillah if it's the first ayah
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
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        try {
            // Check if service is bound before unbinding
            if (audioService != null) {
                context.unbindService(serviceConnection)
                audioService = null
            }
        } catch (e: Exception) {
            Log.e("AudioDebug", "Error unbinding service", e)
        }
    }
}
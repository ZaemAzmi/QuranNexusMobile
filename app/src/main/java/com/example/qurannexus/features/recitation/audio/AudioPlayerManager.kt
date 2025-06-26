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
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity
import com.example.qurannexus.features.recitation.audio.models.AudioRecitationResponse
import com.example.qurannexus.features.recitation.models.PageAyah
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type
import java.util.Collections


class AudioPlayerManager(
    private val context: Context,
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
    private val gson = Gson()
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
    private val _currentlyPlayingAyahKey = MutableLiveData<String?>()
    val currentlyPlayingAyahKey: LiveData<String?> = _currentlyPlayingAyahKey
    private val _isPlaylistMode = MutableLiveData<Boolean>()
    val isPlaylistMode: LiveData<Boolean> = _isPlaylistMode

    private var totalDuration = 0L
    private var currentItemStartTime = 0L
    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "selected_reciter_key") {
            // Handle reciter change if needed in the future
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
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                audioService = null
            }
        }
    }

    init {
        val intent = Intent(context, AudioPlayerService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
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

    fun playAyah(ayahEntity: QuranAyahDetailEntity) {
        stopAndHidePlayer()
        currentMode = PlaybackMode.SINGLE_AYAH
        _shouldShowPlayer.postValue(false) // Don't show the FAB for single ayah playback

        val relativeUrl = getUrlForSelectedReciter(ayahEntity)
        if (relativeUrl.isNotBlank()) {
            audioService?.play(listOf(relativeUrl), listOf(ayahEntity.ayahKey))
            _isPlaying.postValue(true)
            _currentlyPlayingAyahKey.postValue(ayahEntity.ayahKey) // *** SET THE CURRENT KEY ***
        }
        _isPlaylistMode.postValue(false)
    }
    /**
     * Plays a full page sequence. Used for the "By Page" layout.
     */
    fun playPageAyahs(ayahs: List<QuranAyahDetailEntity>) {
        stopAndHidePlayer()
        currentMode = PlaybackMode.PAGE_SEQUENCE
        _shouldShowPlayer.postValue(true)

        val relativeUrls = mutableListOf<String>()
        val ayahKeys = mutableListOf<String>()

        // 1. Prepend Audhubillah and Bismillah if necessary
        val firstAyah = ayahs.firstOrNull()
        if (shouldAddBismillah(firstAyah)) {
            getPrefixAudios()?.let { (audhu, bismi) ->
                relativeUrls.add(audhu)
                ayahKeys.add("Audhubillah")
                relativeUrls.add(bismi)
                ayahKeys.add("Bismillah")
            }
        }

        // 2. Add all ayahs from the page
        for (ayah in ayahs) {
            val url = getUrlForSelectedReciter(ayah)
            if (url.isNotBlank()) {
                relativeUrls.add(url)
                ayahKeys.add(ayah.ayahKey)
            }
        }

        // 3. Play the entire list
        if (relativeUrls.isNotEmpty()) {
            audioService?.play(relativeUrls, ayahKeys)
            _isPlaying.postValue(true)
            // TODO: Add progress updates for playlist
        }
        _isPlaylistMode.postValue(true)
    }
    fun playPageSequence(relativeUrls: List<String>, ayahKeys: List<String>) {
        if (relativeUrls.isEmpty()) return
        stopPlayback()
        _shouldShowPlayer.postValue(true)
        audioService?.play(relativeUrls, ayahKeys)
        startProgressUpdates()
        _isPlaying.postValue(true)
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
//            playAyah(currentAudio.audioUrl, currentAudio.ayahInfo)
        }

        _isPlaying.postValue(true)
        startProgressUpdates()
    }

    private fun getUrlForSelectedReciter(ayahEntity: QuranAyahDetailEntity): String {
        val type: Type = object : TypeToken<Map<String, String>>() {}.type
        val audioUrls: Map<String, String> = gson.fromJson(ayahEntity.ayahAudioUrlsJson, type)
        return audioUrls[getSelectedReciterKey()] ?: ""
    }
    fun startPlayback() {
        if (!isPlaybackActive && audioQueue.isNotEmpty()) {
            isPlaybackActive = true
            shouldContinuePlayback = true
            currentQueueIndex = 0
            playCurrentInQueue()
        }
    }

    // Modify togglePlayPause to handle single ayah state
    fun togglePlayPause() {
        audioService?.togglePlayPause()
        val isNowPlaying = audioService?.isPlaying() == true
        _isPlaying.postValue(isNowPlaying)

        // If we just paused, clear the currently playing key
        if (!isNowPlaying) {
            _currentlyPlayingAyahKey.postValue(null)
        }
    }

    fun stopAndHidePlayer() {
        audioService?.stopPlayback()
        _isPlaying.postValue(false)
        _shouldShowPlayer.postValue(false)
    }

    fun stopPlayback() {
        isPlaybackActive = false
        shouldContinuePlayback = false
        stopProgressUpdates()
        audioService?.stopPlayback()
        _isPlaying.postValue(false)
        currentQueueIndex = 0
        _currentlyPlayingAyahKey.postValue(null) // *** CLEAR THE KEY ***
    }
    fun stopSingleAyah() {
        if (currentMode == PlaybackMode.SINGLE_AYAH) {
            audioService?.stopPlayback()
            _isPlaying.postValue(false)
            _currentlyPlayingAyahKey.postValue(null)
        }
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

    private fun shouldAddBismillah(firstAyah: QuranAyahDetailEntity?): Boolean {
        if (firstAyah == null) return false
        // No Bismillah for Surah 1 (part of Fatiha itself) or Surah 9 (At-Tawbah)
        if (firstAyah.surahId == 1 || firstAyah.surahId == 9) {
            return false
        }
        return firstAyah.ayahIndex == 1
    }
    private fun getPrefixAudios(): Pair<String, String>? {
        // Assuming reciter key in prefs matches the key in the JSON
        return when (getSelectedReciterKey()) {
            "Alafasy" -> Pair("Alafasy/mp3/audhubillah.mp3", "Alafasy/mp3/bismillah.mp3")
            "AbdulBaset_Murattal" -> Pair("AbdulBaset/Murattal/mp3/001000.mp3", "AbdulBaset/Murattal/mp3/bismillah.mp3")
            else -> null // No prefixes for other reciters unless specified
        }
    }
    private fun getSelectedReciterKey(): String {
        // Use the new, correct key to get the saved value
        return sharedPrefs.getString("selected_reciter_key", "Alafasy") ?: "Alafasy"
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
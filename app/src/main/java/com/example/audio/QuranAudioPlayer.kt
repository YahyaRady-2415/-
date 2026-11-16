package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.example.data.static.QuranRecitersData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val surahNumber: Int = 0,
    val surahTitle: String = "",
    val reciterId: String = "alafasy",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val speed: Float = 1.0f,
    val errorMessage: String? = null,
)

class QuranAudioPlayer {
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun playSurah(surahNumber: Int, surahTitle: String, reciterId: String) {
        val current = _state.value
        if (current.surahNumber == surahNumber && current.reciterId == reciterId && mediaPlayer != null) {
            togglePlayPause()
            return
        }
        stop()
        val reciter = QuranRecitersData.byId(reciterId)
        val url = reciter.surahAudioUrl(surahNumber)
        _state.value = AudioPlaybackState(
            surahNumber = surahNumber,
            surahTitle = surahTitle,
            reciterId = reciterId,
            isBuffering = true,
            speed = current.speed,
        )

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        runCatching {
                            player.playbackParams = PlaybackParams().setSpeed(_state.value.speed)
                        }
                    }
                    player.start()
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        isPlaying = true,
                        durationMs = player.duration.coerceAtLeast(0),
                        errorMessage = null,
                    )
                    startTicker()
                }
                setOnCompletionListener {
                    _state.value = _state.value.copy(isPlaying = false, positionMs = 0)
                    tickerJob?.cancel()
                }
                setOnErrorListener { _, _, _ ->
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "تعذر تشغيل التلاوة الصوتية — تأكد من الاتصال بالإنترنت لبث صوت القارئ.",
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "تعذر تهيئة المشغل الصوتي: ${e.localizedMessage ?: ""}",
            )
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = _state.value.copy(isPlaying = false)
            tickerJob?.cancel()
        } else {
            mp.start()
            _state.value = _state.value.copy(isPlaying = true)
            startTicker()
        }
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        runCatching {
            mp.seekTo(positionMs.coerceAtLeast(0))
            _state.value = _state.value.copy(positionMs = positionMs)
        }
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed)
        val mp = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val wasPlaying = mp.isPlaying
                mp.playbackParams = PlaybackParams().setSpeed(speed)
                if (!wasPlaying) mp.pause()
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _state.value = AudioPlaybackState(
            reciterId = _state.value.reciterId,
            speed = _state.value.speed,
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                val mp = mediaPlayer
                if (mp != null && _state.value.isPlaying) {
                    val pos = runCatching { mp.currentPosition }.getOrDefault(0)
                    _state.value = _state.value.copy(positionMs = pos)
                }
                delay(500L)
            }
        }
    }
}

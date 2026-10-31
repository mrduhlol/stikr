package com.stikr.app.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import com.stikr.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small, local interface cues. App lifetime ownership; no audio focus or media session. */
object AppSounds {
    private val mutableEnabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = mutableEnabled.asStateFlow()
    private var preferences: SharedPreferences? = null
    private var audioManager: AudioManager? = null
    private var pool: SoundPool? = null
    private val loaded = mutableSetOf<Int>()
    private val streams = ArrayDeque<Int>()
    private var clickId = 0
    private var slideId = 0
    private var successId = 0
    private var lastPlay = Long.MIN_VALUE

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        val app = context.applicationContext
        preferences = app.getSharedPreferences("app_sounds", Context.MODE_PRIVATE)
        mutableEnabled.value = preferences!!.getBoolean("enabled", true)
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        // Audio is an enhancement: unsupported devices must still open and operate normally.
        runCatching {
            val newPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .build()
            pool = newPool
            newPool.setOnLoadCompleteListener { _, sampleId, status ->
                synchronized(this) { if (status == 0) loaded.add(sampleId) }
            }
            clickId = newPool.load(app, R.raw.ui_click, 1)
            slideId = newPool.load(app, R.raw.ui_slide, 1)
            successId = newPool.load(app, R.raw.ui_success, 1)
        }.onFailure {
            pool?.release()
            pool = null
            loaded.clear()
        }
    }

    @Synchronized
    fun toggle() {
        mutableEnabled.value = !mutableEnabled.value
        preferences?.edit()?.putBoolean("enabled", mutableEnabled.value)?.apply()
        if (!mutableEnabled.value) {
            streams.forEach { pool?.stop(it) }
            streams.clear()
        }
    }

    fun click() = play(clickId, 0.22f)
    fun slide() = play(slideId, 0.20f)
    fun success() = play(successId, 0.28f)

    @Synchronized
    private fun play(sampleId: Int, volume: Float) {
        if (!mutableEnabled.value || sampleId !in loaded) return
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val now = SystemClock.uptimeMillis()
        if (lastPlay != Long.MIN_VALUE && now - lastPlay < 40) return
        val stream = pool?.play(sampleId, volume, volume, 1, 0, 1f) ?: return
        if (stream != 0) {
            lastPlay = now
            // SoundPool reuses stream handles; bound our history as well.
            if (streams.size >= 3) streams.removeFirst()
            streams.addLast(stream)
        }
    }
}

package com.music.bitchord.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * KBitChord's own settings, kept out of [AppSettings] so upstream changes to
 * that file never conflict with them. Stored in their own preferences file.
 */
object KBitChordSettings {

    private const val PREFS = "kbitchord_settings"
    private const val KEY_DIRECT_SOURCE_NO_WAIT = "direct_source_no_wait"

    private lateinit var prefs: SharedPreferences

    /**
     * K-10. Under Direct Source Stream, a track whose addon answer isn't ready
     * when it has to start plays from YouTube straight away instead of waiting
     * up to [DirectSourceGate.LOOKUP_TIMEOUT_MS][com.music.bitchord.playback.DirectSourceGate.LOOKUP_TIMEOUT_MS]
     * for the addon. The addon is still asked, and a better copy it finds is
     * offered in the player rather than swapped in — see
     * [HeldUpgrade][com.music.bitchord.playback.HeldUpgrade].
     */
    val directSourceNoWait = MutableStateFlow(false)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        directSourceNoWait.value = prefs.getBoolean(KEY_DIRECT_SOURCE_NO_WAIT, false)
    }

    fun setDirectSourceNoWait(value: Boolean) {
        directSourceNoWait.value = value
        prefs.edit().putBoolean(KEY_DIRECT_SOURCE_NO_WAIT, value).apply()
    }
}

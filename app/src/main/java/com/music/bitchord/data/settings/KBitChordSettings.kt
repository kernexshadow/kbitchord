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
    private const val KEY_ADDON_HEAD_START_SEC = "direct_source_addon_head_start_sec"

    const val MIN_ADDON_HEAD_START_SEC = 0
    const val MAX_ADDON_HEAD_START_SEC = 10

    /**
     * Measured against Nebula on 2026-11-07: an addon answer took 2.6–3.7 s, the
     * last of it a size check of the file that also opens its server, so 3 s let
     * half of them fall to YouTube and 4 s let all of them play from the addon.
     */
    const val DEFAULT_ADDON_HEAD_START_SEC = 4

    private lateinit var prefs: SharedPreferences

    /**
     * K-10. Under Direct Source Stream, a track whose addon answer isn't ready
     * when it has to start plays from YouTube after [addonHeadStartSec] instead
     * of waiting up to
     * [DirectSourceGate.LOOKUP_TIMEOUT_MS][com.music.bitchord.playback.DirectSourceGate.LOOKUP_TIMEOUT_MS]
     * for the addon. The addon is still asked, and a better copy it finds is
     * offered in the player rather than swapped in — see
     * [HeldUpgrade][com.music.bitchord.playback.HeldUpgrade].
     */
    val directSourceNoWait = MutableStateFlow(false)

    /**
     * K-10. How long a track that has to start now (a tap, a skip) waits for
     * the addon before it plays from YouTube. Zero starts on whichever answers
     * first, which is nearly always YouTube.
     */
    val addonHeadStartSec = MutableStateFlow(DEFAULT_ADDON_HEAD_START_SEC)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        directSourceNoWait.value = prefs.getBoolean(KEY_DIRECT_SOURCE_NO_WAIT, false)
        addonHeadStartSec.value =
            clampHeadStart(prefs.getInt(KEY_ADDON_HEAD_START_SEC, DEFAULT_ADDON_HEAD_START_SEC))
    }

    fun setDirectSourceNoWait(value: Boolean) {
        directSourceNoWait.value = value
        prefs.edit().putBoolean(KEY_DIRECT_SOURCE_NO_WAIT, value).apply()
    }

    fun setAddonHeadStartSec(value: Int) {
        val clamped = clampHeadStart(value)
        if (clamped == addonHeadStartSec.value) return
        addonHeadStartSec.value = clamped
        prefs.edit().putInt(KEY_ADDON_HEAD_START_SEC, clamped).apply()
    }

    internal fun clampHeadStart(value: Int): Int =
        value.coerceIn(MIN_ADDON_HEAD_START_SEC, MAX_ADDON_HEAD_START_SEC)
}

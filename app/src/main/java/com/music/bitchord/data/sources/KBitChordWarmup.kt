package com.music.bitchord.data.sources

import com.music.bitchord.data.TrackLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * KBitChord (K-11): work done once at startup so the first track doesn't pay
 * for it.
 */
object KBitChordWarmup {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Reads every enabled addon's manifest as the app starts. A search or
     * stream request reads it first, and without this the first track after a
     * start waited for it (0.66 s measured against Nebula). Afterwards the
     * client keeps the last copy and refreshes it in the background — see
     * [AddonClient.manifestForRequest][com.music.bitchord.data.sources.addon.AddonClient.manifestForRequest].
     */
    fun warmAddonManifests() {
        scope.launch {
            SourceRegistry.active().filterIsInstance<AddonSource>().forEach { addon ->
                launch {
                    runCatching { addon.refreshPolicy() }
                    TrackLog.d("BitChord", "warmed the manifest of ${addon.displayName}")
                }
            }
        }
    }
}

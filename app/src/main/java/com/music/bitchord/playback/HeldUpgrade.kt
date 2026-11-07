package com.music.bitchord.playback

import android.net.Uri
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.UpgradeOffers
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.KBitChordSettings
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.sources.SourceStream
import com.music.bitchord.data.sources.StreamFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * KBitChord (K-10, K-13): the better copy of a track that started on YouTube,
 * held for the listener to ask for instead of swapped in.
 *
 * With "Start on YouTube if the addon is slow" on, a track whose addon answer
 * isn't ready when it has to start plays from YouTube — see
 * `PlaybackService.resolveDirectWithoutWaiting`. The addon lookup runs on, and
 * whatever [QualityUpgrade] finds is parked here rather than handed to the
 * swap, because a swap cuts the audio and the point of the setting is that
 * nothing does unless the listener asks. The player shows the offer through
 * [UpgradeOffers]; a tap, or "Upgrade quality" in the player menu, swaps it in.
 *
 * What the offer leaves for the next play of the track (K-13):
 *  - **On Wi-Fi** the offered copy is downloaded whole in the background
 *    ([AudioCache.kbKeepOfferedCopy]). A tap then swaps to it from disk, and
 *    once the track has stopped playing it is kept and the YouTube copy
 *    dropped, so the next play starts on it.
 *  - **Otherwise**, or if it never came whole, the YouTube copy is noted as
 *    having a lossless copy at the addon, so the next play asks the addon
 *    first instead of starting on the copy on disk.
 * See [AudioCache.kbSettleOffer].
 */
object HeldUpgrade {

    private const val TAG = "BitChord"

    /** An offer: the copy, the queue item it was found for, and the cache entry that item was reading. */
    private class Held(val stream: SourceStream, val itemUri: Uri?, val opusKey: String?)

    private val held = ConcurrentHashMap<String, Held>()

    /** Tracks whose upgrade the listener asked for from the menu: theirs swaps in as usual. */
    private val byHand = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Background downloads of offered copies, by media id — see [AudioCache.kbKeepOfferedCopy]. */
    private val keeping = ConcurrentHashMap<String, Job>()

    /** Offers whose track stopped playing while their copy was still downloading. */
    private val leftWhileKeeping = ConcurrentHashMap<String, Held>()

    /** Whether a better copy found for [mediaId] is offered rather than swapped in. */
    fun holds(mediaId: String): Boolean =
        KBitChordSettings.directSourceNoWait.value && DirectSourceGate.isActive() && mediaId !in byHand

    /** Offers [stream] for [mediaId], playing from [itemUri], replacing any offer for another track. */
    fun offer(mediaId: String, stream: SourceStream, itemUri: Uri?) {
        held.keys.filter { it != mediaId }.forEach(::drop)
        val offer = Held(stream, itemUri, itemUri?.let { runCatching { AudioCache.cacheKeyOf(it) }.getOrNull() })
        held[mediaId] = offer
        val label = labelFor(stream.format)
        UpgradeOffers.publish(mediaId, label)
        TrackLog.d(TAG, "better copy of $mediaId found ($label); offered instead of swapped in", about = mediaId)
        if (itemUri != null && onWifi()) keep(mediaId, offer)
    }

    /** Whether [mediaId] has a copy on offer. */
    fun has(mediaId: String): Boolean = held.containsKey(mediaId)

    /** Takes the copy on offer for [mediaId], to swap it in. */
    fun take(mediaId: String): SourceStream? {
        val offer = held.remove(mediaId)
        UpgradeOffers.withdraw(mediaId)
        return offer?.stream
    }

    /** The listener asked for [mediaId]'s upgrade from the menu, so its result swaps in. */
    fun askedByHand(mediaId: String) {
        byHand += mediaId
    }

    /** Drops offers and hand requests for tracks that are no longer playing. Main thread. */
    fun onPlaying(audible: Set<String>) {
        held.keys.filter { it !in audible }.forEach(::drop)
        byHand.retainAll(audible)
    }

    private fun drop(mediaId: String) {
        val offer = held.remove(mediaId) ?: return
        UpgradeOffers.withdraw(mediaId)
        if (keeping[mediaId]?.isActive == true) leftWhileKeeping[mediaId] = offer
        // Notes the YouTube copy now; a download that comes whole later
        // replaces it — see [keep].
        AudioCache.kbSettleOffer(mediaId, offer.itemUri, offer.opusKey)
    }

    private fun onWifi(): Boolean = AppSettings.meteredConnection.value == false

    private fun keep(mediaId: String, offer: Held) {
        val itemUri = offer.itemUri ?: return
        if (keeping[mediaId]?.isActive == true) return
        // Parked where the swap looks for it, so a tap reuses what is already
        // on disk instead of starting the rendition over — see `swapIn`.
        QualityUpgrade.force(mediaId, offer.stream)
        keeping[mediaId] = scope.launch(TrackLog.about(mediaId)) {
            val whole = AudioCache.kbKeepOfferedCopy(itemUri, offer.stream) {
                onWifi() &&
                    SourceRegistry.permitsForPlayback(offer.stream.sourceConfigId) &&
                    (held[mediaId] === offer || leftWhileKeeping[mediaId] === offer)
            }
            val left = leftWhileKeeping.remove(mediaId)
            if (whole && left != null) AudioCache.kbSettleOffer(mediaId, left.itemUri, left.opusKey)
        }
    }

    /** What the offer button names: "Dolby Atmos", "Hi-Res", the lossless codec, or the bitrate. */
    internal fun labelFor(format: StreamFormat): String = when {
        format.isDolbyAtmos -> "Dolby Atmos"
        format.isLossless == true ->
            if ((format.bitDepth ?: 16) > 16 || (format.sampleRateHz ?: 44_100) > 48_000) {
                "Hi-Res"
            } else {
                format.codec?.uppercase(java.util.Locale.ROOT)?.takeIf { it.length <= 5 } ?: "Lossless"
            }
        format.kbps != null -> "${format.kbps} kbps"
        else -> format.summary
    }
}

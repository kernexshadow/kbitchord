package com.music.bitchord.playback

import com.music.bitchord.data.settings.KBitChordSettings
import com.music.bitchord.data.spotify.LocalPlaylistStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * KBitChord (K-14): which songs in the song cache belong to a playlist kept on
 * this device — the local playlists, Spotify imports among them — so the cache
 * can clear the others first.
 *
 * Read on the cache's own threads, for every span it considers evicting, so it
 * is a set held in memory and swapped whole whenever the playlists change.
 */
object KBitChordPlaylistCache {

    @Volatile
    private var videoIds: Set<String> = emptySet()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Follows [LocalPlaylistStore] from now on. Call once, after it is initialised. */
    fun start() {
        scope.launch {
            LocalPlaylistStore.playlists.collect { playlists ->
                videoIds = playlists.flatMapTo(HashSet()) { playlist ->
                    playlist.songs.mapNotNull { song -> song.videoId.takeIf { it.isNotBlank() } }
                }
            }
        }
    }

    /**
     * Whether the cache entry [key] holds a song from one of those playlists.
     * The part before `#` is the YouTube id for every rendition of a YouTube
     * track — see `AudioCache`'s key factory; a source row's `source|track` key
     * is never in a playlist.
     */
    fun keyInPlaylist(key: String): Boolean {
        val base = key.substringBefore('#')
        return '|' !in base && base in videoIds
    }

    /** For [DynamicLruCacheEvictor.evictLast]: playlist songs go last, while the setting is on. */
    fun evictLast(key: String): Boolean = KBitChordSettings.cacheKeepPlaylists.value && keyInPlaylist(key)

    /** Test seam. */
    internal fun setForTest(ids: Set<String>) {
        videoIds = ids
    }
}

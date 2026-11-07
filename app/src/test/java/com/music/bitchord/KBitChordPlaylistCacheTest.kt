package com.music.bitchord

import androidx.media3.datasource.cache.CacheSpan
import com.music.bitchord.data.settings.KBitChordSettings
import com.music.bitchord.playback.DynamicLruCacheEvictor
import com.music.bitchord.playback.KBitChordPlaylistCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** KBitChord (K-14): playlist songs are evicted last, and only while the setting is on. */
class KBitChordPlaylistCacheTest {

    @After
    fun reset() {
        KBitChordSettings.cacheKeepPlaylists.value = false
        KBitChordPlaylistCache.setForTest(emptySet())
    }

    private fun span(key: String, touched: Long) = CacheSpan(key, 0, 1_000, touched, null)

    @Test
    fun `every rendition of a playlist song counts, a source row never does`() {
        KBitChordPlaylistCache.setForTest(setOf("abc"))
        assertTrue(KBitChordPlaylistCache.keyInPlaylist("abc"))
        assertTrue(KBitChordPlaylistCache.keyInPlaylist("abc#alt-noatmos"))
        assertTrue(KBitChordPlaylistCache.keyInPlaylist("abc#hifi"))
        assertFalse(KBitChordPlaylistCache.keyInPlaylist("xyz#alt"))
        assertFalse(KBitChordPlaylistCache.keyInPlaylist("src|abc"))
    }

    @Test
    fun `the oldest song outside playlists is evicted first`() {
        KBitChordPlaylistCache.setForTest(setOf("old"))
        KBitChordSettings.cacheKeepPlaylists.value = true
        val spans = listOf(span("old#alt", 1), span("new#alt", 2))
        assertEquals("new#alt", DynamicLruCacheEvictor.pickVictim(spans, KBitChordPlaylistCache::evictLast).key)
    }

    @Test
    fun `with only playlist songs left, the oldest goes`() {
        KBitChordPlaylistCache.setForTest(setOf("a", "b"))
        KBitChordSettings.cacheKeepPlaylists.value = true
        val spans = listOf(span("a#alt", 1), span("b#alt", 2))
        assertEquals("a#alt", DynamicLruCacheEvictor.pickVictim(spans, KBitChordPlaylistCache::evictLast).key)
    }

    @Test
    fun `with the setting off, plain least recently used`() {
        KBitChordPlaylistCache.setForTest(setOf("old"))
        val spans = listOf(span("old#alt", 1), span("new#alt", 2))
        assertEquals("old#alt", DynamicLruCacheEvictor.pickVictim(spans, KBitChordPlaylistCache::evictLast).key)
    }
}

package com.music.bitchord.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** KBitChord: "+k.N" revisions in the in-app updater. Release tags reach isNewer without their "v". */
class KBitChordRevisionTest {

    private fun assertOrdered(vararg versions: String) {
        for (i in versions.indices) {
            for (j in versions.indices) {
                val newer = AppUpdateChecker.isNewer(versions[j], versions[i])
                if (j > i) {
                    assertTrue("${versions[j]} should be newer than ${versions[i]}", newer)
                } else {
                    assertFalse("${versions[j]} should not be newer than ${versions[i]}", newer)
                }
            }
        }
    }

    @Test
    fun revisionsOrderBetweenUpstreamVersions() {
        assertOrdered("1.8.1+k", "1.8.1+k.1", "1.8.1+k.2", "1.9+k", "1.9+k.1", "1.9+k.10", "1.9.1+k", "1.10+k")
    }

    @Test
    fun plainKEqualsUpstreamVersion() {
        assertFalse(AppUpdateChecker.isNewer("1.9+k", "1.9"))
        assertFalse(AppUpdateChecker.isNewer("1.9", "1.9+k"))
        assertTrue(AppUpdateChecker.isNewer("1.9+k.1", "1.9"))
    }

    @Test
    fun otherBuildMetadataIsIgnored() {
        assertFalse(AppUpdateChecker.isNewer("1.9+build.7", "1.9+k"))
        assertFalse(AppUpdateChecker.isNewer("1.9+k", "1.9+build.7"))
        assertTrue(AppUpdateChecker.isNewer("1.9.1+build.7", "1.9+k.3"))
    }

    @Test
    fun betaStillPrecedesRelease() {
        assertTrue(AppUpdateChecker.isNewer("1.9+k", "1.9-beta2"))
        assertFalse(AppUpdateChecker.isNewer("1.9-beta2", "1.9+k"))
        assertTrue(AppUpdateChecker.isNewer("1.9-beta3+k.2", "1.9-beta3+k.1"))
    }

    @Test
    fun upstreamOrderingIsUnchanged() {
        assertOrdered("1.8", "1.8.1", "1.9", "1.10")
        assertTrue(AppUpdateChecker.isNewer("1.9", "1.9-beta2"))
        assertFalse(AppUpdateChecker.isNewer("1.9-beta2", "1.9"))
    }
}

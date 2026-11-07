package com.music.bitchord

import com.music.bitchord.data.UpgradeOffers
import com.music.bitchord.data.sources.StreamFormat
import com.music.bitchord.playback.HeldUpgrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** KBitChord (K-10): what the player's offer button says, and when it goes away. */
class KBitChordUpgradeOfferTest {

    @Test
    fun `a CD-quality lossless copy is named by its codec`() {
        assertEquals("FLAC", HeldUpgrade.labelFor(StreamFormat(codec = "flac", bitDepth = 16, sampleRateHz = 44_100)))
        assertEquals("ALAC", HeldUpgrade.labelFor(StreamFormat(codec = "alac")))
    }

    @Test
    fun `a lossless copy above CD quality is Hi-Res`() {
        assertEquals("Hi-Res", HeldUpgrade.labelFor(StreamFormat(codec = "flac", bitDepth = 24, sampleRateHz = 48_000)))
        assertEquals("Hi-Res", HeldUpgrade.labelFor(StreamFormat(codec = "flac", bitDepth = 16, sampleRateHz = 96_000)))
    }

    @Test
    fun `a lossy copy is named by its bitrate`() {
        assertEquals("320 kbps", HeldUpgrade.labelFor(StreamFormat(codec = "aac", kbps = 320)))
    }

    @Test
    fun `Dolby Atmos is named as such`() {
        assertEquals("Dolby Atmos", HeldUpgrade.labelFor(StreamFormat(codec = "eac3-joc", kbps = 768)))
    }

    @Test
    fun `an offer is withdrawn only for its own track`() {
        UpgradeOffers.publish("a", "FLAC")
        UpgradeOffers.withdraw("b")
        assertEquals(UpgradeOffers.Offer("a", "FLAC"), UpgradeOffers.offered.value)
        UpgradeOffers.withdraw("a")
        assertNull(UpgradeOffers.offered.value)
    }
}

/** KBitChord (K-10): the addon head start stays within what the slider offers. */
class KBitChordHeadStartTest {

    @Test
    fun `the head start is kept between 0 and 10 seconds`() {
        assertEquals(0, com.music.bitchord.data.settings.KBitChordSettings.clampHeadStart(-2))
        assertEquals(3, com.music.bitchord.data.settings.KBitChordSettings.clampHeadStart(3))
        assertEquals(10, com.music.bitchord.data.settings.KBitChordSettings.clampHeadStart(45))
    }
}

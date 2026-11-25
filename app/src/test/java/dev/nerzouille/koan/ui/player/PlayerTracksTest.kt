package dev.nerzouille.koan.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PlayerTracksTest {
    private val en = Locale.ENGLISH
    private val tracks = listOf(TrackInfo("", "fr"), TrackInfo("English (SDH)", "en"), TrackInfo("", ""))

    @Test fun `a track is named by its label, else its language, else its number`() {
        assertEquals("French", trackLabel(tracks[0], 0, en))
        assertEquals("English (SDH)", trackLabel(tracks[1], 1, en))
        assertEquals("Track 3", trackLabel(tracks[2], 2, en))
        assertEquals("Track 1", trackLabel(TrackInfo(" ", "und"), 0, en))
        // MediaPlayer's three-letter codes.
        assertEquals("English", trackLabel(TrackInfo("", "eng"), 0, en))
        assertEquals("French", trackLabel(TrackInfo("", "fre"), 0, en))
        assertEquals("Japanese", trackLabel(TrackInfo("", "jpn"), 0, en))
    }

    @Test fun `captions are Off plus each track, the current one checked`() {
        var picked = 99
        val c = captionChoices(tracks, selected = 1, pick = { picked = it }, locale = en)
        assertEquals(listOf("Off", "French", "English (SDH)", "Track 3"), c.map { it.label })
        assertEquals(listOf(false, false, true, false), c.map { it.selected })
        c[0].onPick()
        assertEquals(-1, picked)
        c[1].onPick()
        assertEquals(0, picked)
        assertTrue(captionChoices(tracks, -1, {}, en)[0].selected)
    }

    @Test fun `no captions row without tracks, a row with one`() {
        assertNull(choiceRow(dev.nerzouille.koan.ui.icons.KoanIcons.Captions, "Captions", captionChoices(emptyList(), -1, {})))
        val one = choiceRow(dev.nerzouille.koan.ui.icons.KoanIcons.Captions, "Captions", captionChoices(listOf(TrackInfo("", "de")), -1, {}, en))
        assertEquals("Off", one?.value)
    }

    @Test fun `audio needs two tracks`() {
        assertEquals(0, audioChoices(listOf(TrackInfo("", "en")), 0, {}).size)
        assertEquals(2, audioChoices(listOf(TrackInfo("", "en"), TrackInfo("", "ja")), 0, {}).size)
    }

    @Test fun `the language picked last comes back when nothing is on`() {
        assertEquals(1, rememberedTrack(tracks, current = -1, remembered = "en-GB"))
        assertEquals(-1, rememberedTrack(tracks, current = 0, remembered = "en"))
        assertEquals(-1, rememberedTrack(tracks, current = -1, remembered = ""))
        assertEquals(-1, rememberedTrack(tracks, current = -1, remembered = null))
        assertEquals(-1, rememberedTrack(tracks, current = -1, remembered = "ja"))
    }

    @Test fun `a picked choice also closes the menu`() {
        var closed = false
        var picked = -2
        val row = MenuRow(dev.nerzouille.koan.ui.icons.KoanIcons.Captions, "Captions", choices = captionChoices(tracks, -1, { picked = it }, en))
            .after { closed = true }
        row.choices!![2].onPick()
        assertTrue(closed)
        assertEquals(1, picked)
    }

    @Test fun `standalone subtitles always offer a file`() {
        var chose = false
        val empty = subtitlesRow(emptyList(), -1, {}, { chose = true }, en)
        assertEquals(listOf("Off", CHOOSE_FILE), empty.choices!!.map { it.label })
        assertEquals("Off", empty.value)
        empty.choices!!.last().onPick()
        assertTrue(chose)
        val two = subtitlesRow(listOf(TrackInfo("Clip.srt", "und"), TrackInfo("", "eng")), 0, {}, {}, en)
        assertEquals(listOf("Off", "Clip.srt", "English", CHOOSE_FILE), two.choices!!.map { it.label })
        assertEquals("Clip.srt", two.value)
    }
}

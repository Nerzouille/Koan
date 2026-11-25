package dev.nerzouille.koan.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitlesTest {
    @Test fun `webvtt becomes subrip`() {
        val vtt = "\uFEFFWEBVTT - a title\r\nKind: captions\r\n\r\n" +
            "NOTE a comment\r\nwith two lines\r\n\r\n" +
            "STYLE\n::cue { color: yellow }\n\n" +
            "intro\n00:01.000 --> 00:04.250 align:start position:10%\n<v Narrator>Hello &amp; <i>welcome</i></v>\nSecond line\n\n" +
            "1:02:03.5 --> 1:02:05.000\n<c.loud>Later</c> <00:00:01.000>on\n\n" +
            "00:00:09.000 --> 00:00:10.000\n\n"
        val expected = "1\n00:00:01,000 --> 00:00:04,250\nHello & welcome\nSecond line\n\n" +
            "2\n01:02:03,500 --> 01:02:05,000\nLater on\n\n"
        assertEquals(expected, vttToSrt(vtt))
    }

    @Test fun `times`() {
        assertEquals("00:00:01,000", srtTime("00:01.000"))
        assertEquals("10:00:00,050", srtTime("10:00:00.05"))
        assertNull(srtTime("soon"))
    }

    @Test fun `a file is read by its kind`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nHi\n"
        assertEquals(srt, subtitleAsSrt("Clip.srt", srt))
        assertEquals(srt, subtitleAsSrt("download", "\uFEFF" + srt))
        assertEquals("1\n00:00:01,000 --> 00:00:02,000\nHi\n\n", subtitleAsSrt("clip.VTT", "WEBVTT\n\n00:01.000 --> 00:02.000\nHi"))
        assertNull(subtitleAsSrt("notes.txt", "nothing here"))
        assertNull(subtitleAsSrt("empty.vtt", "WEBVTT\n"))
    }

    @Test fun `side files share the video's name`() {
        assertEquals(listOf("My clip.srt", "My clip.vtt"), sideSubtitleNames("My clip.mp4"))
        assertEquals(listOf("noext.srt", "noext.vtt"), sideSubtitleNames("noext"))
    }
}

package dev.yusufaf.duckviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareContentTest {
    @Test
    fun `https page is shareable with its title`() {
        assertEquals(
            ShareContent("https://example.com/a?b=1", "Example"),
            shareContent("https://example.com/a?b=1", "Example"),
        )
    }

    @Test
    fun `http page is shareable`() {
        assertEquals(ShareContent("http://example.com/", "Example"), shareContent("http://example.com/", "Example"))
    }

    @Test
    fun `scheme match ignores case`() {
        assertEquals(ShareContent("HTTPS://EXAMPLE.COM/", null), shareContent("HTTPS://EXAMPLE.COM/", null))
    }

    @Test
    fun `non web urls are not shareable`() {
        listOf(
            "about:blank",
            "data:text/html,hi",
            "javascript:alert(1)",
            "file:///sdcard/a.html",
            "intent://x#Intent;end",
            "https://",
            "https://?q=1",
            "http://#frag",
        ).forEach { assertNull(it, shareContent(it, "Title")) }
    }

    @Test
    fun `null or blank url is not shareable`() {
        assertNull(shareContent(null, "Title"))
        assertNull(shareContent("", "Title"))
        assertNull(shareContent("   ", "Title"))
    }

    @Test
    fun `blank or missing title is dropped`() {
        assertEquals(ShareContent("https://example.com/", null), shareContent("https://example.com/", null))
        assertEquals(ShareContent("https://example.com/", null), shareContent("https://example.com/", "  "))
    }

    @Test
    fun `title equal to url is dropped`() {
        assertEquals(
            ShareContent("https://example.com/", null),
            shareContent("https://example.com/", "https://example.com/"),
        )
    }

    @Test
    fun `title is trimmed`() {
        assertEquals(
            ShareContent("https://example.com/", "Example"),
            shareContent("https://example.com/", "  Example \n"),
        )
    }

    @Test
    fun `title reported for another url is dropped`() {
        assertEquals(
            ShareContent("https://example.com/b", null),
            shareContent("https://example.com/b", "Page A", titleUrl = "https://example.com/a"),
        )
    }

    @Test
    fun `title reported for this url is kept`() {
        assertEquals(
            ShareContent("https://example.com/b", "Page B"),
            shareContent("https://example.com/b", "Page B", titleUrl = "https://example.com/b"),
        )
    }

    @Test
    fun `title with no known url is dropped`() {
        assertEquals(
            ShareContent("https://example.com/b", null),
            shareContent("https://example.com/b", "Page B", titleUrl = null),
        )
    }

    @Test
    fun `title survives in-page fragment navigation`() {
        assertEquals(
            ShareContent("https://example.com/a#history", "Page A"),
            shareContent("https://example.com/a#history", "Page A", titleUrl = "https://example.com/a"),
        )
    }

    @Test
    fun `credentials are stripped from the shared url`() {
        assertEquals(
            ShareContent("https://intranet.example/a", "Wiki"),
            shareContent("https://admin:secret@intranet.example/a", "Wiki", titleUrl = "https://admin:secret@intranet.example/a"),
        )
        assertEquals(ShareContent("http://example.com/", null), shareContent("http://user@example.com/", null))
    }

    @Test
    fun `userinfo without a host is not shareable`() {
        assertNull(shareContent("http://user@/", null))
    }

    @Test
    fun `url is trimmed`() {
        assertEquals(ShareContent("https://example.com/", null), shareContent(" https://example.com/ ", null))
    }
}

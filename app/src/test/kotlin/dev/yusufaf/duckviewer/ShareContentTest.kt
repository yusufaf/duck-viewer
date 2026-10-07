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
    fun `url is trimmed`() {
        assertEquals(ShareContent("https://example.com/", null), shareContent(" https://example.com/ ", null))
    }
}

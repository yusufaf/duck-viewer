package dev.yusufaf.duckviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingRequestTest {

    private val view = "android.intent.action.VIEW"
    private val webSearch = "android.intent.action.WEB_SEARCH"
    private val processText = "android.intent.action.PROCESS_TEXT"
    private val main = "android.intent.action.MAIN"

    @Test
    fun `view with a link is a url`() {
        assertEquals(
            IncomingRequest.Url("https://duckduckgo.com/?q=a"),
            IncomingRequest.from(view, data = "https://duckduckgo.com/?q=a", query = null, processText = null),
        )
    }

    @Test
    fun `view without data is empty`() {
        assertEquals(IncomingRequest.Empty, IncomingRequest.from(view, data = null, query = null, processText = null))
    }

    @Test
    fun `web search with text is a trimmed query`() {
        assertEquals(
            IncomingRequest.Query("portland weather"),
            IncomingRequest.from(webSearch, data = null, query = "  portland weather ", processText = null),
        )
    }

    @Test
    fun `web search with a link is a url`() {
        assertEquals(
            IncomingRequest.Url("https://example.com/x"),
            IncomingRequest.from(webSearch, data = null, query = "https://example.com/x", processText = null),
        )
        assertEquals(
            IncomingRequest.Url("HTTP://example.com"),
            IncomingRequest.from(webSearch, data = null, query = "HTTP://example.com", processText = null),
        )
    }

    @Test
    fun `text with a link inside is still a query`() {
        assertEquals(
            IncomingRequest.Query("see https://example.com"),
            IncomingRequest.from(webSearch, data = null, query = "see https://example.com", processText = null),
        )
    }

    @Test
    fun `selected text is a query`() {
        assertEquals(
            IncomingRequest.Query("kotlin coroutines"),
            IncomingRequest.from(processText, data = null, query = null, processText = "kotlin coroutines"),
        )
    }

    @Test
    fun `blank text is empty`() {
        assertEquals(IncomingRequest.Empty, IncomingRequest.from(webSearch, data = null, query = "   ", processText = null))
        assertEquals(IncomingRequest.Empty, IncomingRequest.from(processText, data = null, query = null, processText = ""))
    }

    @Test
    fun `launcher start is empty`() {
        assertEquals(IncomingRequest.Empty, IncomingRequest.from(main, data = null, query = null, processText = null))
        assertEquals(IncomingRequest.Empty, IncomingRequest.from(null, data = null, query = null, processText = null))
    }
}

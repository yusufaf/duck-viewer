package dev.yusufaf.duckviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlRouterTest {

    private val router = UrlRouter(ResultsPage.LITE)

    private fun lite(encodedQuery: String) = Route.Load("https://lite.duckduckgo.com/lite/?q=$encodedQuery")

    @Test
    fun `ddg search becomes lite results`() {
        assertEquals(lite("portland+weather"), router.route("https://duckduckgo.com/?q=portland+weather"))
    }

    @Test
    fun `extra params like t and ia=web are dropped`() {
        assertEquals(
            lite("portland+weather"),
            router.route("https://duckduckgo.com/?q=portland+weather&t=niagara&ia=web"),
        )
    }

    @Test
    fun `niagara suggestion url becomes lite results`() {
        // Captured from Niagara Launcher on a real phone (2026-10-03).
        assertEquals(lite("best+pizza"), router.route("https://duckduckgo.com/?q=best%20pizza&ia=web"))
    }

    @Test
    fun `ddg subdomain and mixed case host are recognized`() {
        assertEquals(lite("kotlin"), router.route("https://start.duckduckgo.com/?q=kotlin"))
        assertEquals(lite("kotlin"), router.route("https://DuckDuckGo.COM/?q=kotlin"))
    }

    @Test
    fun `search without trailing slash is recognized`() {
        assertEquals(lite("kotlin"), router.route("https://duckduckgo.com?q=kotlin"))
    }

    @Test
    fun `html endpoint search becomes lite when lite is configured`() {
        assertEquals(lite("kotlin"), router.route("https://html.duckduckgo.com/html/?q=kotlin"))
    }

    @Test
    fun `look-alike hosts are not ddg`() {
        assertEquals(
            Route.External("https://evilduckduckgo.com/?q=kotlin"),
            router.route("https://evilduckduckgo.com/?q=kotlin"),
        )
        assertEquals(
            Route.External("https://duckduckgo.com.evil.example/?q=kotlin"),
            router.route("https://duckduckgo.com.evil.example/?q=kotlin"),
        )
    }

    @Test
    fun `redirect link is decoded to its destination`() {
        assertEquals(
            Route.External("https://example.com/a?b=1&c=2"),
            router.route("https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fa%3Fb%3D1%26c%3D2&rut=abc123"),
        )
    }

    @Test
    fun `protocol-relative redirect link is decoded`() {
        assertEquals(
            Route.External("https://example.com/"),
            router.route("//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2F&rut=abc123"),
        )
    }

    @Test
    fun `redirect is decoded exactly once`() {
        assertEquals(
            Route.External("https://example.com/a%20b"),
            router.route("https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fa%2520b"),
        )
    }

    @Test
    fun `redirect to a ddg search is rewritten to lite`() {
        assertEquals(
            lite("kotlin"),
            router.route("https://duckduckgo.com/l/?uddg=https%3A%2F%2Fduckduckgo.com%2F%3Fq%3Dkotlin"),
        )
    }

    @Test
    fun `image and other tab searches load as-is`() {
        val images = "https://duckduckgo.com/?q=cats&ia=images"
        val imagesX = "https://duckduckgo.com/?q=cats&iax=images&ia=images"
        assertEquals(Route.Load(images), router.route(images))
        assertEquals(Route.Load(imagesX), router.route(imagesX))
    }

    @Test
    fun `bang searches load on the full site as-is`() {
        val bang = "https://duckduckgo.com/?q=!w+kotlin"
        val trailingBang = "https://duckduckgo.com/?q=kotlin+%21w"
        assertEquals(Route.Load(bang), router.route(bang))
        assertEquals(Route.Load(trailingBang), router.route(trailingBang))
    }

    @Test
    fun `exclamation inside a word is not a bang`() {
        assertEquals(lite("yahoo%21"), router.route("https://duckduckgo.com/?q=yahoo%21"))
    }

    @Test
    fun `ddg pages that are not searches load as-is`() {
        assertEquals(Route.Load("https://duckduckgo.com/"), router.route("https://duckduckgo.com/"))
        assertEquals(Route.Load("https://duckduckgo.com/settings"), router.route("https://duckduckgo.com/settings"))
        assertEquals(Route.Load("https://duckduckgo.com/?q="), router.route("https://duckduckgo.com/?q="))
    }

    @Test
    fun `configured results page is left untouched`() {
        val url = "https://lite.duckduckgo.com/lite/?q=portland%20weather&kl=us-en"
        assertEquals(Route.Load(url), router.route(url))
    }

    @Test
    fun `http is upgraded to https`() {
        assertEquals(Route.External("https://example.com/page"), router.route("http://example.com/page"))
        assertEquals(lite("kotlin"), router.route("http://duckduckgo.com/?q=kotlin"))
    }

    @Test
    fun `unencoded spaces are tolerated`() {
        assertEquals(lite("kotlin+webview"), router.route("https://duckduckgo.com/?q=kotlin webview"))
    }

    @Test
    fun `urls java's parser rejects but browsers accept still route`() {
        assertEquals(Route.External("https://my_site.example.com/"), router.route("https://my_site.example.com/"))
        assertEquals(Route.External("https://example.com/?a=b|c{d}^"), router.route("https://example.com/?a=b|c{d}^"))
        assertEquals(lite("a%7Cb"), router.route("https://duckduckgo.com/?q=a|b"))
    }

    @Test
    fun `app schemes are handed off and dangerous schemes blocked`() {
        assertEquals(Route.Handoff("mailto:someone@example.com"), router.route("mailto:someone@example.com"))
        assertEquals(Route.Handoff("tel:+15551234567"), router.route("tel:+15551234567"))
        assertEquals(Route.Blocked, router.route("javascript:alert(1)"))
        assertEquals(Route.Blocked, router.route("file:///sdcard/secret.txt"))
        assertEquals(Route.Blocked, router.route("content://com.example/secret"))
    }

    @Test
    fun `garbage and blank input are blocked`() {
        assertEquals(Route.Blocked, router.route(""))
        assertEquals(Route.Blocked, router.route("   "))
        assertEquals(Route.Blocked, router.route("https://"))
        assertEquals(Route.Blocked, router.route("not a url"))
    }

    @Test
    fun `query is url-encoded`() {
        assertEquals(lite("c%2B%2B+%26+rust"), router.forQuery("c++ & rust"))
        assertEquals(lite("caf%C3%A9"), router.forQuery("café"))
    }

    @Test
    fun `bang query goes to the full site`() {
        assertEquals(Route.Load("https://duckduckgo.com/?q=%21w+kotlin"), router.forQuery("!w kotlin"))
    }

    @Test
    fun `other results pages build their own urls`() {
        assertEquals(
            Route.Load("https://html.duckduckgo.com/html/?q=kotlin"),
            UrlRouter(ResultsPage.HTML).forQuery("kotlin"),
        )
        assertEquals(
            Route.Load("https://duckduckgo.com/?q=kotlin"),
            UrlRouter(ResultsPage.FULL).forQuery("kotlin"),
        )
        assertEquals(
            Route.Load("https://duckduckgo.com/?q=kotlin&ia=web"),
            UrlRouter(ResultsPage.FULL).route("https://duckduckgo.com/?q=kotlin&ia=web"),
        )
    }
}

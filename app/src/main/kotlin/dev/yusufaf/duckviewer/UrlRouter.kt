package dev.yusufaf.duckviewer

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

sealed interface Route {
    /** A DuckDuckGo page; always shown in the view. */
    data class Load(val url: String) : Route

    /** Any other web page; [LinkPolicy] decides what happens to it. */
    data class External(val url: String) : Route

    /** A non-web link (mailto:, tel:, intent:) for another app to handle. */
    data class Handoff(val uri: String) : Route

    data object Blocked : Route
}

/**
 * Pure URL logic, kept free of android.net.Uri so it runs in plain JVM tests.
 */
class UrlRouter(private val page: ResultsPage) {

    fun forQuery(query: String): Route.Load =
        Route.Load(if (isBang(query)) ResultsPage.FULL.url(query) else page.url(query))

    fun route(raw: String): Route = route(raw, depth = 0)

    private fun route(raw: String, depth: Int): Route {
        // Valid URIs never contain a raw space, but pasted or hand-typed links do.
        val text = raw.trim().replace(" ", "%20").let { if (it.startsWith("//")) "https:$it" else it }
        val uri = parse(text) ?: return Route.Blocked
        val scheme = uri.scheme?.lowercase() ?: return Route.Blocked

        if (scheme !in WEB_SCHEMES) {
            return if (scheme in BLOCKED_SCHEMES) Route.Blocked else Route.Handoff(text)
        }
        val host = uri.host?.lowercase() ?: return Route.Blocked
        val url = if (scheme == "http") "https" + text.substring(scheme.length) else text

        if (!isDdgHost(host)) return Route.External(url)

        val path = uri.rawPath.orEmpty()
        val params = queryParams(uri.rawQuery)

        val destination = params["uddg"]
        if (path.startsWith("/l/") && destination != null && depth < MAX_REDIRECT_DEPTH) {
            return route(destination, depth + 1)
        }

        val query = params["q"]
        val isWebSearch = !query.isNullOrBlank() &&
            path in SEARCH_PATHS &&
            params["ia"].let { it == null || it == "web" } &&
            "iax" !in params &&
            !isBang(query)
        if (!isWebSearch || page.matches(host, path)) return Route.Load(url)
        return Route.Load(page.url(query!!))
    }

    private fun parse(text: String): URI? =
        try {
            URI(text)
        } catch (_: URISyntaxException) {
            null
        }

    private fun queryParams(rawQuery: String?): Map<String, String> =
        rawQuery.orEmpty()
            .split('&')
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val key = pair.substringBefore('=')
                val value = pair.substringAfter('=', missingDelimiterValue = "")
                decode(key) to decode(value)
            }

    private fun decode(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: IllegalArgumentException) {
            value
        }

    private fun isDdgHost(host: String): Boolean = host == DDG_HOST || host.endsWith(".$DDG_HOST")

    /** DDG bangs (`!w kotlin`, `kotlin !w`) only redirect from the full site. */
    private fun isBang(query: String): Boolean = query.split(WHITESPACE).any { it.startsWith("!") }

    private companion object {
        const val DDG_HOST = "duckduckgo.com"
        const val MAX_REDIRECT_DEPTH = 2
        val SEARCH_PATHS = setOf("", "/", "/lite", "/lite/", "/html", "/html/")
        val WEB_SCHEMES = setOf("http", "https")
        val BLOCKED_SCHEMES = setOf("javascript", "file", "content", "data", "blob", "about")
        val WHITESPACE = Regex("\\s+")
    }
}

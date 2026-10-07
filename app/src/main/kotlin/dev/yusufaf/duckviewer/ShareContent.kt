package dev.yusufaf.duckviewer

data class ShareContent(val url: String, val title: String?)

private val WEB_URL = Regex("^https?://[^\\s/?#]+", RegexOption.IGNORE_CASE)

/** Only web pages are worth sharing; about:, data:, javascript: and the like are not. */
fun shareContent(url: String?, title: String?): ShareContent? {
    val cleanUrl = url?.trim().orEmpty()
    if (!WEB_URL.containsMatchIn(cleanUrl)) return null
    val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() && it != cleanUrl }
    return ShareContent(cleanUrl, cleanTitle)
}

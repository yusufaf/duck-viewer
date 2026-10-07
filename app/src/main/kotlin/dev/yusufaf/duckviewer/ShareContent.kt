package dev.yusufaf.duckviewer

data class ShareContent(val url: String, val title: String?)

/**
 * Only web pages are worth sharing; about:, data:, javascript: and the like are not.
 * [titleUrl] is the url the WebView reported [title] for: WebView titles lag navigation,
 * so a title from another url (or an unknown one) is dropped rather than mislabel the page.
 */
fun shareContent(url: String?, title: String?, titleUrl: String? = url): ShareContent? {
    val cleanUrl = url?.trim().orEmpty()
    if (!WEB_URL.containsMatchIn(cleanUrl)) return null
    val cleanTitle = title?.trim()
        ?.takeIf { it.isNotEmpty() && it != cleanUrl && titleUrl?.trim() == cleanUrl }
    return ShareContent(cleanUrl, cleanTitle)
}

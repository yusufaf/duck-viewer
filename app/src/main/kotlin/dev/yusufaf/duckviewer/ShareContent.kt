package dev.yusufaf.duckviewer

data class ShareContent(val url: String, val title: String?)

/** Userinfo runs to the last '@' of the authority, as in UrlRouter.hostOf; a backslash ends the authority. */
private val USER_INFO = Regex("""^(https?://)[^/?#\\\s]*@""", RegexOption.IGNORE_CASE)

private fun String.normalized() = trim().replaceFirst(USER_INFO, "$1")

private fun String.withoutFragment() = substringBefore('#')

/**
 * Only web pages are worth sharing; about:, data:, javascript: and the like are not.
 * Credentials in the url are stripped, as browsers do before sharing.
 * [titleUrl] is the url the WebView reported [title] for: WebView titles lag navigation,
 * so a title from another page (or an unknown one) is dropped rather than mislabel the page.
 * Fragment changes stay on the same page, so they keep the title. A title that is just the
 * page's url (WebView's fallback for untitled pages) is dropped, credentials and all.
 */
fun shareContent(url: String?, title: String?, titleUrl: String? = url): ShareContent? {
    val cleanUrl = url.orEmpty().normalized()
    if (!WEB_URL.containsMatchIn(cleanUrl)) return null
    val page = cleanUrl.withoutFragment()
    val samePage = titleUrl?.normalized()?.withoutFragment() == page
    val cleanTitle = title?.trim()
        ?.takeIf { it.isNotEmpty() && samePage && it.normalized().withoutFragment() != page }
    return ShareContent(cleanUrl, cleanTitle)
}

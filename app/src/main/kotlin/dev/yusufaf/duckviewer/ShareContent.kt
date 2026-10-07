package dev.yusufaf.duckviewer

data class ShareContent(val url: String, val title: String?)

private val USER_INFO = Regex("""^(https?://)[^/?#@\s]*@""", RegexOption.IGNORE_CASE)

/**
 * Only web pages are worth sharing; about:, data:, javascript: and the like are not.
 * Credentials in the url are stripped, as browsers do before sharing.
 * [titleUrl] is the url the WebView reported [title] for: WebView titles lag navigation,
 * so a title from another page (or an unknown one) is dropped rather than mislabel the page.
 * Fragment changes stay on the same page, so they keep the title.
 */
fun shareContent(url: String?, title: String?, titleUrl: String? = url): ShareContent? {
    val cleanUrl = url?.trim().orEmpty().replaceFirst(USER_INFO, "$1")
    if (!WEB_URL.containsMatchIn(cleanUrl)) return null
    val samePage = titleUrl?.trim()?.replaceFirst(USER_INFO, "$1")?.substringBefore('#') == cleanUrl.substringBefore('#')
    val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() && it != cleanUrl && samePage }
    return ShareContent(cleanUrl, cleanTitle)
}

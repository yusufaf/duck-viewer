package dev.yusufaf.duckviewer

import android.content.Context

/** What to do with a non-DuckDuckGo link. Reader view is planned for v2. */
enum class LinkPolicy { OPEN_IN_VIEW, SHARE_ONLY }

/** Read-only for now; the settings screen that writes these comes later. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val resultsPage: ResultsPage get() = enumPref("results_page", ResultsPage.LITE)
    val linkPolicy: LinkPolicy get() = enumPref("link_policy", LinkPolicy.OPEN_IN_VIEW)
    val javaScript: Boolean get() = prefs.getBoolean("javascript", true)

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
            ?: default
}

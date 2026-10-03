package dev.yusufaf.duckviewer

sealed interface IncomingRequest {
    data class Url(val url: String) : IncomingRequest
    data class Query(val text: String) : IncomingRequest
    data object Empty : IncomingRequest

    companion object {
        private const val ACTION_VIEW = "android.intent.action.VIEW"
        private const val ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH"
        private const val ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT"
        private val WEB_URL = Regex("""^https?://\S+$""", RegexOption.IGNORE_CASE)

        /**
         * Takes the intent's parts rather than the Intent itself so this stays
         * testable on the plain JVM.
         */
        fun from(action: String?, data: String?, query: String?, processText: CharSequence?): IncomingRequest {
            val text = when (action) {
                ACTION_VIEW -> return if (data.isNullOrBlank()) Empty else Url(data)
                ACTION_WEB_SEARCH -> query
                ACTION_PROCESS_TEXT -> processText?.toString()
                else -> null
            }?.trim()

            return when {
                text.isNullOrEmpty() -> Empty
                // WEB_SEARCH's contract: an http(s) URL opens the site, anything else is searched.
                WEB_URL.matches(text) -> Url(text)
                else -> Query(text)
            }
        }
    }
}

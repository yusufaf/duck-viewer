package dev.yusufaf.duckviewer

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Lite and HTML results are laid out in tables, and a table grows to fit its
 * longest unbreakable string. Chrome won't break a URL after "/" when a digit
 * follows, so a result URL like accuweather.com/en/us/portland/97209/... can
 * widen the page past the screen. `overflow-wrap: anywhere` (unlike
 * `break-word`) lowers the table's minimum width so it fits. It's limited to
 * result text (Lite and HTML class names) so the numbering column keeps "1."
 * on one line.
 *
 * DuckDuckGo's CSP has no 'unsafe-inline' in style-src, which silently drops an
 * injected <style> element; a constructed stylesheet isn't subject to CSP.
 */
object ResultsPageStyle {
    private val ORIGINS = setOf("https://lite.duckduckgo.com", "https://html.duckduckgo.com")

    private const val SCRIPT = """
        (() => {
          const sheet = new CSSStyleSheet();
          sheet.replaceSync(
            '.result-link, .result-snippet, .link-text, .result__a, .result__snippet, .result__url' +
            ' { overflow-wrap: anywhere; }'
          );
          document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];
        })();
    """

    /** Needs JavaScript; with it off, long URLs can still overflow. */
    fun install(view: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(view, SCRIPT, ORIGINS)
        }
    }
}

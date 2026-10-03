package dev.yusufaf.duckviewer

import java.net.URLEncoder

enum class ResultsPage(val host: String, private val paths: Set<String>, private val base: String) {
    LITE("lite.duckduckgo.com", setOf("/lite", "/lite/"), "https://lite.duckduckgo.com/lite/"),
    HTML("html.duckduckgo.com", setOf("/html", "/html/"), "https://html.duckduckgo.com/html/"),
    FULL("duckduckgo.com", setOf("", "/"), "https://duckduckgo.com/");

    fun url(query: String): String = base + "?q=" + URLEncoder.encode(query, "UTF-8")

    fun matches(host: String, path: String): Boolean = host == this.host && path in paths
}

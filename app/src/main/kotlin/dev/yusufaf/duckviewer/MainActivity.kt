package dev.yusufaf.duckviewer

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var settings: Settings
    private lateinit var router: UrlRouter
    private lateinit var role: BrowserRole

    private var webView: WebView? = null
    private lateinit var progress: ProgressBar
    private lateinit var emptyState: View
    private lateinit var shareButton: View
    private lateinit var roleCard: View
    private lateinit var roleRequestButton: Button
    private lateinit var roleSettingsButton: Button

    private var roleDeclined = false

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            webView?.goBack()
        }
    }

    private val requestRole =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            roleDeclined = result.resultCode != RESULT_OK
            refreshRoleCard()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = Settings(this)
        router = UrlRouter(settings.resultsPage)
        role = BrowserRole(this)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        progress = findViewById(R.id.progress)
        emptyState = findViewById(R.id.empty_state)
        shareButton = findViewById(R.id.share_button)
        roleCard = findViewById(R.id.role_card)
        roleRequestButton = findViewById(R.id.role_request)
        roleSettingsButton = findViewById(R.id.role_settings)

        val view = attachWebView()
        if (view == null) {
            showWebViewMissing()
            return
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        findViewById<EditText>(R.id.search_box).setOnEditorActionListener { box, actionId, event ->
            // Hardware keyboards send Enter as IME_NULL with a key event instead of IME_ACTION_SEARCH.
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId != EditorInfo.IME_ACTION_SEARCH && !enter) return@setOnEditorActionListener false
            val query = box.text.toString().trim()
            if (query.isNotEmpty() && (!enter || event.action == KeyEvent.ACTION_DOWN)) open(router.forQuery(query))
            true
        }
        roleRequestButton.setOnClickListener {
            role.requestIntent()?.let(requestRole::launch)
        }
        roleSettingsButton.setOnClickListener {
            startSafely(role.settingsIntent())
        }
        shareButton.setOnClickListener { sharePage() }

        if (savedInstanceState == null || view.restoreState(savedInstanceState) == null) {
            handle(intent)
        } else {
            showWeb()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (webView != null) handle(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshRoleCard()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    private fun handle(intent: Intent) {
        logIntent(intent)
        val request = IncomingRequest.from(
            action = intent.action,
            data = intent.dataString,
            query = intent.getStringExtra(SearchManager.QUERY),
            processText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT),
        )
        when (request) {
            is IncomingRequest.Url -> open(router.route(request.url))
            is IncomingRequest.Query -> open(router.forQuery(request.text))
            // Opening from the launcher while a page is up keeps that page.
            IncomingRequest.Empty -> if (webView?.url == null) showEmpty()
        }
    }

    private fun open(route: Route) {
        when (route) {
            is Route.Load -> load(route.url)
            is Route.External -> when (settings.linkPolicy) {
                LinkPolicy.OPEN_IN_VIEW -> load(route.url)
                LinkPolicy.SHARE_ONLY -> share(route.url)
            }
            is Route.Handoff -> handOff(route.uri)
            Route.Blocked -> toast(R.string.link_blocked)
        }
    }

    private fun load(url: String) {
        showWeb()
        webView?.loadUrl(url)
    }

    private fun attachWebView(): WebView? {
        val view = createWebView() ?: return null
        findViewById<FrameLayout>(R.id.web_container).addView(view)
        webView = view
        return view
    }

    private fun createWebView(): WebView? {
        val view = try {
            WebView(this)
        } catch (e: RuntimeException) {
            // Thrown when no WebView provider is installed or it's disabled.
            Log.e(TAG, "WebView unavailable", e)
            return null
        }
        view.settings.apply {
            javaScriptEnabled = settings.javaScript
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            safeBrowsingEnabled = true
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, true)
        }
        ResultsPageStyle.install(view)
        view.setDownloadListener { _, _, _, _, _ -> toast(R.string.downloads_unsupported) }
        view.webViewClient = Client()
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.isVisible = newProgress < 100
            }
        }
        return view
    }

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val url = request.url.toString()
            // Upgrading a server redirect back to https could bounce forever on a
            // site that redirects https to http; let it fail with the cleartext error.
            if (request.isRedirect && url.startsWith("http:", ignoreCase = true)) return false
            return when (val route = router.route(url)) {
                is Route.Load -> redirectIfChanged(view, url, route.url)
                is Route.External -> when (settings.linkPolicy) {
                    LinkPolicy.OPEN_IN_VIEW -> redirectIfChanged(view, url, route.url)
                    LinkPolicy.SHARE_ONLY -> true.also { share(route.url) }
                }
                // Like Chrome, only a tap may open another app; otherwise any page
                // could launch apps or the dialer by script or redirect.
                is Route.Handoff -> true.also { if (request.hasGesture()) handOff(route.uri) }
                Route.Blocked -> true
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            progress.isVisible = true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            // Kept in sync so system back (and predictive back) closes the app once history runs out.
            backCallback.isEnabled = view.canGoBack()
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // A dead renderer leaves this WebView unusable, so swap in a new one.
            Log.w(TAG, "WebView renderer gone, crashed=${detail.didCrash()}")
            val lastUrl = view.url
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
            webView = null
            backCallback.isEnabled = false
            if (attachWebView() == null) {
                showWebViewMissing()
            } else if (detail.didCrash() || lastUrl == null) {
                // Reloading a page that crashed the renderer would just crash it again.
                showEmpty()
                toast(R.string.page_crashed)
            } else {
                load(lastUrl)
            }
            return true
        }

        private fun redirectIfChanged(view: WebView, requested: String, routed: String): Boolean {
            if (routed == requested) return false
            view.loadUrl(routed)
            return true
        }
    }

    /**
     * Opens a non-web link in another app. An intent: URI was written by a web
     * page, so it gets the same limits a browser applies: no explicit component
     * or selector, no URI permission grants, and only browsable activities.
     */
    private fun handOff(uri: String) {
        val target = if (uri.startsWith("intent:", ignoreCase = true)) {
            try {
                Intent.parseUri(uri, Intent.URI_INTENT_SCHEME).apply {
                    component = null
                    selector = null
                    flags = 0
                }
            } catch (e: URISyntaxException) {
                Log.w(TAG, "Bad intent URI", e)
                return toast(R.string.link_blocked)
            }
        } else {
            Intent(Intent.ACTION_VIEW, uri.toUri())
        }
        target.addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(target)
        } catch (_: ActivityNotFoundException) {
            val fallback = target.getStringExtra(EXTRA_BROWSER_FALLBACK_URL)
                ?.let(router::route)
                ?.takeIf { it is Route.Load || it is Route.External }
            if (fallback != null) open(fallback) else toast(R.string.no_app_for_link)
        }
    }

    private fun share(url: String, title: String? = null) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        if (title != null) send.putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TITLE, title)
        startSafely(Intent.createChooser(send, getString(R.string.share_link)))
    }

    private fun sharePage() {
        // WebView.title lags the url while a page loads, so share the url alone until it finishes.
        val page = shareContent(webView?.url, webView?.title?.takeUnless { progress.isVisible })
        if (page == null) toast(R.string.nothing_to_share) else share(page.url, page.title)
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(R.string.no_app_for_link)
        }
    }

    private fun showWeb() {
        emptyState.isVisible = false
        shareButton.isVisible = true
        webView?.isVisible = true
    }

    private fun showEmpty() {
        webView?.isVisible = false
        shareButton.isVisible = false
        emptyState.isVisible = true
        refreshRoleCard()
    }

    private fun showWebViewMissing() {
        emptyState.isVisible = false
        shareButton.isVisible = false
        findViewById<View>(R.id.webview_missing).isVisible = true
        findViewById<Button>(R.id.webview_get).setOnClickListener {
            startSafely(Intent(Intent.ACTION_VIEW, WEBVIEW_PLAY_URL.toUri()))
        }
    }

    private fun refreshRoleCard() {
        if (role.isHeld) {
            roleCard.isVisible = false
            return
        }
        roleCard.isVisible = true
        roleRequestButton.isVisible = role.isAvailable && !roleDeclined
        roleSettingsButton.isVisible = !role.isAvailable || roleDeclined
    }

    private fun logIntent(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "action=${intent.action} data=${intent.dataString} categories=${intent.categories}")
        val extras = intent.extras ?: return
        for (key in extras.keySet()) {
            @Suppress("DEPRECATION")
            Log.d(TAG, "  extra $key=${extras.get(key)}")
        }
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "DuckViewer"
        const val EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url"
        const val WEBVIEW_PLAY_URL = "market://details?id=com.google.android.webview"
    }
}

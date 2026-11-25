package com.malakaonline.tv7reals

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var cursorLayout: CursorLayout

    // HTML5 Fullscreen Video State
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Double-Back-to-Exit Guard (2 seconds)
    private var backPressedTime: Long = 0L
    private val backPressIntervalMs: Long = 2000L

    // Long-press Back key to directly exit app without multiple back clicks
    private var backKeyDownTime: Long = 0L
    private var isBackLongPressHandled: Boolean = false

    companion object {
        private const val TAG = "7ReelsTV"
        private const val TARGET_URL = "https://7reels.cc"

        // High performance Desktop Chrome User Agent for Linux to ensure wide 1080p layout
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.activity_main)

        cursorLayout = findViewById(R.id.cursorLayout)
        webView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)
        progressBar = findViewById(R.id.progressBar)

        setupBackPressHandler()
        setupWebView()

        if (savedInstanceState == null) {
            webView.loadUrl(TARGET_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    /**
     * Long-press BACK key handler:
     * Holding the TV remote BACK button for 0.8s immediately exits the app,
     * avoiding having to traverse deep web page history.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        backKeyDownTime = System.currentTimeMillis()
                        isBackLongPressHandled = false
                    } else if (event.repeatCount > 0 && !isBackLongPressHandled) {
                        if (System.currentTimeMillis() - backKeyDownTime >= 800) {
                            isBackLongPressHandled = true
                            Toast.makeText(this, "Exiting 7Reels TV...", Toast.LENGTH_SHORT).show()
                            finish()
                            return true
                        }
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isBackLongPressHandled) {
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Hides status and navigation bars for immersive full-screen landscape TV display.
     */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /**
     * Back button navigation:
     * 1. If HTML5 fullscreen video is visible, exit fullscreen first.
     * 2. Else if webView.canGoBack(), traverse web history.
     * 3. Else require double press within 2s to exit app.
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. Close HTML5 video if active
                if (customView != null) {
                    hideCustomView()
                    return
                }

                // 2. Go back in WebView history if available
                if (webView.canGoBack()) {
                    webView.goBack()
                    return
                }

                // 3. Double-back guard to prevent accidental exit on TV remotes
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < backPressIntervalMs) {
                    isEnabled = false
                    finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.exit_prompt),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    /**
     * JavaScript Interface allowing web page buttons to control native app lifecycle.
     */
    class WebAppInterface(private val activity: MainActivity) {
        @JavascriptInterface
        fun exitApp() {
            Log.d(TAG, "Exit requested via Web Interface")
            activity.runOnUiThread {
                Toast.makeText(activity, "Closing 7Reels TV...", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        // 1. Core feature enablement for modern web apps & video players
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false // Autoplay video without extra clicks
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // 2. Desktop User-Agent for wide 1080p layout
        settings.userAgentString = DESKTOP_USER_AGENT

        // 3. Popups & New Window Blocking
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = false

        // 4. Zoom & rendering optimization
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // 5. Caching & Network optimization for Pi 4
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        // Enable third-party cookies if required by embedded players/CDNs
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // Expose Native Android API to JavaScript
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidApp")

        // WebChromeClient: Block popups + support HTML5 fullscreen video
        webView.webChromeClient = object : WebChromeClient() {

            // Layer 1 Blocking: Drop all window.open() & target="_blank" popup requests silently
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                Log.d(TAG, "Suppressed popup window creation (isUserGesture=$isUserGesture)")
                return false
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                showCustomView(view, callback)
            }

            override fun onHideCustomView() {
                hideCustomView()
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "JS Console: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}")
                return true
            }
        }

        // WebViewClient: Domain locking + ad/tracker network blocking
        webView.webViewClient = object : WebViewClient() {

            // Layer 2 Blocking: Domain-Locked Navigation
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false

                if (AdBlocker.isNavigationAllowed(uri)) {
                    Log.d(TAG, "Allowed navigation to: $uri")
                    return false // Let WebView load it
                }

                Log.w(TAG, "Blocked unauthorized navigation attempt to: $uri")
                return true // Drop and block the redirect
            }

            // Layer 3 Blocking: Sub-resource Ad & Tracker Interception
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return null

                if (AdBlocker.isAdOrTracker(uri)) {
                    return AdBlocker.createEmptyResponse()
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                injectExitButton()
            }
        }

        // Ensure WebView takes focus for remote navigation
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
    }

    /**
     * Injects 'Exit' buttons into:
     * 1. The top navigation bar (for browse pages: Home, Movies, Series, My List).
     * 2. Beside the floating 'Back' button inside the video player (/watch).
     * Uses MutationObserver to persist across Next.js / SPA route changes.
     */
    private fun injectExitButton() {
        val js = """
            (function() {
                // 1. Injected Header Exit Button (Browse pages)
                function addHeaderExitBtn() {
                    if (document.getElementById('tv-exit-btn')) return;

                    var navContainer = document.querySelector('[data-nav-links="true"]');
                    var flexContainer = document.querySelector('div.flex.min-w-0.flex-1.items-center');
                    var target = navContainer || flexContainer;
                    if (!target) return;

                    var btn = document.createElement('a');
                    btn.id = 'tv-exit-btn';
                    btn.setAttribute('role', 'button');
                    btn.setAttribute('tabindex', '0');
                    btn.className = 'relative z-10 inline-flex items-center gap-1.5 rounded-lg px-3.5 py-1.5 text-sm font-semibold text-red-400 hover:text-red-300 hover:bg-red-500/20 cursor-pointer transition-colors';
                    btn.style.outline = 'none';
                    btn.style.marginLeft = '8px';
                    btn.style.userSelect = 'none';
                    btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="display:inline-block;vertical-align:middle;margin-right:4px;"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg><span>Exit</span>';

                    var styleId = 'tv-exit-btn-style';
                    if (!document.getElementById(styleId)) {
                        var st = document.createElement('style');
                        st.id = styleId;
                        st.textContent = '#tv-exit-btn:focus, #tv-exit-btn:focus-visible { outline: 2px solid #ef4444 !important; background: rgba(239, 68, 68, 0.3) !important; color: #ffffff !important; transform: scale(1.05); }';
                        document.head.appendChild(st);
                    }

                    function triggerExit(e) {
                        if (e) { e.preventDefault(); e.stopPropagation(); }
                        if (window.AndroidApp && window.AndroidApp.exitApp) {
                            window.AndroidApp.exitApp();
                        }
                    }

                    btn.addEventListener('click', triggerExit);
                    btn.addEventListener('keydown', function(e) {
                        if (e.key === 'Enter' || e.keyCode === 13) {
                            triggerExit(e);
                        }
                    });

                    target.appendChild(btn);
                }

                // 2. Injected Watch Player Exit Button (beside floating Back button)
                function addWatchExitBtn() {
                    var backBtn = document.querySelector('a[aria-label="Back"]') || 
                                  document.querySelector('a.fixed[href="/"]') ||
                                  document.querySelector('a[href="/"].group.fixed');

                    var existing = document.getElementById('tv-watch-exit-btn');
                    if (!backBtn) {
                        if (existing) existing.remove();
                        return;
                    }

                    if (!existing) {
                        existing = document.createElement('a');
                        existing.id = 'tv-watch-exit-btn';
                        existing.setAttribute('role', 'button');
                        existing.setAttribute('tabindex', '0');
                        existing.className = 'group fixed z-[120] inline-flex cursor-pointer select-none items-center gap-2 rounded-[11px] border py-2.5 pl-2.5 pr-3.5 shadow-[0_10px_28px_-10px_rgba(0,0,0,0.9)] transition-all sm:py-2 border-red-500/30 bg-[hsl(150_3%_12%)] hover:bg-red-950/40 text-red-400 hover:text-red-300';
                        existing.style.touchAction = 'manipulation';
                        existing.style.outline = 'none';
                        existing.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="h-[18px] w-[18px] text-red-400 drop-shadow-[0_1px_5px_rgba(0,0,0,0.9)]"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg><span class="text-[13px] font-semibold text-red-400">Exit</span>';

                        var watchStyleId = 'tv-watch-exit-style';
                        if (!document.getElementById(watchStyleId)) {
                            var wst = document.createElement('style');
                            wst.id = watchStyleId;
                            wst.textContent = '#tv-watch-exit-btn:focus, #tv-watch-exit-btn:focus-visible { outline: 2px solid #ef4444 !important; background: rgba(239, 68, 68, 0.3) !important; color: #ffffff !important; transform: scale(1.05); }';
                            document.head.appendChild(wst);
                        }

                        function triggerExit(e) {
                            if (e) { e.preventDefault(); e.stopPropagation(); }
                            if (window.AndroidApp && window.AndroidApp.exitApp) {
                                window.AndroidApp.exitApp();
                            }
                        }

                        existing.addEventListener('click', triggerExit);
                        existing.addEventListener('keydown', function(e) {
                            if (e.key === 'Enter' || e.keyCode === 13) {
                                triggerExit(e);
                            }
                        });

                        document.body.appendChild(existing);
                    }

                    // Dynamically position exactly to the right of the Back button
                    var rect = backBtn.getBoundingClientRect();
                    if (rect.width > 0) {
                        existing.style.position = 'fixed';
                        existing.style.top = rect.top + 'px';
                        existing.style.left = (rect.right + 12) + 'px';
                        existing.style.zIndex = '120';
                    }
                }

                function checkAllButtons() {
                    addHeaderExitBtn();
                    addWatchExitBtn();
                }

                checkAllButtons();

                if (!window.__7reels_observer_set) {
                    window.__7reels_observer_set = true;
                    var observer = new MutationObserver(checkAllButtons);
                    if (document.body) {
                        observer.observe(document.body, { childList: true, subtree: true });
                    }
                    setInterval(checkAllButtons, 1200);
                    window.addEventListener('resize', checkAllButtons);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * Expands HTML5 video to fullscreen TV display.
     */
    private fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }

        customView = view
        customViewCallback = callback

        webView.visibility = View.GONE
        customViewContainer.apply {
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            visibility = View.VISIBLE
            bringToFront()
            requestFocus()
        }
        hideSystemBars()
    }

    /**
     * Restores normal WebView layout from fullscreen video.
     */
    private fun hideCustomView() {
        if (customView == null) return

        customViewContainer.apply {
            visibility = View.GONE
            removeAllViews()
        }
        webView.visibility = View.VISIBLE
        webView.requestFocus()

        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
        hideSystemBars()
    }
}

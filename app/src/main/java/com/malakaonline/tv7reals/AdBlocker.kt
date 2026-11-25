package com.malakaonline.tv7reals

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

object AdBlocker {

    private const val TAG = "AdBlocker"

    // Primary target domain
    private const val PRIMARY_DOMAIN = "7reels.cc"

    /**
     * Whitelist for top-level navigation and iframe/player hosts.
     * Any top-level navigation to hosts NOT matching PRIMARY_DOMAIN or ALLOWED_HOSTS
     * will be strictly blocked to prevent ad-redirects hijacking the TV screen.
     */
    val ALLOWED_HOSTS = hashSetOf(
        "7reels.cc",
        "www.7reels.cc",
        "m.7reels.cc",
        // Common video CDN / streaming hosts used by web video embeds
        "streamtape.com",
        "doodstream.com",
        "dood.to",
        "dood.so",
        "vidsrc.me",
        "vidsrc.to",
        "vidplay.site",
        "megacloud.tv",
        "rabbitstream.net",
        "streamwish.to",
        "filemoon.sx",
        "mixdrop.co",
        "streamlare.com",
        "upstream.to",
        "mp4upload.com",
        // Cloudflare and Google CDN resources for web infrastructure
        "cloudflare.com",
        "cdnjs.cloudflare.com",
        "ajax.googleapis.com",
        "fonts.googleapis.com",
        "fonts.gstatic.com"
    )

    /**
     * Known ad, popunder, redirect, and telemetry domains.
     */
    private val BLOCKED_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adnxs.com",
        "popads.net",
        "popcash.net",
        "adsterra.com",
        "propellerads.com",
        "exoclick.com",
        "trafficjunky.com",
        "hilltopads.net",
        "monetag.com",
        "clickadu.com",
        "admaven.com",
        "juicyads.com",
        "zergnet.com",
        "mgid.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "scorecardresearch.com",
        "histats.com",
        "whos.amung.us",
        "statcounter.com",
        "hotjar.com",
        "clarity.ms",
        "yandex.ru",
        "mc.yandex.ru"
    )

    /**
     * Substring patterns in path or query parameters that denote ad/popup scripts or trackers.
     */
    private val BLOCKED_URL_PATTERNS = arrayOf(
        "/ads/",
        "/ads.",
        "/ad.",
        "ad_provider",
        "ad-provider",
        "/popunder",
        "/popup",
        "pop_under",
        "banner_id=",
        "zoneid=",
        "ad_type=",
        "analytics.js",
        "gtag/js",
        "adsystem",
        "adserver",
        "deliver.html"
    )

    /**
     * Checks if top-level navigation to the given URI is permitted.
     * Allows 7reels.cc, *.7reels.cc, and explicitly whitelisted CDN/video hosts.
     */
    fun isNavigationAllowed(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme?.lowercase() ?: return false

        // Block non-web intents (intent://, market://, tg://, etc.)
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "Blocked non-web scheme: $scheme in $uri")
            return false
        }

        val host = uri.host?.lowercase() ?: return false

        // Check if primary domain or subdomain of 7reels.cc
        if (host == PRIMARY_DOMAIN || host.endsWith(".$PRIMARY_DOMAIN")) {
            return true
        }

        // Check against whitelisted streaming / player hosts
        for (allowed in ALLOWED_HOSTS) {
            if (host == allowed || host.endsWith(".$allowed")) {
                return true
            }
        }

        Log.w(TAG, "Blocked external top-level navigation to: $host")
        return false
    }

    /**
     * Checks if a sub-resource request (scripts, iframes, images, tracking pixels)
     * matches known ad networks or popup injection patterns.
     */
    fun isAdOrTracker(uri: Uri?): Boolean {
        if (uri == null) return false
        val host = uri.host?.lowercase() ?: ""
        val urlString = uri.toString().lowercase()

        // 1. Check exact or wildcard domain match
        for (blocked in BLOCKED_DOMAINS) {
            if (host == blocked || host.endsWith(".$blocked")) {
                Log.d(TAG, "Blocked ad domain: $host")
                return true
            }
        }

        // 2. Check URL keyword patterns
        for (pattern in BLOCKED_URL_PATTERNS) {
            if (urlString.contains(pattern)) {
                Log.d(TAG, "Blocked ad pattern [$pattern] in URL: $urlString")
                return true
            }
        }

        return false
    }

    /**
     * Creates a dummy empty WebResourceResponse to silently neutralize blocked resources.
     */
    fun createEmptyResponse(): WebResourceResponse {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        val response = WebResourceResponse("text/plain", StandardCharsets.UTF_8.name(), emptyStream)
        response.setResponseHeaders(
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        )
        return response
    }
}

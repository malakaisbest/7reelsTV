package com.malakaonline.tv7reals

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var cursorLayout: CursorLayout

    // Native TV Mode Views
    private lateinit var nativeTvContainer: View
    private lateinit var browseContainer: View
    private lateinit var detailsContainer: View

    // Browse Screen Views
    private lateinit var heroBackdrop: ImageView
    private lateinit var heroTitle: TextView
    private lateinit var heroMeta: TextView
    private lateinit var heroOverview: TextView
    private lateinit var btnHeroWatch: Button
    private lateinit var recyclerBottomTrending: RecyclerView
    private lateinit var recyclerRightMovies: RecyclerView
    private lateinit var recyclerRightTv: RecyclerView
    private lateinit var nativeLoading: ProgressBar
    private lateinit var btnNativeSearch: Button
    private lateinit var btnModeSettings: Button
    private lateinit var btnNativeExit: Button

    // Movie Details Screen Views
    private lateinit var detailsBackdrop: ImageView
    private lateinit var detailsPoster: ImageView
    private lateinit var detailsTitle: TextView
    private lateinit var detailsTagline: TextView
    private lateinit var detailsMeta: TextView
    private lateinit var detailsGenres: TextView
    private lateinit var detailsOverview: TextView
    private lateinit var btnDetailsBack: Button
    private lateinit var btnDetailsExit: Button
    private lateinit var recyclerServers: RecyclerView
    private lateinit var layoutTvEpisodes: LinearLayout
    private lateinit var recyclerSeasons: RecyclerView
    private lateinit var recyclerEpisodes: RecyclerView
    private lateinit var layoutMovieExtras: LinearLayout
    private lateinit var recyclerSimilarMovies: RecyclerView
    private lateinit var btnPlayNow: Button
    private lateinit var tvPlayStatus: TextView
    private lateinit var recyclerCast: RecyclerView
    private lateinit var detailsLoading: ProgressBar
    private lateinit var textBottomRowTitle: TextView
    private lateinit var btnToggleBottomRow: Button
    private lateinit var recyclerContinueWatching: RecyclerView

    private var currentHeroMovie: MovieItem? = null
    private var currentDetailMovie: MovieItem? = null
    private var featuredQueue: List<MovieItem> = emptyList()
    private var currentFeaturedIndex: Int = 0

    private var trendingAdapter: TrendingCompactAdapter? = null
    private var similarAdapter: TrendingCompactAdapter? = null
    private var continueWatchingAdapter: ContinueWatchingAdapter? = null
    private var isShowingRecents: Boolean = false
    private var currentSeekTime: Long = 0L
    private var moviesColumnAdapter: ColumnMovieAdapter? = null
    private var tvColumnAdapter: ColumnMovieAdapter? = null
    private var castAdapter: CastAdapter? = null
    private var serverAdapter: ServerAdapter? = null
    private var seasonAdapter: SeasonAdapter? = null
    private var episodeAdapter: EpisodeAdapter? = null

    private var selectedServer: StreamServer = ServerConfig.AVAILABLE_SERVERS[0]
    private var selectedSeasonNum: Int = 1
    private var selectedEpisodeNum: Int = 1

    // 5-second Auto-Rotation for Browse Queue
    private val autoRotateHandler = Handler(Looper.getMainLooper())
    private val autoRotateRunnable = object : Runnable {
        override fun run() {
            if (currentAppMode == AppPreferences.MODE_NATIVE && 
                browseContainer.visibility == View.VISIBLE && 
                featuredQueue.isNotEmpty()) {
                val nextIndex = (currentFeaturedIndex + 1) % featuredQueue.size
                selectFeaturedMovieByIndex(nextIndex, fromAuto = true)
            }
            autoRotateHandler.postDelayed(this, 5000L)
        }
    }

    // App Mode State
    private var currentAppMode: String = AppPreferences.MODE_WEBVIEW
    private var isWatchingFromNative: Boolean = false

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

        initNativeTvViews()
        setupBackPressHandler()
        setupWebView()

        val savedMode = AppPreferences.getAppMode(this)
        if (savedMode == null) {
            showModeSelectionDialog(isFirstRun = true)
        } else {
            switchAppMode(savedMode)
        }
    }

    private fun initNativeTvViews() {
        nativeTvContainer = findViewById(R.id.nativeTvContainer)
        browseContainer = findViewById(R.id.browseContainer)
        detailsContainer = findViewById(R.id.detailsContainer)

        // 1. Browse Screen Views
        heroBackdrop = findViewById(R.id.heroBackdrop)
        heroTitle = findViewById(R.id.heroTitle)
        heroMeta = findViewById(R.id.heroMeta)
        heroOverview = findViewById(R.id.heroOverview)
        btnHeroWatch = findViewById(R.id.btnHeroWatch)
        recyclerBottomTrending = findViewById(R.id.recyclerBottomTrending)
        recyclerRightMovies = findViewById(R.id.recyclerRightMovies)
        recyclerRightTv = findViewById(R.id.recyclerRightTv)
        nativeLoading = findViewById(R.id.nativeLoading)
        btnNativeSearch = findViewById(R.id.btnNativeSearch)
        btnModeSettings = findViewById(R.id.btnModeSettings)
        btnNativeExit = findViewById(R.id.btnNativeExit)

        // 2. Movie Details Screen Views
        detailsBackdrop = findViewById(R.id.detailsBackdrop)
        detailsPoster = findViewById(R.id.detailsPoster)
        detailsTitle = findViewById(R.id.detailsTitle)
        detailsTagline = findViewById(R.id.detailsTagline)
        detailsMeta = findViewById(R.id.detailsMeta)
        detailsGenres = findViewById(R.id.detailsGenres)
        detailsOverview = findViewById(R.id.detailsOverview)
        btnDetailsBack = findViewById(R.id.btnDetailsBack)
        btnDetailsExit = findViewById(R.id.btnDetailsExit)
        recyclerServers = findViewById(R.id.recyclerServers)
        layoutTvEpisodes = findViewById(R.id.layoutTvEpisodes)
        layoutMovieExtras = findViewById(R.id.layoutMovieExtras)
        recyclerSeasons = findViewById(R.id.recyclerSeasons)
        recyclerEpisodes = findViewById(R.id.recyclerEpisodes)
        recyclerSimilarMovies = findViewById(R.id.recyclerSimilarMovies)
        btnPlayNow = findViewById(R.id.btnPlayNow)
        tvPlayStatus = findViewById(R.id.tvPlayStatus)
        recyclerCast = findViewById(R.id.recyclerCast)
        detailsLoading = findViewById(R.id.detailsLoading)

        textBottomRowTitle = findViewById(R.id.textBottomRowTitle)
        btnToggleBottomRow = findViewById(R.id.btnToggleBottomRow)
        recyclerContinueWatching = findViewById(R.id.recyclerContinueWatching)

        // Header Actions
        btnNativeSearch.setOnClickListener { showSearchDialog() }
        btnModeSettings.setOnClickListener { showModeSelectionDialog(isFirstRun = false) }
        btnNativeExit.setOnClickListener { finish() }
        btnDetailsExit.setOnClickListener { finish() }
        btnDetailsBack.setOnClickListener { closeMovieDetails() }

        // Hero Button opens Movie Details & Server List (No auto play)
        btnHeroWatch.setOnClickListener {
            currentHeroMovie?.let { openMovieDetails(it) }
        }

        // Setup Browse RecyclerViews
        recyclerBottomTrending.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        trendingAdapter = TrendingCompactAdapter(
            emptyList(),
            onItemFocused = { item -> selectMovieDirectly(item) },
            onItemClick = { item -> openMovieDetails(item) }
        )
        recyclerBottomTrending.adapter = trendingAdapter

        recyclerContinueWatching.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        continueWatchingAdapter = ContinueWatchingAdapter(
            emptyList(),
            onItemFocused = { item -> selectMovieDirectly(item.toMovieItem()) },
            onItemClick = { item -> promptResumeOrStart(item.toMovieItem(), item) }
        )
        recyclerContinueWatching.adapter = continueWatchingAdapter

        btnToggleBottomRow.setOnClickListener {
            toggleBottomRow()
        }

        recyclerRightMovies.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        moviesColumnAdapter = ColumnMovieAdapter(
            emptyList(),
            onItemFocused = { item, pos -> selectMovieDirectly(item) },
            onItemClick = { item -> openMovieDetails(item) }
        )
        recyclerRightMovies.adapter = moviesColumnAdapter

        recyclerRightTv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        tvColumnAdapter = ColumnMovieAdapter(
            emptyList(),
            onItemFocused = { item, pos -> selectMovieDirectly(item) },
            onItemClick = { item -> openMovieDetails(item) }
        )
        recyclerRightTv.adapter = tvColumnAdapter

        // Setup Details RecyclerViews
        recyclerServers.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(this, 3)
        serverAdapter = ServerAdapter(ServerConfig.AVAILABLE_SERVERS) { server ->
            selectedServer = server
            serverAdapter?.setActiveServer(server.id)
            updatePlayStatus()
        }
        recyclerServers.adapter = serverAdapter

        recyclerSimilarMovies.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        similarAdapter = TrendingCompactAdapter(
            emptyList(),
            onItemFocused = {},
            onItemClick = { item -> openMovieDetails(item) }
        )
        recyclerSimilarMovies.adapter = similarAdapter

        recyclerSeasons.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        seasonAdapter = SeasonAdapter(emptyList()) { season ->
            selectedSeasonNum = season.seasonNumber
            seasonAdapter?.setActiveSeason(season.seasonNumber)
            currentDetailMovie?.let { movie ->
                loadSeasonEpisodes(movie.id, season.seasonNumber)
            }
        }
        recyclerSeasons.adapter = seasonAdapter

        recyclerEpisodes.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        episodeAdapter = EpisodeAdapter(emptyList()) { ep ->
            selectedEpisodeNum = ep.episodeNumber
            episodeAdapter?.setActiveEpisode(ep.episodeNumber)
            updatePlayStatus()
            // Check if this episode has saved timeline progress
            currentDetailMovie?.let { movie ->
                val history = WatchHistoryManager.getProgress(this, movie.id, movie.mediaType, selectedSeasonNum, ep.episodeNumber)
                if (history != null && history.positionSeconds > 15L && (history.durationSeconds == 0L || history.positionSeconds < history.durationSeconds - 60L)) {
                    promptResumeOrStart(movie, history)
                } else {
                    executePlayback(movie, selectedSeasonNum, ep.episodeNumber, resumeAt = 0L)
                }
            }
        }
        recyclerEpisodes.adapter = episodeAdapter

        btnPlayNow.setOnClickListener {
            playCurrentSelection()
        }

        recyclerCast.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        castAdapter = CastAdapter(emptyList())
        recyclerCast.adapter = castAdapter
    }

    private fun updatePlayStatus() {
        val movie = currentDetailMovie ?: return
        if (movie.mediaType == "tv") {
            tvPlayStatus.text = "${selectedServer.name}  •  S${selectedSeasonNum} E${selectedEpisodeNum}"
            btnPlayNow.text = "▶ Watch S${selectedSeasonNum} E${selectedEpisodeNum}"
        } else {
            tvPlayStatus.text = "${selectedServer.name}  •  Full Movie"
            btnPlayNow.text = "▶ Watch Movie"
        }
    }

    private fun loadSeasonEpisodes(tvId: Long, seasonNum: Int) {
        lifecycleScope.launch {
            try {
                val eps = MovieRepository.getSeasonEpisodes(tvId, seasonNum)
                if (eps.isNotEmpty()) {
                    episodeAdapter?.updateEpisodes(eps, selectedEpisode = 1)
                    selectedEpisodeNum = eps.first().episodeNumber
                    updatePlayStatus()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playCurrentSelection() {
        val movie = currentDetailMovie ?: return
        val history = WatchHistoryManager.getProgress(this, movie.id, movie.mediaType, selectedSeasonNum, selectedEpisodeNum)
        if (history != null && history.positionSeconds > 15L && (history.durationSeconds == 0L || history.positionSeconds < history.durationSeconds - 60L)) {
            promptResumeOrStart(movie, history)
        } else {
            executePlayback(movie, selectedSeasonNum, selectedEpisodeNum, resumeAt = 0L)
        }
    }

    private fun promptResumeOrStart(movie: MovieItem, history: WatchHistoryItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_resume_prompt, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val textTitle = dialogView.findViewById<TextView>(R.id.textResumeTitle)
        val textSubtitle = dialogView.findViewById<TextView>(R.id.textResumeSubtitle)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progressResume)
        val btnStartOver = dialogView.findViewById<Button>(R.id.btnResumeStartOver)
        val btnResume = dialogView.findViewById<Button>(R.id.btnResumePlay)

        val episodeLabel = if (history.mediaType == "tv") " • S${history.season} E${history.episode}" else ""
        textTitle.text = "${history.title}$episodeLabel"
        val remaining = history.getRemainingTime()
        val remStr = if (remaining.isNotEmpty()) " • $remaining" else ""
        textSubtitle.text = "You left off at ${history.getFormattedPosition()}$remStr (${history.getProgressPercent()}%). How would you like to watch?"
        progress.progress = history.getProgressPercent()

        btnResume.text = "⏩ Resume at ${history.getFormattedPosition()}"
        btnResume.setOnClickListener {
            dialog.dismiss()
            executePlayback(movie, history.season, history.episode, resumeAt = history.positionSeconds)
        }

        btnStartOver.setOnClickListener {
            dialog.dismiss()
            executePlayback(movie, history.season, history.episode, resumeAt = 0L)
        }

        dialog.show()
        btnResume.requestFocus()
    }

    private fun executePlayback(movie: MovieItem, season: Int, episode: Int, resumeAt: Long) {
        stopAutoRotateTimer()
        isWatchingFromNative = true
        currentSeekTime = resumeAt
        nativeTvContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE

        val baseStreamUrl = ServerConfig.getStreamUrl(
            selectedServer.id,
            movie.id,
            movie.mediaType,
            season,
            episode
        )
        val finalUrl = if (resumeAt > 0L) "$baseStreamUrl#t=$resumeAt" else baseStreamUrl
        Log.d(TAG, "Playing stream via ${selectedServer.name}: $finalUrl (resumeAt: $resumeAt)")
        val itemLabel = if (movie.mediaType == "tv") "S${season} E${episode}" else "Movie"
        val resumeMsg = if (resumeAt > 0L) " [Resuming at ${formatTimeSeconds(resumeAt)}]" else ""
        Toast.makeText(this, "▶ OK: Play • ◄/►: Seek • ▲ UP: Subtitles • ▼ DOWN: Mouse Cursor", Toast.LENGTH_LONG).show()
        webView.loadUrl(finalUrl)
        webView.requestFocus()
    }

    private fun formatTimeSeconds(seconds: Long): String {
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val h = seconds / 3600
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun openMovieDetails(item: MovieItem) {
        currentDetailMovie = item
        stopAutoRotateTimer()

        // Reset server and season/episode state
        selectedServer = ServerConfig.AVAILABLE_SERVERS[0]
        serverAdapter?.setActiveServer(selectedServer.id)
        selectedSeasonNum = 1
        selectedEpisodeNum = 1

        // Switch containers
        browseContainer.visibility = View.GONE
        detailsContainer.visibility = View.VISIBLE

        // Populate initial data
        detailsTitle.text = item.title
        detailsTagline.text = ""
        detailsOverview.text = item.overview.ifEmpty { "High definition stream available on 7Reels TV." }
        detailsGenres.text = ""

        val year = item.releaseDate.split("-").firstOrNull() ?: ""
        val ratingStr = if (item.voteAverage > 0) "★ ${String.format("%.1f", item.voteAverage)}  •  " else ""
        detailsMeta.text = "$ratingStr$year  •  ${item.mediaType.uppercase()}"

        detailsPoster.load(item.getPosterUrl()) { crossfade(true) }
        detailsBackdrop.load(item.getBackdropUrl()) { crossfade(true) }

        if (item.mediaType == "tv") {
            layoutMovieExtras.visibility = View.GONE
            layoutTvEpisodes.visibility = View.VISIBLE
            updatePlayStatus()
        } else {
            layoutTvEpisodes.visibility = View.GONE
            layoutMovieExtras.visibility = View.VISIBLE
            updatePlayStatus()

            // Fetch and display Similar Movies to fill right pane
            similarAdapter?.updateItems(emptyList())
            lifecycleScope.launch {
                try {
                    val similar = MovieRepository.getSimilar(item.id, item.mediaType)
                    val list = if (similar.isNotEmpty()) {
                        similar.take(12)
                    } else {
                        featuredQueue.filter { it.id != item.id }.take(10)
                    }
                    similarAdapter?.updateItems(list)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Fetch deep details & actors asynchronously
        detailsLoading.visibility = View.VISIBLE
        castAdapter?.updateItems(emptyList())

        lifecycleScope.launch {
            try {
                val detail = MovieRepository.getMovieDetails(item.id, item.mediaType)
                detailsLoading.visibility = View.GONE
                if (detail != null) {
                    if (detail.tagline.isNotEmpty()) {
                        detailsTagline.text = "\"${detail.tagline}\""
                        detailsTagline.visibility = View.VISIBLE
                    } else {
                        detailsTagline.visibility = View.GONE
                    }

                    val runtimeStr = detail.getFormattedRuntime().takeIf { it.isNotEmpty() }?.let { "  •  $it" } ?: ""
                    detailsMeta.text = "$ratingStr$year  •  ${detail.mediaType.uppercase()}$runtimeStr"

                    if (detail.genres.isNotEmpty()) {
                        detailsGenres.text = "Genres: " + detail.genres.joinToString(", ")
                        detailsGenres.visibility = View.VISIBLE
                    }

                    if (detail.overview.isNotEmpty()) {
                        detailsOverview.text = detail.overview
                    }

                    castAdapter?.updateItems(detail.cast)

                    if (item.mediaType == "tv") {
                        val validSeasons = detail.seasons.filter { it.seasonNumber > 0 }.ifEmpty { detail.seasons }
                        seasonAdapter?.updateSeasons(validSeasons, selectedSeason = 1)
                        loadSeasonEpisodes(item.id, 1)
                    }
                }
            } catch (e: Exception) {
                detailsLoading.visibility = View.GONE
                e.printStackTrace()
            }
        }

        btnPlayNow.requestFocus()
    }

    private fun closeMovieDetails() {
        detailsContainer.visibility = View.GONE
        browseContainer.visibility = View.VISIBLE
        if (currentAppMode == AppPreferences.MODE_NATIVE) {
            startAutoRotateTimer()
        }
        btnHeroWatch.requestFocus()
    }

    private fun selectMovieDirectly(item: MovieItem) {
        val idx = featuredQueue.indexOfFirst { it.id == item.id }
        if (idx != -1) {
            currentFeaturedIndex = idx
        }
        updateHeroSection(item)
        preloadAhead(currentFeaturedIndex)
    }

    private fun selectFeaturedMovieByIndex(index: Int, fromAuto: Boolean) {
        if (index !in featuredQueue.indices) return
        currentFeaturedIndex = index
        val item = featuredQueue[index]
        updateHeroSection(item)
        preloadAhead(index)
    }

    private fun updateHeroSection(item: MovieItem) {
        currentHeroMovie = item
        heroTitle.text = item.title
        val year = item.releaseDate.split("-").firstOrNull() ?: ""
        val ratingStr = if (item.voteAverage > 0) "★ ${String.format("%.1f", item.voteAverage)}  •  " else ""
        heroMeta.text = "$ratingStr$year  •  ${item.mediaType.uppercase()}"
        heroOverview.text = item.overview.ifEmpty { "Stream high quality video with fast remote navigation on 7Reels TV." }

        heroBackdrop.load(item.getBackdropUrl()) {
            crossfade(true)
        }
    }

    private fun preloadAhead(fromIndex: Int) {
        if (featuredQueue.isEmpty()) return
        val count = minOf(10, featuredQueue.size)
        for (step in 1..count) {
            val idx = (fromIndex + step) % featuredQueue.size
            val item = featuredQueue[idx]
            val backdropUrl = item.getBackdropUrl()
            val posterUrl = item.getPosterUrl()

            if (backdropUrl.isNotEmpty()) {
                val req = ImageRequest.Builder(this)
                    .data(backdropUrl)
                    .build()
                imageLoader.enqueue(req)
            }
            if (posterUrl.isNotEmpty()) {
                val req = ImageRequest.Builder(this)
                    .data(posterUrl)
                    .build()
                imageLoader.enqueue(req)
            }
        }
    }

    private fun loadNativeData() {
        nativeLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val movies = MovieRepository.getTrendingMovies()
                val tvShows = MovieRepository.getTrendingTv()

                val combined = mutableListOf<MovieItem>()
                combined.addAll(movies)
                combined.addAll(tvShows)
                featuredQueue = combined

                trendingAdapter?.updateItems(combined.shuffled().take(15))
                moviesColumnAdapter?.updateItems(movies)
                tvColumnAdapter?.updateItems(tvShows)
                refreshContinueWatching()

                if (featuredQueue.isNotEmpty() && currentHeroMovie == null) {
                    selectFeaturedMovieByIndex(0, fromAuto = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                nativeLoading.visibility = View.GONE
            }
        }
    }

    private fun startAutoRotateTimer() {
        autoRotateHandler.removeCallbacks(autoRotateRunnable)
        autoRotateHandler.postDelayed(autoRotateRunnable, 5000L)
    }

    private fun stopAutoRotateTimer() {
        autoRotateHandler.removeCallbacks(autoRotateRunnable)
    }

    private fun resetAutoRotateTimer() {
        stopAutoRotateTimer()
        startAutoRotateTimer()
    }

    fun switchAppMode(mode: String) {
        currentAppMode = mode
        AppPreferences.setAppMode(this, mode)

        if (mode == AppPreferences.MODE_NATIVE) {
            webView.visibility = View.GONE
            cursorLayout.toggleCursorMode(false)
            nativeTvContainer.visibility = View.VISIBLE
            browseContainer.visibility = View.VISIBLE
            detailsContainer.visibility = View.GONE
            loadNativeData()
            startAutoRotateTimer()
            btnHeroWatch.requestFocus()
        } else {
            stopAutoRotateTimer()
            nativeTvContainer.visibility = View.GONE
            webView.visibility = View.VISIBLE
            if (webView.url == null || webView.url == "about:blank") {
                webView.loadUrl(TARGET_URL)
            }
            webView.requestFocus()
        }
    }

    fun showModeSelectionDialog(isFirstRun: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mode_select, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(!isFirstRun)
            .create()

        val cardNative = dialogView.findViewById<View>(R.id.cardNativeMode)
        val cardWeb = dialogView.findViewById<View>(R.id.cardWebViewMode)

        cardNative.setOnClickListener {
            switchAppMode(AppPreferences.MODE_NATIVE)
            dialog.dismiss()
        }

        cardWeb.setOnClickListener {
            switchAppMode(AppPreferences.MODE_WEBVIEW)
            dialog.dismiss()
        }

        dialog.show()
        cardNative.requestFocus()
    }

    fun showSearchDialog() {
        stopAutoRotateTimer()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val editQuery = dialogView.findViewById<android.widget.EditText>(R.id.editSearchQuery)
        val btnSearch = dialogView.findViewById<Button>(R.id.btnSearchExecute)
        val btnClose = dialogView.findViewById<Button>(R.id.btnSearchClose)
        val searchProgress = dialogView.findViewById<ProgressBar>(R.id.searchProgress)
        val textStatus = dialogView.findViewById<TextView>(R.id.textSearchStatus)
        val recyclerResults = dialogView.findViewById<RecyclerView>(R.id.recyclerSearchResults)

        recyclerResults.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val searchAdapter = MovieAdapter(
            emptyList(),
            onItemFocused = {},
            onItemClick = { item ->
                dialog.dismiss()
                openMovieDetails(item)
            }
        )
        recyclerResults.adapter = searchAdapter

        fun performSearch() {
            val q = editQuery.text.toString().trim()
            if (q.isEmpty()) return

            searchProgress.visibility = View.VISIBLE
            textStatus.visibility = View.GONE
            recyclerResults.visibility = View.GONE

            lifecycleScope.launch {
                try {
                    val results = MovieRepository.searchMulti(q)
                    searchProgress.visibility = View.GONE
                    if (results.isEmpty()) {
                        textStatus.text = "No results found for \"$q\""
                        textStatus.visibility = View.VISIBLE
                        recyclerResults.visibility = View.GONE
                    } else {
                        textStatus.visibility = View.GONE
                        searchAdapter.updateItems(results)
                        recyclerResults.visibility = View.VISIBLE
                        recyclerResults.requestFocus()
                    }
                } catch (e: Exception) {
                    searchProgress.visibility = View.GONE
                    textStatus.text = "Search error: ${e.message}"
                    textStatus.visibility = View.VISIBLE
                }
            }
        }

        btnSearch.setOnClickListener { performSearch() }
        btnClose.setOnClickListener { dialog.dismiss() }

        editQuery.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch()
                true
            } else false
        }

        dialog.setOnDismissListener {
            if (currentAppMode == AppPreferences.MODE_NATIVE && browseContainer.visibility == View.VISIBLE) {
                startAutoRotateTimer()
            }
        }

        dialog.show()
        editQuery.requestFocus()
    }

    private fun refreshContinueWatching() {
        val history = WatchHistoryManager.getHistory(this)
        if (history.isNotEmpty()) {
            continueWatchingAdapter?.updateItems(history)
            btnToggleBottomRow.visibility = View.VISIBLE
            showContinueWatchingRow(true)
        } else {
            btnToggleBottomRow.visibility = View.GONE
            showContinueWatchingRow(false)
        }
    }

    private fun showContinueWatchingRow(showRecents: Boolean) {
        isShowingRecents = showRecents
        if (showRecents) {
            textBottomRowTitle.text = "⏱️ Continue Watching"
            btnToggleBottomRow.text = "🔥 Trending"
            recyclerContinueWatching.visibility = View.VISIBLE
            recyclerBottomTrending.visibility = View.GONE
        } else {
            textBottomRowTitle.text = "🔥 Trending Now"
            btnToggleBottomRow.text = "⏱️ Recents"
            recyclerContinueWatching.visibility = View.GONE
            recyclerBottomTrending.visibility = View.VISIBLE
        }
    }

    private fun toggleBottomRow() {
        showContinueWatchingRow(!isShowingRecents)
    }

    fun handleWatchProgress(cur: Long, dur: Long) {
        val movie = currentDetailMovie ?: return
        if (cur <= 5L) return
        val item = WatchHistoryItem(
            tmdbId = movie.id,
            mediaType = movie.mediaType,
            title = movie.title,
            posterPath = movie.posterPath,
            backdropPath = movie.backdropPath,
            season = selectedSeasonNum,
            episode = selectedEpisodeNum,
            episodeTitle = null,
            positionSeconds = cur,
            durationSeconds = dur,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        WatchHistoryManager.saveProgress(this, item)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        webView.onResume()
        if (currentAppMode == AppPreferences.MODE_NATIVE && browseContainer.visibility == View.VISIBLE) {
            startAutoRotateTimer()
            refreshContinueWatching()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoRotateTimer()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRotateTimer()
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            Log.d(TAG, "dispatchKeyEvent UP: keyCode=${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)})")
        }

        // Reset 5s auto-rotate timer on any user remote input
        if (currentAppMode == AppPreferences.MODE_NATIVE && browseContainer.visibility == View.VISIBLE) {
            resetAutoRotateTimer()
        }

        // Menu key on remote opens mode selection dialog
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            showModeSelectionDialog(isFirstRun = false)
            return true
        }

        // Subtitle / Captions button or Color buttons (Yellow/Blue) on TV remote
        val isSubtitleKey = event.keyCode == KeyEvent.KEYCODE_CAPTIONS ||
                            event.keyCode == KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK ||
                            event.keyCode == 175 || // KEYCODE_CAPTIONS explicit fallback
                            event.keyCode == 233 || // KEYCODE_TV_TELETEXT
                            event.keyCode == KeyEvent.KEYCODE_PROG_YELLOW ||
                            event.keyCode == KeyEvent.KEYCODE_PROG_BLUE ||
                            event.keyCode == 185 || // KEYCODE_PROG_YELLOW fallback
                            event.keyCode == 186    // KEYCODE_PROG_BLUE fallback
        if (isSubtitleKey && event.action == KeyEvent.ACTION_UP) {
            toggleSubtitlesAction()
            return true
        }

        // Remote navigation during video playback (from Native mode or custom video view)
        if (isWatchingFromNative || customView != null) {
            val keyCode = event.keyCode
            // 1. Remote OK / Center / Enter / Play-Pause: Direct Click on Web Video Player
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (event.action == KeyEvent.ACTION_UP) {
                    if (!cursorLayout.isCursorMode) {
                        // Dispatch physical touch event at center of screen to trigger HTML5 play button
                        cursorLayout.dispatchCenterClick()
                        // Also trigger JS toggle play/pause handler
                        webView.evaluateJavascript("window.__7reels_toggle_play && window.__7reels_toggle_play();", null)
                        return true
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                if (event.action == KeyEvent.ACTION_DOWN && !cursorLayout.isCursorMode) {
                    webView.evaluateJavascript("window.__7reels_seek && window.__7reels_seek(-10);", null)
                    Toast.makeText(this, "⏪ -10s", Toast.LENGTH_SHORT).show()
                    return true
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                if (event.action == KeyEvent.ACTION_DOWN && !cursorLayout.isCursorMode) {
                    webView.evaluateJavascript("window.__7reels_seek && window.__7reels_seek(10);", null)
                    Toast.makeText(this, "⏩ +10s", Toast.LENGTH_SHORT).show()
                    return true
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // D-Pad UP on TV remote: Instantly open Subtitle / CC menu during video playback!
                if (event.action == KeyEvent.ACTION_UP && !cursorLayout.isCursorMode) {
                    toggleSubtitlesAction()
                    return true
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // D-Pad DOWN on TV remote: Enable mouse cursor mode to interact with player options
                if (event.action == KeyEvent.ACTION_UP && !cursorLayout.isCursorMode) {
                    cursorLayout.toggleCursorMode(true)
                    val w = if (cursorLayout.width > 0) cursorLayout.width.toFloat() else 1920f
                    val h = if (cursorLayout.height > 0) cursorLayout.height.toFloat() else 1080f
                    cursorLayout.setCursorPosition(w * 0.88f, h * 0.88f)
                    Toast.makeText(this, "🖱️ Cursor ON (Long-press OK to toggle off)", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }

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

    private fun toggleSubtitlesAction() {
        if (isWatchingFromNative || customView != null) {
            // 1. Evaluate JS to click the subtitle/CC button in the player or toggle textTracks
            webView.evaluateJavascript("""
                (function() {
                    if (window.__7reels_toggle_subtitles) {
                        return window.__7reels_toggle_subtitles();
                    }
                    return 'no_handler';
                })();
            """.trimIndent()) { result ->
                Log.d(TAG, "Subtitles toggled: $result")
            }

            // 2. Enable mouse cursor positioned at bottom-right near player controls
            if (!cursorLayout.isCursorMode) {
                cursorLayout.toggleCursorMode(true)
                val w = if (cursorLayout.width > 0) cursorLayout.width.toFloat() else 1920f
                val h = if (cursorLayout.height > 0) cursorLayout.height.toFloat() else 1080f
                cursorLayout.setCursorPosition(w * 0.88f, h * 0.88f)
            }
            Toast.makeText(this, "💬 Subtitles / CC (Use remote arrows & OK to select)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "💬 Subtitles button is active during video playback", Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. Close HTML5 video if active
                if (customView != null) {
                    hideCustomView()
                    return
                }

                // 2. If watching stream initiated from Native mode, return to Movie Details / Server screen
                if (isWatchingFromNative) {
                    isWatchingFromNative = false
                    cursorLayout.toggleCursorMode(false)
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.visibility = View.GONE
                    nativeTvContainer.visibility = View.VISIBLE
                    refreshContinueWatching()
                    // Return focus to Play button
                    btnPlayNow.requestFocus()
                    return
                }

                // 3. If Movie Details screen is open, back key returns to Browse dashboard
                if (currentAppMode == AppPreferences.MODE_NATIVE && detailsContainer.visibility == View.VISIBLE) {
                    closeMovieDetails()
                    return
                }

                // 4. Go back in WebView history if available (when in Web mode)
                if (currentAppMode == AppPreferences.MODE_WEBVIEW && webView.canGoBack()) {
                    webView.goBack()
                    return
                }

                // 5. Double-back guard to prevent accidental exit on TV remotes
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

    class WebAppInterface(private val activity: MainActivity) {
        @JavascriptInterface
        fun exitApp() {
            if (activity.currentAppMode == AppPreferences.MODE_WEBVIEW &&
                activity.webView.url?.contains("7reels.cc") == true) {
                Log.d(TAG, "Exit requested via Web Interface")
                activity.runOnUiThread {
                    Toast.makeText(activity, "Closing 7Reels TV...", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
            }
        }

        @JavascriptInterface
        fun switchToNative() {
            Log.d(TAG, "Switch to Native requested via Web Interface")
            activity.runOnUiThread {
                activity.switchAppMode(AppPreferences.MODE_NATIVE)
            }
        }

        @JavascriptInterface
        fun openModeSelector() {
            Log.d(TAG, "Mode selector requested via Web Interface")
            activity.runOnUiThread {
                activity.showModeSelectionDialog(isFirstRun = false)
            }
        }

        @JavascriptInterface
        fun saveWatchProgress(currentTime: Double, duration: Double) {
            activity.runOnUiThread {
                activity.handleWatchProgress(currentTime.toLong(), duration.toLong())
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.userAgentString = DESKTOP_USER_AGENT
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(WebAppInterface(this), "AndroidApp")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                customViewContainer.visibility = View.VISIBLE
                customViewContainer.addView(view)
                hideSystemBars()
            }

            override fun onHideCustomView() {
                hideCustomView()
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (currentAppMode == AppPreferences.MODE_WEBVIEW) {
                    if (newProgress < 100) {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = newProgress
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                return false
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                if (!AdBlocker.isNavigationAllowed(uri)) {
                    return true
                }
                return false
            }

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
                if (currentAppMode == AppPreferences.MODE_WEBVIEW) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 10
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                if (url != null && url.contains("7reels.cc")) {
                    injectNavButtons()
                }
                if (isWatchingFromNative) {
                    injectProgressTracker()
                }
            }
        }

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
    }

    private fun injectNavButtons() {
        val js = """
            (function() {
                function addHeaderButtons() {
                    var navContainer = document.querySelector('[data-nav-links="true"]');
                    var flexContainer = document.querySelector('div.flex.min-w-0.flex-1.items-center');
                    var target = navContainer || flexContainer;
                    if (!target) return;

                    // 1. Mode Switch Button (⚡ Native TV)
                    if (!document.getElementById('tv-mode-btn')) {
                        var modeBtn = document.createElement('a');
                        modeBtn.id = 'tv-mode-btn';
                        modeBtn.setAttribute('role', 'button');
                        modeBtn.setAttribute('tabindex', '0');
                        modeBtn.className = 'relative z-10 inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-semibold text-emerald-400 hover:text-emerald-300 hover:bg-emerald-500/20 cursor-pointer transition-colors';
                        modeBtn.style.outline = 'none';
                        modeBtn.style.marginLeft = '8px';
                        modeBtn.style.userSelect = 'none';
                        modeBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="display:inline-block;vertical-align:middle;margin-right:4px;"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg><span>⚡ Native TV</span>';

                        var modeStyleId = 'tv-mode-btn-style';
                        if (!document.getElementById(modeStyleId)) {
                            var mst = document.createElement('style');
                            mst.id = modeStyleId;
                            mst.textContent = '#tv-mode-btn:focus, #tv-mode-btn:focus-visible { outline: 2px solid #10b981 !important; background: rgba(16, 185, 129, 0.3) !important; color: #ffffff !important; transform: scale(1.05); }';
                            document.head.appendChild(mst);
                        }

                        function triggerSwitch(e) {
                            if (e) { e.preventDefault(); e.stopPropagation(); }
                            if (window.AndroidApp && window.AndroidApp.switchToNative) {
                                window.AndroidApp.switchToNative();
                            }
                        }

                        modeBtn.addEventListener('click', triggerSwitch);
                        modeBtn.addEventListener('keydown', function(e) {
                            if (e.key === 'Enter' || e.keyCode === 13) triggerSwitch(e);
                        });

                        target.appendChild(modeBtn);
                    }

                    // 2. Exit Button
                    if (!document.getElementById('tv-exit-btn')) {
                        var btn = document.createElement('a');
                        btn.id = 'tv-exit-btn';
                        btn.setAttribute('role', 'button');
                        btn.setAttribute('tabindex', '0');
                        btn.className = 'relative z-10 inline-flex items-center gap-1.5 rounded-lg px-3.5 py-1.5 text-sm font-semibold text-red-400 hover:text-red-300 hover:bg-red-500/20 cursor-pointer transition-colors';
                        btn.style.outline = 'none';
                        btn.style.marginLeft = '6px';
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
                            if (e.key === 'Enter' || e.keyCode === 13) triggerExit(e);
                        });

                        target.appendChild(btn);
                    }
                }

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
                            if (e.key === 'Enter' || e.keyCode === 13) triggerExit(e);
                        });

                        document.body.appendChild(existing);
                    }

                    var rect = backBtn.getBoundingClientRect();
                    if (rect.width > 0) {
                        existing.style.position = 'fixed';
                        existing.style.top = rect.top + 'px';
                        existing.style.left = (rect.right + 12) + 'px';
                        existing.style.zIndex = '120';
                    }
                }

                function checkAllButtons() {
                    addHeaderButtons();
                    addWatchExitBtn();
                }

                checkAllButtons();

                if (!window.__7reels_observer_set) {
                    window.__7reels_observer_set = true;
                    var observer = new MutationObserver(checkAllButtons);
                    observer.observe(document.body, { childList: true, subtree: true });
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun injectProgressTracker() {
        val targetSeek = currentSeekTime
        val js = """
            (function() {
                if (window.__7reels_tracker_installed) return;
                window.__7reels_tracker_installed = true;

                function findActiveVideo() {
                    var v = document.querySelector('video');
                    if (v && v.duration > 0) return v;
                    var frames = document.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        try {
                            var fv = frames[i].contentDocument ? frames[i].contentDocument.querySelector('video') : null;
                            if (fv && fv.duration > 0) return fv;
                        } catch(e) {}
                    }
                    return null;
                }

                // Periodic progress reporter
                var lastTime = 0;
                setInterval(function() {
                    var v = findActiveVideo();
                    if (v && !isNaN(v.currentTime) && v.duration > 0) {
                        var cur = Math.floor(v.currentTime);
                        var dur = Math.floor(v.duration);
                        if (cur !== lastTime && cur > 0) {
                            lastTime = cur;
                            if (window.AndroidApp && window.AndroidApp.saveWatchProgress) {
                                window.AndroidApp.saveWatchProgress(cur, dur);
                            }
                        }
                    }
                }, 3000);

                // Listen for postMessage from embed iframes
                window.addEventListener('message', function(ev) {
                    if (!ev || !ev.data) return;
                    try {
                        var d = typeof ev.data === 'string' ? JSON.parse(ev.data) : ev.data;
                        var cur = Math.floor(d.currentTime || d.time || (d.data ? d.data.time : 0) || 0);
                        var dur = Math.floor(d.duration || (d.data ? d.data.duration : 0) || 0);
                        if (cur > 0 && window.AndroidApp && window.AndroidApp.saveWatchProgress) {
                            window.AndroidApp.saveWatchProgress(cur, dur);
                        }
                    } catch(err) {}
                });

                // Toggle Play/Pause helper callable from Android TV remote
                window.__7reels_toggle_play = function() {
                    var v = findActiveVideo();
                    if (v) {
                        if (v.paused) {
                            try { v.play(); } catch(e) {}
                        } else {
                            try { v.pause(); } catch(e) {}
                        }
                        return true;
                    }
                    var cx = window.innerWidth / 2;
                    var cy = window.innerHeight / 2;
                    var el = document.elementFromPoint(cx, cy);
                    if (el && el.tagName !== 'BODY' && el.tagName !== 'HTML') {
                        try {
                            ['mousedown', 'mouseup', 'click'].forEach(function(evt) {
                                el.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true, clientX: cx, clientY: cy }));
                            });
                            return true;
                        } catch(e) {}
                    }
                    return false;
                };

                // Relative seek helper callable from Android TV remote
                window.__7reels_seek = function(delta) {
                    var v = findActiveVideo();
                    if (v && !isNaN(v.currentTime)) {
                        try {
                            v.currentTime = Math.max(0, Math.min(v.duration || 999999, v.currentTime + delta));
                            return true;
                        } catch(e) {}
                    }
                    return false;
                };

                // Subtitles / Captions toggle helper callable from Android TV remote
                window.__7reels_toggle_subtitles = function() {
                    // 1. Wake up control bar with mouse movement
                    var w = window.innerWidth;
                    var h = window.innerHeight;
                    try {
                        var moveEvt = new MouseEvent('mousemove', { bubbles: true, clientX: w / 2, clientY: h - 50 });
                        document.dispatchEvent(moveEvt);
                    } catch(e) {}

                    // 2. Search for subtitle/CC button in document and child iframes
                    var selectors = [
                        'button[aria-label*="subtitle" i]',
                        'button[aria-label*="caption" i]',
                        'button[title*="subtitle" i]',
                        'button[title*="caption" i]',
                        '.jw-icon-cc',
                        '.vjs-subs-caps-button',
                        '[data-plyr="captions"]',
                        'button.subtitles',
                        '[class*="subtitles" i]',
                        '[class*="captions" i]',
                        '[class*="caption-button" i]',
                        'button[data-tooltip*="subtitle" i]',
                        'button[data-tooltip*="caption" i]',
                        '.plyr__control[data-plyr="captions"]',
                        '.jw-settings-content'
                    ];

                    function searchButton(doc) {
                        for (var i = 0; i < selectors.length; i++) {
                            var el = doc.querySelector(selectors[i]);
                            if (el) return el;
                        }
                        return null;
                    }

                    var btn = searchButton(document);
                    if (!btn) {
                        var frames = document.querySelectorAll('iframe');
                        for (var f = 0; f < frames.length; f++) {
                            try {
                                if (frames[f].contentDocument) {
                                    btn = searchButton(frames[f].contentDocument);
                                    if (btn) break;
                                }
                            } catch(e) {}
                        }
                    }

                    if (btn) {
                        try {
                            btn.click();
                            return 'clicked_button';
                        } catch(e) {}
                    }

                    // 3. Fallback: Check HTML5 video textTracks
                    var v = findActiveVideo();
                    if (v && v.textTracks && v.textTracks.length > 0) {
                        var tracks = v.textTracks;
                        var showingIdx = -1;
                        for (var t = 0; t < tracks.length; t++) {
                            if (tracks[t].mode === 'showing') {
                                showingIdx = t;
                                break;
                            }
                        }
                        if (showingIdx === -1) {
                            tracks[0].mode = 'showing';
                            return 'enabled_' + (tracks[0].label || tracks[0].language || '0');
                        } else if (showingIdx + 1 < tracks.length) {
                            tracks[showingIdx].mode = 'disabled';
                            tracks[showingIdx + 1].mode = 'showing';
                            return 'enabled_' + (tracks[showingIdx + 1].label || tracks[showingIdx + 1].language || '' + (showingIdx + 1));
                        } else {
                            tracks[showingIdx].mode = 'disabled';
                            return 'disabled';
                        }
                    }

                    return 'not_found';
                };

                // Automatic play trigger attempts for Android TV
                function autoTriggerPlay() {
                    var v = findActiveVideo();
                    if (v && v.paused) {
                        try {
                            var p = v.play();
                            if (p !== undefined) p.catch(function(){});
                        } catch(e) {}
                    }
                    var cx = window.innerWidth / 2;
                    var cy = window.innerHeight / 2;
                    var el = document.elementFromPoint(cx, cy);
                    if (el && el.tagName !== 'BODY' && el.tagName !== 'HTML') {
                        try {
                            ['mousedown', 'mouseup', 'click'].forEach(function(evt) {
                                el.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true, clientX: cx, clientY: cy }));
                            });
                        } catch(e) {}
                    }
                    var playBtns = document.querySelectorAll('button[aria-label*="Play" i], button[title*="Play" i], .play-btn, .vjs-big-play-button, .jw-display-icon-container, [class*="play-button" i], [class*="PlayButton" i], .plyr__control--overlaid');
                    for (var i = 0; i < playBtns.length; i++) {
                        try { playBtns[i].click(); } catch(e){}
                    }
                }

                [800, 1500, 2500, 4000].forEach(function(delay) {
                    setTimeout(autoTriggerPlay, delay);
                });

                // Auto-seek if resuming
                var seekTarget = $targetSeek;
                if (seekTarget > 0) {
                    var tries = 0;
                    var seekInterval = setInterval(function() {
                        tries++;
                        var v = findActiveVideo();
                        if (v && v.readyState >= 1) {
                            try {
                                v.currentTime = seekTarget;
                                clearInterval(seekInterval);
                            } catch(e) {}
                        }
                        if (tries > 35) clearInterval(seekInterval);
                    }, 400);
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun hideCustomView() {
        if (customView == null) return
        customViewContainer.removeView(customView)
        customView = null
        customViewContainer.visibility = View.GONE
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        webView.visibility = View.VISIBLE
        hideSystemBars()
    }
}

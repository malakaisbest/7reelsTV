package com.malakaonline.tv7reals

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class WatchHistoryItem(
    val tmdbId: Long,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val season: Int = 1,
    val episode: Int = 1,
    val episodeTitle: String? = null,
    val positionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    fun getProgressPercent(): Int {
        if (durationSeconds <= 0L) return 0
        val pct = ((positionSeconds.toDouble() / durationSeconds.toDouble()) * 100).toInt()
        return pct.coerceIn(1, 99)
    }

    fun getFormattedPosition(): String {
        return formatTime(positionSeconds)
    }

    fun getFormattedDuration(): String {
        return formatTime(durationSeconds)
    }

    fun getRemainingTime(): String {
        if (durationSeconds <= 0L) return ""
        val remaining = (durationSeconds - positionSeconds).coerceAtLeast(0L)
        val mins = remaining / 60
        return if (mins >= 60) {
            val hrs = mins / 60
            val remMins = mins % 60
            "${hrs}h ${remMins}m left"
        } else {
            "${mins}m left"
        }
    }

    private fun formatTime(seconds: Long): String {
        if (seconds <= 0L) return "0:00"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    fun getPosterUrl(): String {
        return if (!posterPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/w500$posterPath"
        } else ""
    }

    fun getBackdropUrl(): String {
        return if (!backdropPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/w500$backdropPath"
        } else ""
    }

    fun toMovieItem(): MovieItem {
        return MovieItem(
            id = tmdbId,
            title = title,
            overview = episodeTitle ?: "",
            posterPath = posterPath,
            backdropPath = backdropPath,
            voteAverage = 0.0,
            releaseDate = "",
            mediaType = mediaType
        )
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("tmdbId", tmdbId)
        obj.put("mediaType", mediaType)
        obj.put("title", title)
        obj.put("posterPath", posterPath ?: "")
        obj.put("backdropPath", backdropPath ?: "")
        obj.put("season", season)
        obj.put("episode", episode)
        obj.put("episodeTitle", episodeTitle ?: "")
        obj.put("positionSeconds", positionSeconds)
        obj.put("durationSeconds", durationSeconds)
        obj.put("lastWatchedTimestamp", lastWatchedTimestamp)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): WatchHistoryItem {
            return WatchHistoryItem(
                tmdbId = obj.optLong("tmdbId"),
                mediaType = obj.optString("mediaType", "movie"),
                title = obj.optString("title"),
                posterPath = obj.optString("posterPath").takeIf { it.isNotEmpty() },
                backdropPath = obj.optString("backdropPath").takeIf { it.isNotEmpty() },
                season = obj.optInt("season", 1),
                episode = obj.optInt("episode", 1),
                episodeTitle = obj.optString("episodeTitle").takeIf { it.isNotEmpty() },
                positionSeconds = obj.optLong("positionSeconds", 0L),
                durationSeconds = obj.optLong("durationSeconds", 0L),
                lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", System.currentTimeMillis())
            )
        }
    }
}

object WatchHistoryManager {
    private const val PREFS_NAME = "7reels_watch_history"
    private const val KEY_HISTORY = "history_list_json"
    private const val MAX_HISTORY_ITEMS = 25

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getHistory(context: Context): List<WatchHistoryItem> {
        val raw = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<WatchHistoryItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(WatchHistoryItem.fromJson(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.lastWatchedTimestamp }
    }

    @Synchronized
    fun getProgress(context: Context, tmdbId: Long, mediaType: String, season: Int = 1, episode: Int = 1): WatchHistoryItem? {
        val history = getHistory(context)
        return history.firstOrNull {
            it.tmdbId == tmdbId &&
            it.mediaType.equals(mediaType, ignoreCase = true) &&
            (mediaType != "tv" || (it.season == season && it.episode == episode))
        }
    }

    @Synchronized
    fun saveProgress(context: Context, item: WatchHistoryItem) {
        // Do not save if position is trivial (less than 10 seconds)
        if (item.positionSeconds < 10L) return

        val currentList = getHistory(context).toMutableList()
        // Remove previous entry for same media/episode
        currentList.removeAll {
            it.tmdbId == item.tmdbId &&
            it.mediaType.equals(item.mediaType, ignoreCase = true) &&
            (item.mediaType != "tv" || (it.season == item.season && it.episode == item.episode))
        }

        // Add to top of list
        currentList.add(0, item)

        val trimmed = currentList.take(MAX_HISTORY_ITEMS)
        val arr = JSONArray()
        for (h in trimmed) {
            arr.put(h.toJson())
        }
        getPrefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    @Synchronized
    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }
}

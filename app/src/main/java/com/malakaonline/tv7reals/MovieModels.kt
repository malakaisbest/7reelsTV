package com.malakaonline.tv7reals

data class CastMember(
    val name: String,
    val character: String,
    val profilePath: String?
) {
    fun getProfileUrl(): String {
        return if (!profilePath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/w185$profilePath"
        } else ""
    }
}

data class MovieDetail(
    val id: Long,
    val title: String,
    val tagline: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val runtime: Int, // in minutes
    val genres: List<String>,
    val mediaType: String,
    val numberOfSeasons: Int,
    val cast: List<CastMember>
) {
    fun getPosterUrl(): String {
        return if (!posterPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/w500$posterPath"
        } else ""
    }

    fun getBackdropUrl(): String {
        return if (!backdropPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/original$backdropPath"
        } else ""
    }

    fun getFormattedRuntime(): String {
        if (runtime <= 0) return ""
        val h = runtime / 60
        val m = runtime % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

data class StreamServer(
    val id: String,
    val name: String,
    val description: String
)

object ServerConfig {
    val AVAILABLE_SERVERS = listOf(
        StreamServer("vidfast", "🟢 Server 1 (VidFast)", "Ultra Fast • HD • Auto Play"),
        StreamServer("vidlink", "🔵 Server 2 (VidLink)", "Multi-Language • Subtitles"),
        StreamServer("vidsrc", "🟣 Server 3 (VidSrc)", "High Stability • Mirror"),
        StreamServer("vidbolt", "🟠 Server 4 (VidBolt)", "Fast Stream • 1080p"),
        StreamServer("vidrock", "🟡 Server 5 (VidRock)", "Direct Stream Source"),
        StreamServer("vidzee", "🔴 Server 6 (VidZee)", "Backup Server")
    )

    fun getStreamUrl(serverId: String, tmdbId: Long, mediaType: String, season: Int = 1, episode: Int = 1): String {
        val isTv = mediaType.equals("tv", ignoreCase = true)
        return when (serverId) {
            "vidfast" -> if (isTv) "https://vidfast.vc/tv/$tmdbId/$season/$episode" else "https://vidfast.vc/movie/$tmdbId"
            "vidlink" -> if (isTv) "https://vidlink.pro/tv/$tmdbId/$season/$episode" else "https://vidlink.pro/movie/$tmdbId"
            "vidsrc" -> if (isTv) "https://vidsrc.mov/embed/tv/$tmdbId/$season/$episode" else "https://vidsrc.mov/embed/movie/$tmdbId"
            "vidbolt" -> if (isTv) "https://vidbolt.xyz/embed/tv/$tmdbId/$season/$episode" else "https://vidbolt.xyz/embed/movie/$tmdbId"
            "vidrock" -> if (isTv) "https://vidrock.net/tv/$tmdbId/$season/$episode" else "https://vidrock.net/movie/$tmdbId"
            "vidzee" -> if (isTv) "https://player.vidzee.wtf/embed/tv/$tmdbId/$season/$episode" else "https://player.vidzee.wtf/embed/movie/$tmdbId"
            else -> if (isTv) "https://vidfast.vc/tv/$tmdbId/$season/$episode" else "https://vidfast.vc/movie/$tmdbId"
        }
    }
}

data class MovieItem(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val mediaType: String // "movie" or "tv"
) {
    fun getPosterUrl(): String {
        return if (!posterPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/w500$posterPath"
        } else ""
    }

    fun getBackdropUrl(): String {
        return if (!backdropPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/original$backdropPath"
        } else ""
    }

    fun getStreamUrl(season: Int = 1, episode: Int = 1): String {
        return ServerConfig.getStreamUrl("vidfast", id, mediaType, season, episode)
    }
}

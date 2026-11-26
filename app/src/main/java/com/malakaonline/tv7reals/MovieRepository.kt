package com.malakaonline.tv7reals

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MovieRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://7reels.cc/api/tmdb"

    suspend fun getTrendingMovies(): List<MovieItem> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/trending/movie/week?language=en-US&include_adult=false"
        fetchItems(url, "movie")
    }

    suspend fun getTrendingTv(): List<MovieItem> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/trending/tv/week?language=en-US&include_adult=false"
        fetchItems(url, "tv")
    }

    suspend fun searchMulti(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BASE_URL/search/multi?query=$encoded&language=en-US&include_adult=false"
        fetchItems(url, "movie")
    }

    suspend fun getMovieDetails(id: Long, mediaType: String): MovieDetail? = withContext(Dispatchers.IO) {
        val type = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val url = "$BASE_URL/$type/$id?append_to_response=credits&language=en-US"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val obj = JSONObject(body)

                val title = obj.optString("title").ifEmpty { obj.optString("name") }
                val tagline = obj.optString("tagline")
                val overview = obj.optString("overview")
                val posterPath = obj.optString("poster_path").takeIf { it.isNotEmpty() && it != "null" }
                val backdropPath = obj.optString("backdrop_path").takeIf { it.isNotEmpty() && it != "null" }
                val voteAverage = obj.optDouble("vote_average", 0.0)
                val releaseDate = obj.optString("release_date").ifEmpty { obj.optString("first_air_date") }
                val runtime = obj.optInt("runtime", 0)
                val numberOfSeasons = obj.optInt("number_of_seasons", 1)

                val genresList = mutableListOf<String>()
                val genresArr = obj.optJSONArray("genres")
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        val g = genresArr.getJSONObject(i).optString("name")
                        if (g.isNotEmpty()) genresList.add(g)
                    }
                }

                val seasonsList = mutableListOf<SeasonItem>()
                val seasonsArr = obj.optJSONArray("seasons")
                if (seasonsArr != null) {
                    for (i in 0 until seasonsArr.length()) {
                        val s = seasonsArr.getJSONObject(i)
                        val seasonNum = s.optInt("season_number", 0)
                        val name = s.optString("name")
                        val epCount = s.optInt("episode_count", 0)
                        if (seasonNum > 0) {
                            seasonsList.add(SeasonItem(seasonNum, name.ifEmpty { "Season $seasonNum" }, epCount))
                        }
                    }
                }

                val castList = mutableListOf<CastMember>()
                val creditsObj = obj.optJSONObject("credits")
                val castArr = creditsObj?.optJSONArray("cast")
                if (castArr != null) {
                    val limit = minOf(castArr.length(), 16)
                    for (i in 0 until limit) {
                        val c = castArr.getJSONObject(i)
                        val name = c.optString("name")
                        val character = c.optString("character")
                        val profilePath = c.optString("profile_path").takeIf { it.isNotEmpty() && it != "null" }
                        if (name.isNotEmpty()) {
                            castList.add(CastMember(name, character, profilePath))
                        }
                    }
                }

                return@withContext MovieDetail(
                    id = id,
                    title = title,
                    tagline = tagline,
                    overview = overview,
                    posterPath = posterPath,
                    backdropPath = backdropPath,
                    voteAverage = voteAverage,
                    releaseDate = releaseDate,
                    runtime = runtime,
                    genres = genresList,
                    mediaType = type,
                    numberOfSeasons = numberOfSeasons,
                    seasons = seasonsList,
                    cast = castList
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun getSeasonEpisodes(tvId: Long, seasonNumber: Int): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/tv/$tvId/season/$seasonNumber?language=en-US"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val obj = JSONObject(body)
                val episodesArr = obj.optJSONArray("episodes") ?: return@withContext emptyList()

                val epList = mutableListOf<EpisodeItem>()
                for (i in 0 until episodesArr.length()) {
                    val ep = episodesArr.getJSONObject(i)
                    val epNum = ep.optInt("episode_number", i + 1)
                    val name = ep.optString("name")
                    val overview = ep.optString("overview")
                    val stillPath = ep.optString("still_path").takeIf { it.isNotEmpty() && it != "null" }
                    epList.add(EpisodeItem(epNum, name.ifEmpty { "Episode $epNum" }, overview, stillPath))
                }
                return@withContext epList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getSimilar(id: Long, mediaType: String): List<MovieItem> = withContext(Dispatchers.IO) {
        val endpoint = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val url = "https://7reels.cc/api/tmdb/$endpoint/$id/similar?language=en-US"
        fetchItems(url, mediaType)
    }

    private fun fetchItems(url: String, defaultMediaType: String): List<MovieItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return emptyList()

                val items = mutableListOf<MovieItem>()
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val id = obj.optLong("id")
                    val title = obj.optString("title").ifEmpty { obj.optString("name") }
                    val overview = obj.optString("overview")
                    val posterPath = obj.optString("poster_path").takeIf { it.isNotEmpty() && it != "null" }
                    val backdropPath = obj.optString("backdrop_path").takeIf { it.isNotEmpty() && it != "null" }
                    val voteAverage = obj.optDouble("vote_average", 0.0)
                    val releaseDate = obj.optString("release_date").ifEmpty { obj.optString("first_air_date") }
                    val mediaType = obj.optString("media_type", defaultMediaType)

                    if (mediaType == "person") continue

                    if (id > 0 && title.isNotEmpty()) {
                        items.add(
                            MovieItem(
                                id = id,
                                title = title,
                                overview = overview,
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                voteAverage = voteAverage,
                                releaseDate = releaseDate,
                                mediaType = mediaType
                            )
                        )
                    }
                }
                return items
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

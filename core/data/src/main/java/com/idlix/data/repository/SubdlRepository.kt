package com.idlix.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.idlix.data.api.ApiClient
import com.idlix.data.model.SubdlQuery
import com.idlix.data.model.SubdlResponse
import com.idlix.data.model.SubdlSearchResult
import com.idlix.data.model.SubdlSubtitle
import com.idlix.data.model.SubtitleTrack
import com.idlix.data.util.LocaleHelper
import com.idlix.data.util.SubdlTitleCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.zip.ZipInputStream

object SubdlRepository {
    private const val TAG = "SubdlRepository"
    private const val PREF_KEY_SUBDL_API_KEY = "custom_subdl_api_key"
    const val DEFAULT_API_KEY = "subdl__VuGBV3ExlH8tW68l4wVEIsk6Mfnaxq1_FiONFN8Yg0"
    private const val BASE_URL = "https://api.subdl.com/api/v1/subtitles"

    @Volatile
    private var currentApiKey: String? = null

    fun getApiKey(): String {
        currentApiKey?.let { return it }
        val saved = try {
            val prefs = ApiClient.applicationContext?.getSharedPreferences("idlix_config", Context.MODE_PRIVATE)
            prefs?.getString(PREF_KEY_SUBDL_API_KEY, null)
        } catch (_: Exception) {
            null
        }
        val key = if (!saved.isNullOrBlank()) saved else DEFAULT_API_KEY
        currentApiKey = key
        return key
    }

    fun setApiKey(newKey: String) {
        val trimmed = newKey.trim()
        currentApiKey = trimmed
        try {
            val prefs = ApiClient.applicationContext?.getSharedPreferences("idlix_config", Context.MODE_PRIVATE)
            prefs?.edit()?.putString(PREF_KEY_SUBDL_API_KEY, trimmed)?.apply()
        } catch (_: Exception) {}
    }

    suspend fun testApiKey(targetKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = targetKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("SubDL API key cannot be empty"))
        }
        val testEndpoint = "$BASE_URL?api_key=$trimmed&film_name=Avatar"
        try {
            val req = Request.Builder()
                .url(testEndpoint)
                .header("User-Agent", "Mozilla/5.0 (Android TV; Idlix)")
                .get()
                .build()
            ApiClient.okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val parsed = try {
                        ApiClient.json.decodeFromString<SubdlResponse>(body)
                    } catch (_: Exception) { null }

                    if (parsed?.status == true) {
                        Result.success("SubDL API key is valid (${parsed.subtitles.size} subtitles found)")
                    } else {
                        Result.failure(Exception("Invalid response or quota exceeded"))
                    }
                } else {
                    Result.failure(Exception("HTTP error: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildSearchUrl(query: SubdlQuery, apiKey: String): String {
        val queryParams = StringBuilder("api_key=$apiKey&languages=${query.languages}")
        if (query.sdId != null && query.sdId > 0) {
            queryParams.append("&sd_id=${query.sdId}")
        } else if (query.tmdbId != null && query.tmdbId > 0) {
            queryParams.append("&tmdb_id=${query.tmdbId}")
        } else if (!query.imdbId.isNullOrBlank()) {
            queryParams.append("&imdb_id=${query.imdbId.trim()}")
        } else if (!query.filmName.isNullOrBlank()) {
            val encoded = URLEncoder.encode(query.filmName.trim(), "UTF-8")
            queryParams.append("&film_name=$encoded")
        }

        if (query.year != null && query.year > 0) {
            queryParams.append("&year=${query.year}")
        }

        val effectiveType = if (query.type == "tv" || query.season != null) "tv" else "movie"
        queryParams.append("&type=$effectiveType")
        if (effectiveType == "tv") {
            if (query.season != null) queryParams.append("&season_number=${query.season}")
            if (query.episode != null) queryParams.append("&episode_number=${query.episode}")
        }

        return "$BASE_URL?$queryParams"
    }

    suspend fun search(query: SubdlQuery): SubdlSearchResult = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val requestUrl = buildSearchUrl(query, apiKey)
            val request = Request.Builder()
                .url(requestUrl)
                .header("User-Agent", "Mozilla/5.0 (Android TV; Idlix)")
                .build()

            val response = ApiClient.okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "SubDL request failed with code: ${response.code}")
                val reason = when (response.code) {
                    401, 403 -> SubdlSearchResult.Error.Reason.INVALID_KEY
                    429 -> SubdlSearchResult.Error.Reason.QUOTA
                    else -> SubdlSearchResult.Error.Reason.HTTP
                }
                return@withContext SubdlSearchResult.Error(reason, "HTTP error: ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val subdlRes = try {
                ApiClient.json.decodeFromString<SubdlResponse>(body)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse SubDL response: ${e.message}")
                return@withContext SubdlSearchResult.Error(SubdlSearchResult.Error.Reason.PARSE, e.localizedMessage)
            }

            if (subdlRes.subtitles.isNotEmpty()) {
                SubdlSearchResult.Found(
                    subtitles = subdlRes.subtitles,
                    candidates = subdlRes.results
                )
            } else {
                if (!subdlRes.status && !subdlRes.error.isNullOrBlank()) {
                    val err = subdlRes.error.lowercase()
                    if (err.contains("key") || err.contains("auth")) {
                        SubdlSearchResult.Error(SubdlSearchResult.Error.Reason.INVALID_KEY, subdlRes.error)
                    } else if (err.contains("limit") || err.contains("quota")) {
                        SubdlSearchResult.Error(SubdlSearchResult.Error.Reason.QUOTA, subdlRes.error)
                    } else {
                        SubdlSearchResult.NotFound(candidates = subdlRes.results)
                    }
                } else {
                    SubdlSearchResult.NotFound(candidates = subdlRes.results)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search SubDL subtitles: ${e.message}")
            SubdlSearchResult.Error(SubdlSearchResult.Error.Reason.NETWORK, e.localizedMessage)
        }
    }

    suspend fun searchAllSubtitles(
        title: String,
        type: String = "movie",
        tmdbId: Long? = null,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        languages: String = "ID,EN"
    ): List<SubdlSubtitle> = withContext(Dispatchers.IO) {
        val cleaned = SubdlTitleCleaner.clean(title)
        val query = SubdlQuery(
            filmName = cleaned.title,
            year = cleaned.year,
            type = type,
            tmdbId = tmdbId,
            imdbId = imdbId,
            season = season,
            episode = episode,
            languages = languages
        )
        when (val result = search(query)) {
            is SubdlSearchResult.Found -> result.subtitles
            else -> emptyList()
        }
    }

    suspend fun downloadSubtitle(
        sub: SubdlSubtitle,
        context: Context
    ): SubtitleTrack? = withContext(Dispatchers.IO) {
        try {
            val isIndo = sub.language.equals("ID", ignoreCase = true) || sub.lang.equals("indonesian", ignoreCase = true)
            val langCode = if (isIndo) "id" else (sub.language?.lowercase() ?: "en")
            val prefix = "subdl_${langCode}_${System.currentTimeMillis()}"

            // SubDL direct download: dl.subdl.com enforces a 50/day limit on ?api_key= urls,
            // but provides direct zip access on clean URLs without api_key!
            val primaryUrl = sub.cleanDownloadUrl
            val fallbackUrl = if (sub.url.startsWith("http")) sub.url else "https://dl.subdl.com${sub.url}"

            val extractedFile = downloadAndExtractSrt(
                primaryUrl = primaryUrl,
                fallbackUrl = fallbackUrl,
                context = context,
                filePrefix = prefix,
                isIndo = isIndo
            ) ?: return@withContext null

            val releaseTag = sub.author?.let { " ($it)" } ?: ""
            val label = if (isIndo) "Indonesian [SubDL]$releaseTag" else "English [SubDL]$releaseTag"

            SubtitleTrack(
                lang = langCode,
                label = label,
                path = Uri.fromFile(extractedFile).toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download SubDL subtitle: ${e.message}")
            null
        }
    }

    suspend fun fetchSubtitleForLanguage(
        title: String,
        type: String = "movie",
        tmdbId: Long? = null,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        targetLang: String = LocaleHelper.getPreferredLanguage(),
        context: Context
    ): SubtitleTrack? = withContext(Dispatchers.IO) {
        val langParam = if (targetLang.equals("id", ignoreCase = true)) "ID" else "EN"
        val allSubs = searchAllSubtitles(title, type, tmdbId, imdbId, season, episode, languages = langParam)
        val chosen = allSubs.firstOrNull {
            if (langParam == "ID") {
                it.language.equals("ID", ignoreCase = true) || it.lang.equals("indonesian", ignoreCase = true)
            } else {
                it.language.equals("EN", ignoreCase = true) || it.lang.equals("english", ignoreCase = true)
            }
        } ?: allSubs.firstOrNull() ?: return@withContext null

        downloadSubtitle(chosen, context)
    }

    suspend fun fetchIndonesianSubtitle(
        title: String,
        type: String = "movie",
        tmdbId: Long? = null,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        context: Context
    ): SubtitleTrack? = fetchSubtitleForLanguage(
        title = title,
        type = type,
        tmdbId = tmdbId,
        imdbId = imdbId,
        season = season,
        episode = episode,
        targetLang = "id",
        context = context
    )

    private fun downloadAndExtractSrt(
        primaryUrl: String,
        fallbackUrl: String?,
        context: Context,
        filePrefix: String,
        isIndo: Boolean
    ): File? {
        // Attempt 1: Clean URL (without ?api_key= to bypass 50/day rate-limit)
        var file = tryDownloadAndExtractZip(primaryUrl, context, filePrefix, isIndo)
        if (file != null) return file

        // Attempt 2: Fallback URL if different
        if (!fallbackUrl.isNullOrBlank() && fallbackUrl != primaryUrl) {
            file = tryDownloadAndExtractZip(fallbackUrl, context, filePrefix, isIndo)
            if (file != null) return file
        }

        return null
    }

    private fun tryDownloadAndExtractZip(
        url: String,
        context: Context,
        filePrefix: String,
        isIndo: Boolean
    ): File? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://subdl.com/")
                .header("Accept", "*/*")
                .build()

            val res = ApiClient.okHttpClient.newCall(req).execute()
            if (!res.isSuccessful) {
                Log.w(TAG, "Download zip failed with HTTP ${res.code} for: $url")
                return null
            }

            val body = res.body ?: return null
            val bytes = body.bytes()
            if (bytes.isEmpty()) return null

            val subDir = File(context.cacheDir, "subtitles").apply { mkdirs() }

            // Check if zip archive (magic bytes PK 0x50 0x4B)
            if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                val zipStream = ZipInputStream(bytes.inputStream())
                val entries = mutableListOf<Pair<String, ByteArray>>()

                var entry = zipStream.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val lower = entryName.lowercase()
                    if (!entry.isDirectory &&
                        !entryName.contains("__MACOSX") &&
                        !entryName.startsWith(".") &&
                        !entryName.contains("/.")
                    ) {
                        if (lower.endsWith(".srt") || lower.endsWith(".vtt")) {
                            entries.add(entryName to zipStream.readBytes())
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
                zipStream.close()

                if (entries.isNotEmpty()) {
                    val chosen = if (isIndo) {
                        entries.firstOrNull { (name, _) ->
                            val l = name.lowercase()
                            l.contains("ind") || l.contains("id") || l.contains("bahasa")
                        } ?: entries.first()
                    } else {
                        entries.first()
                    }

                    val ext = if (chosen.first.lowercase().endsWith(".vtt")) ".vtt" else ".srt"
                    val outFile = File(subDir, "$filePrefix$ext")
                    outFile.writeBytes(chosen.second)
                    return outFile
                }
            } else {
                // Raw srt or vtt stream
                val isVtt = url.contains(".vtt", ignoreCase = true)
                val ext = if (isVtt) ".vtt" else ".srt"
                val outFile = File(subDir, "$filePrefix$ext")
                outFile.writeBytes(bytes)
                return outFile
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting zip from $url: ${e.message}")
            null
        }
    }
}

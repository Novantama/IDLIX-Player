package com.idlix.tv.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object Search : Screen("search")
    object ContinueWatching : Screen("continue_watching")
    object Settings : Screen("settings")
    object SubtitlePositionEditor : Screen("subtitle_position_editor")

    object Detail : Screen("detail/{contentType}/{slug}") {
        fun createRoute(contentType: String, slug: String): String =
            "detail/$contentType/${Uri.encode(slug)}"
    }

    object Player : Screen("player/{contentType}/{contentId}/{title}?poster={poster}&backdrop={backdrop}&slug={slug}&season={season}&episode={episode}") {
        fun createRoute(
            contentType: String,
            contentId: String,
            title: String,
            poster: String? = null,
            backdrop: String? = null,
            slug: String? = null,
            season: Int? = null,
            episode: Int? = null
        ): String {
            val encodedId = Uri.encode(contentId.ifBlank { "0" })
            val encodedTitle = Uri.encode(title.ifBlank { "Video" })
            val encodedPoster = Uri.encode(poster ?: "")
            val encodedBackdrop = Uri.encode(backdrop ?: "")
            val encodedSlug = Uri.encode(slug ?: "")
            val seasonParam = season?.toString() ?: ""
            val episodeParam = episode?.toString() ?: ""
            return "player/$contentType/$encodedId/$encodedTitle?poster=$encodedPoster&backdrop=$encodedBackdrop&slug=$encodedSlug&season=$seasonParam&episode=$episodeParam"
        }
    }
}

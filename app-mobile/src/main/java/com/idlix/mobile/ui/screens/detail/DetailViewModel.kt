package com.idlix.mobile.ui.screens.detail

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.data.local.WatchHistoryStore
import com.idlix.data.model.EpisodeItem
import com.idlix.data.model.MovieDetail
import com.idlix.data.model.ResolvedPlayback
import com.idlix.data.model.Season
import com.idlix.data.model.SeriesDetail
import com.idlix.data.model.SubdlItem
import com.idlix.data.model.SubdlQuery
import com.idlix.data.model.SubdlSearchResult
import com.idlix.data.model.SubdlSubtitle
import com.idlix.data.model.SubtitleTrack
import com.idlix.data.repository.IdlixRepository
import com.idlix.data.repository.SubdlRepository
import com.idlix.data.util.SubdlTitleCleaner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NextEpisodeInfo(
    val episode: EpisodeItem,
    val seasonNumber: Int,
    val seriesTitle: String,
    val seriesSlug: String,
    val posterUrl: String?,
    val backdropUrl: String?
)

data class DetailUiState(
    val isLoadingDetail: Boolean = true,
    val detailError: String? = null,
    val movie: MovieDetail? = null,
    val series: SeriesDetail? = null,
    val selectedSeason: Season? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val selectedEpisode: EpisodeItem? = null,

    // Active playback info
    val playingEpisode: EpisodeItem? = null,
    val playingSeasonNumber: Int? = null,

    // Playback state
    val isStreamLoading: Boolean = false,
    val countdownSeconds: Int = 0,
    val streamError: String? = null,
    val resolvedStream: ResolvedPlayback? = null,
    val initialPositionMs: Long = 0L,

    // Subtitles
    val selectedSubtitle: SubtitleTrack? = null,
    val isSubtitleEnabled: Boolean = true,
    val isSubtitlePickerVisible: Boolean = false,
    val subdlSubtitles: List<SubdlSubtitle> = emptyList(),
    val isSearchingSubdl: Boolean = false,
    val subdlCandidates: List<SubdlItem> = emptyList(),
    val subdlError: SubdlSearchResult.Error? = null,
    val subdlActiveQuery: SubdlQuery? = null,
    val isManualSearchOpen: Boolean = false,
    val manualTitle: String = "",
    val manualYear: String = "",
    val manualSeason: String = "",
    val manualEpisode: String = "",
    val isUsingSessionOverride: Boolean = false,
    val isAppearancePanelOpen: Boolean = false,

    // Next Episode Auto-Prompt
    val nextEpisode: NextEpisodeInfo? = null,
    val isNextEpisodeDismissed: Boolean = false,
    val isBookmarked: Boolean = false
)

class DetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = IdlixRepository()
    private val historyStore = WatchHistoryStore.getInstance(application.applicationContext)
    private val bookmarkPrefs = application.applicationContext.getSharedPreferences("idlix_watchlist", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentContentId: String = ""
    private var currentContentType: String = ""
    private var currentContentTitle: String = ""
    private var currentPosterUrl: String? = null
    private var currentBackdropUrl: String? = null
    private var currentSlug: String? = null
    private var currentSeasonNumber: Int? = null
    private var currentEpisodeNumber: Int? = null

    private var playbackJob: Job? = null
    private var resolveNextEpisodeJob: Job? = null
    private var seasonLoadJob: Job? = null

    fun loadDetail(contentType: String, slug: String) {
        viewModelScope.launch {
            val bookmarkKey = "${if (contentType == "episode" || contentType == "tv_series") "series" else contentType}:$slug"
            val savedBookmark = bookmarkPrefs.getBoolean(bookmarkKey, false)
            _uiState.value = _uiState.value.copy(
                isLoadingDetail = true,
                detailError = null,
                isBookmarked = savedBookmark
            )

            try {
                if (contentType == "tv_series" || contentType == "series" || contentType == "episode") {
                    val series = repository.getSeriesDetail(slug)
                    if (series != null) {
                        currentContentType = "episode"
                        currentContentTitle = series.title
                        currentSlug = series.slug
                        val allSeasons = series.seasons
                        val defaultSeason = series.defaultSeason
                        val defaultSeasonNum = defaultSeason?.seasonNumber
                        val initialSeason = allSeasons.firstOrNull { it.seasonNumber == defaultSeasonNum }
                            ?: defaultSeason?.let { Season(id = it.id, seasonNumber = it.seasonNumber, name = it.name) }
                            ?: allSeasons.firstOrNull() ?: Season(
                                seasonNumber = defaultSeason?.seasonNumber ?: 1,
                                name = defaultSeason?.name ?: "Season 1"
                            )
                        val initialEpisodes = defaultSeason?.episodes ?: emptyList()
                        val firstEp = initialEpisodes.firstOrNull()
                        currentSeasonNumber = initialSeason.seasonNumber
                        currentEpisodeNumber = firstEp?.episodeNumber

                        _uiState.value = _uiState.value.copy(
                            isLoadingDetail = false,
                            series = series,
                            movie = null,
                            selectedSeason = initialSeason,
                            episodes = initialEpisodes,
                            selectedEpisode = firstEp
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingDetail = false,
                            detailError = "Failed to load TV series details"
                        )
                    }
                } else {
                    val movie = repository.getMovieDetail(slug)
                    if (movie != null) {
                        currentContentType = "movie"
                        currentContentTitle = movie.title
                        currentSlug = movie.slug
                        currentSeasonNumber = null
                        currentEpisodeNumber = null
                        _uiState.value = _uiState.value.copy(
                            isLoadingDetail = false,
                            movie = movie,
                            series = null,
                            episodes = emptyList()
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingDetail = false,
                            detailError = "Failed to load movie details"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error loading detail: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    detailError = e.localizedMessage ?: "Network error loading details"
                )
            }
        }
    }

    fun selectSeason(season: Season) {
        val series = _uiState.value.series ?: return
        if (_uiState.value.selectedSeason?.seasonNumber == season.seasonNumber && _uiState.value.episodes.isNotEmpty()) {
            return
        }
        seasonLoadJob?.cancel()
        seasonLoadJob = viewModelScope.launch {
            val defaultSeason = series.defaultSeason
            if (defaultSeason != null && defaultSeason.seasonNumber == season.seasonNumber && defaultSeason.episodes.isNotEmpty()) {
                val cachedEpisodes = defaultSeason.episodes
                _uiState.value = _uiState.value.copy(
                    selectedSeason = season,
                    episodes = cachedEpisodes,
                    selectedEpisode = cachedEpisodes.firstOrNull()
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(selectedSeason = season, episodes = emptyList())
            try {
                val epRes = repository.getEpisodeDetail(series.slug, season.seasonNumber, 1)
                if (_uiState.value.selectedSeason?.seasonNumber != season.seasonNumber) return@launch
                val newEpisodes = epRes?.season?.episodes ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    episodes = newEpisodes,
                    selectedEpisode = newEpisodes.firstOrNull()
                )
            } catch (e: Exception) {
                Log.w("DetailViewModel", "Failed to fetch episodes for season ${season.seasonNumber}: ${e.message}")
            }
        }
    }

    fun selectEpisode(episode: EpisodeItem) {
        val series = _uiState.value.series ?: return
        val seasonNum = _uiState.value.selectedSeason?.seasonNumber ?: 1

        // If this exact episode is already playing and active without error, keep playing
        if (_uiState.value.playingEpisode?.id == episode.id &&
            (_uiState.value.resolvedStream != null || _uiState.value.isStreamLoading) &&
            _uiState.value.streamError == null
        ) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedEpisode = episode,
            playingEpisode = episode,
            playingSeasonNumber = seasonNum
        )
        playEpisode(series, seasonNum, episode)
    }

    fun playCurrentMedia() {
        val state = _uiState.value
        val movie = state.movie
        val series = state.series
        if (movie != null) {
            playMovie(movie)
        } else if (series != null) {
            val seasonNum = state.selectedSeason?.seasonNumber ?: 1
            val ep = state.selectedEpisode ?: state.episodes.firstOrNull()
            if (ep != null) {
                _uiState.value = _uiState.value.copy(
                    selectedEpisode = ep,
                    playingEpisode = ep,
                    playingSeasonNumber = seasonNum
                )
                playEpisode(series, seasonNum, ep)
            }
        }
    }

    private fun playMovie(movie: MovieDetail) {
        startStreamPlayback(
            contentType = "movie",
            contentId = movie.id,
            title = movie.title,
            posterUrl = movie.fullPosterUrl,
            backdropUrl = movie.fullBackdropUrl,
            slug = movie.slug,
            seasonNumber = null,
            episodeNumber = null
        )
    }

    private fun playEpisode(series: SeriesDetail, seasonNumber: Int, episode: EpisodeItem) {
        val episodeTitle = "${series.title} - S${seasonNumber}E${episode.episodeNumber}: ${episode.name}"
        startStreamPlayback(
            contentType = "episode",
            contentId = episode.id,
            title = episodeTitle,
            posterUrl = episode.fullStillUrl.ifBlank { series.fullPosterUrl },
            backdropUrl = series.fullBackdropUrl,
            slug = series.slug,
            seasonNumber = seasonNumber,
            episodeNumber = episode.episodeNumber
        )
    }

    fun retryPlayback() {
        if (currentContentId.isNotBlank()) {
            startStreamPlayback(
                contentType = currentContentType,
                contentId = currentContentId,
                title = currentContentTitle,
                posterUrl = currentPosterUrl,
                backdropUrl = currentBackdropUrl,
                slug = currentSlug,
                seasonNumber = currentSeasonNumber,
                episodeNumber = currentEpisodeNumber
            )
        }
    }

    private fun startStreamPlayback(
        contentType: String,
        contentId: String,
        title: String,
        posterUrl: String? = null,
        backdropUrl: String? = null,
        slug: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) {
        // Cancel in-flight playback and resolve jobs to prevent race conditions and double play
        playbackJob?.cancel()
        resolveNextEpisodeJob?.cancel()
        subdlSearchJob?.cancel()
        sessionQueryOverride = null

        currentContentId = contentId
        currentContentType = contentType
        currentContentTitle = title
        currentPosterUrl = posterUrl
        currentBackdropUrl = backdropUrl
        currentSlug = slug
        currentSeasonNumber = seasonNumber
        currentEpisodeNumber = episodeNumber

        val savedPos = historyStore.getSavedPosition(contentId)

        historyStore.recordStartPlayback(
            contentId = contentId,
            contentType = contentType,
            title = title,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            slug = slug,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )

        playbackJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isStreamLoading = true,
                countdownSeconds = 0,
                streamError = null,
                resolvedStream = null,
                initialPositionMs = savedPos,
                nextEpisode = null,
                isNextEpisodeDismissed = false,
                subdlSubtitles = emptyList(),
                subdlCandidates = emptyList(),
                subdlError = null,
                subdlActiveQuery = null,
                isSearchingSubdl = false,
                isUsingSessionOverride = false
            )

            // Asynchronously resolve next episode for Series
            if (contentType == "episode" && !slug.isNullOrBlank()) {
                resolveNextEpisodeJob = resolveNextEpisode(
                    seriesSlug = slug,
                    currentEpisodeId = contentId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    fallbackTitle = title,
                    posterUrl = posterUrl,
                    backdropUrl = backdropUrl
                )
            }

            val result = repository.resolveStream(
                contentType = contentType,
                contentId = contentId,
                contentTitle = title,
                onCountdown = { sec ->
                    _uiState.value = _uiState.value.copy(countdownSeconds = sec)
                }
            )

            result.onSuccess { resolved ->
                val preSelected = _uiState.value.selectedSubtitle
                val finalSubtitles = if (preSelected != null && resolved.subtitles.none { it.path == preSelected.path }) {
                    listOf(preSelected) + resolved.subtitles
                } else {
                    resolved.subtitles
                }
                val finalResolved = resolved.copy(subtitles = finalSubtitles)

                // Always prioritize pre-selected subtitle or Bahasa Indonesia ("id" / "in") by default
                val defaultSub = preSelected
                    ?: finalSubtitles.firstOrNull { it.lang.equals("id", ignoreCase = true) || it.lang.equals("in", ignoreCase = true) }
                    ?: finalSubtitles.firstOrNull { it.lang.equals("en", ignoreCase = true) }
                    ?: finalSubtitles.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isStreamLoading = false,
                    resolvedStream = finalResolved,
                    selectedSubtitle = defaultSub,
                    isSubtitleEnabled = defaultSub != null
                )

                // Pre-cache SubDL subtitles in background so both Indonesian and English are ready instantly
                if (_uiState.value.subdlSubtitles.isEmpty() && !_uiState.value.isSearchingSubdl) {
                    searchSubdlSubtitles()
                }
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isStreamLoading = false,
                    streamError = err.localizedMessage ?: "Failed to play stream"
                )
            }
        }
    }

    private fun resolveNextEpisode(
        seriesSlug: String,
        currentEpisodeId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
        fallbackTitle: String,
        posterUrl: String?,
        backdropUrl: String?
    ): Job {
        return viewModelScope.launch {
            try {
                val series = repository.getSeriesDetail(seriesSlug) ?: return@launch
                val defaultSeason = series.defaultSeason
                val seriesTitle = series.title.ifBlank { fallbackTitle.substringBefore(" -") }
                val rawSeasons = series.seasons.ifEmpty {
                    if (defaultSeason != null) {
                        listOf(Season(seasonNumber = defaultSeason.seasonNumber, name = defaultSeason.name))
                    } else emptyList()
                }
                val allSeasons = rawSeasons.sortedBy { it.seasonNumber }

                var currentSeasonNum = seasonNumber
                var currentEpNum = episodeNumber
                var seasonEpisodes: List<EpisodeItem> = emptyList()

                // Strategy 1: Check if currentEpisodeId is in defaultSeason
                if (defaultSeason?.episodes?.any { it.id == currentEpisodeId } == true) {
                    currentSeasonNum = defaultSeason.seasonNumber
                    seasonEpisodes = defaultSeason.episodes
                } else if (currentSeasonNum != null && defaultSeason?.seasonNumber == currentSeasonNum && !defaultSeason.episodes.isNullOrEmpty()) {
                    seasonEpisodes = defaultSeason.episodes
                } else if (currentSeasonNum != null) {
                    val epDetail = try {
                        repository.getEpisodeDetail(seriesSlug, currentSeasonNum, 1)
                    } catch (_: Exception) {
                        null
                    }
                    seasonEpisodes = epDetail?.season?.episodes ?: emptyList()
                }

                // Strategy 2: If seasonEpisodes is still empty or currentEpisodeId not found, scan all seasons
                if (seasonEpisodes.isEmpty() || (currentEpisodeId.isNotBlank() && seasonEpisodes.none { it.id == currentEpisodeId } && currentEpNum == null)) {
                    for (s in allSeasons) {
                        if (s.seasonNumber == defaultSeason?.seasonNumber && !defaultSeason.episodes.isNullOrEmpty()) {
                            if (defaultSeason.episodes.any { it.id == currentEpisodeId }) {
                                currentSeasonNum = s.seasonNumber
                                seasonEpisodes = defaultSeason.episodes
                                break
                            }
                        } else {
                            val epDetail = try {
                                repository.getEpisodeDetail(seriesSlug, s.seasonNumber, 1)
                            } catch (_: Exception) {
                                null
                            }
                            val eps = epDetail?.season?.episodes ?: emptyList()
                            if (eps.any { it.id == currentEpisodeId }) {
                                currentSeasonNum = s.seasonNumber
                                seasonEpisodes = eps
                                break
                            }
                        }
                    }
                }

                if (seasonEpisodes.isEmpty()) return@launch

                // Find index of current episode in this season
                var currentIndex = seasonEpisodes.indexOfFirst { it.id == currentEpisodeId }
                if (currentIndex == -1 && currentEpNum != null) {
                    currentIndex = seasonEpisodes.indexOfFirst { it.episodeNumber == currentEpNum }
                }

                if (currentIndex != -1 && currentEpNum == null) {
                    currentEpNum = seasonEpisodes[currentIndex].episodeNumber
                }

                if (currentIndex != -1 && currentIndex + 1 < seasonEpisodes.size) {
                    // Next episode is in the same season!
                    val nextEp = seasonEpisodes[currentIndex + 1]
                    _uiState.value = _uiState.value.copy(
                        nextEpisode = NextEpisodeInfo(
                            episode = nextEp,
                            seasonNumber = currentSeasonNum ?: 1,
                            seriesTitle = seriesTitle,
                            seriesSlug = seriesSlug,
                            posterUrl = nextEp.fullStillUrl.ifBlank { posterUrl },
                            backdropUrl = backdropUrl ?: series.fullBackdropUrl
                        )
                    )
                } else if (currentSeasonNum != null) {
                    // Check if there is a next season
                    val nextSeason = allSeasons.firstOrNull { it.seasonNumber > currentSeasonNum }
                    if (nextSeason != null) {
                        val nextSeasonDetail = try {
                            repository.getEpisodeDetail(seriesSlug, nextSeason.seasonNumber, 1)
                        } catch (_: Exception) {
                            null
                        }
                        val nextEp = nextSeasonDetail?.season?.episodes?.firstOrNull()
                            ?: nextSeasonDetail?.episode
                        if (nextEp != null) {
                            _uiState.value = _uiState.value.copy(
                                nextEpisode = NextEpisodeInfo(
                                    episode = nextEp,
                                    seasonNumber = nextSeason.seasonNumber,
                                    seriesTitle = seriesTitle,
                                    seriesSlug = seriesSlug,
                                    posterUrl = nextEp.fullStillUrl.ifBlank { posterUrl },
                                    backdropUrl = backdropUrl ?: series.fullBackdropUrl
                                )
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(nextEpisode = null)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(nextEpisode = null)
                    }
                }
            } catch (e: Exception) {
                Log.w("DetailViewModel", "Failed to resolve next episode: ${e.message}")
            }
        }
    }

    fun dismissNextEpisode() {
        _uiState.value = _uiState.value.copy(isNextEpisodeDismissed = true)
    }

    fun playNextEpisode() {
        val next = _uiState.value.nextEpisode ?: return
        val series = _uiState.value.series ?: return
        _uiState.value = _uiState.value.copy(
            selectedEpisode = next.episode,
            playingEpisode = next.episode,
            playingSeasonNumber = next.seasonNumber
        )
        playEpisode(series, next.seasonNumber, next.episode)
    }

    private var sessionQueryOverride: SubdlQuery? = null
    private var subdlSearchJob: Job? = null

    fun openSubtitlePicker() {
        _uiState.value = _uiState.value.copy(
            isSubtitlePickerVisible = true,
            isManualSearchOpen = false,
            isAppearancePanelOpen = false
        )
        if (_uiState.value.subdlSubtitles.isEmpty() && !_uiState.value.isSearchingSubdl) {
            searchSubdlSubtitles()
        }
    }

    fun closeSubtitlePicker() {
        _uiState.value = _uiState.value.copy(
            isSubtitlePickerVisible = false,
            isManualSearchOpen = false,
            isAppearancePanelOpen = false
        )
    }

    fun searchSubdlSubtitles(queryOverride: SubdlQuery? = null) {
        val targetQuery = queryOverride ?: sessionQueryOverride ?: run {
            val title = currentContentTitle
            if (title.isBlank()) return
            val cleaned = SubdlTitleCleaner.clean(title)
            SubdlQuery(
                filmName = cleaned.title,
                year = cleaned.year,
                type = if (currentContentType == "episode") "tv" else "movie",
                season = currentSeasonNumber,
                episode = currentEpisodeNumber
            )
        }

        subdlSearchJob?.cancel()
        subdlSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearchingSubdl = true,
                subdlActiveQuery = targetQuery,
                subdlError = null,
                isUsingSessionOverride = sessionQueryOverride != null
            )
            when (val result = SubdlRepository.search(targetQuery)) {
                is SubdlSearchResult.Found -> {
                    if (queryOverride != null) {
                        sessionQueryOverride = queryOverride
                    }
                    _uiState.value = _uiState.value.copy(
                        isSearchingSubdl = false,
                        subdlSubtitles = result.subtitles,
                        subdlCandidates = result.candidates,
                        subdlError = null,
                        isUsingSessionOverride = sessionQueryOverride != null
                    )
                }
                is SubdlSearchResult.NotFound -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingSubdl = false,
                        subdlSubtitles = emptyList(),
                        subdlCandidates = result.candidates,
                        subdlError = null,
                        isUsingSessionOverride = sessionQueryOverride != null
                    )
                }
                is SubdlSearchResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingSubdl = false,
                        subdlSubtitles = emptyList(),
                        subdlCandidates = emptyList(),
                        subdlError = result,
                        isUsingSessionOverride = sessionQueryOverride != null
                    )
                }
            }
        }
    }

    fun openManualSearch() {
        val defaultCleaned = SubdlTitleCleaner.clean(currentContentTitle)
        val initialTitle = sessionQueryOverride?.filmName ?: defaultCleaned.title
        val initialYear = (sessionQueryOverride?.year ?: defaultCleaned.year)?.toString() ?: ""
        val initialSeason = (sessionQueryOverride?.season ?: currentSeasonNumber)?.toString() ?: ""
        val initialEpisode = (sessionQueryOverride?.episode ?: currentEpisodeNumber)?.toString() ?: ""

        _uiState.value = _uiState.value.copy(
            isManualSearchOpen = true,
            isAppearancePanelOpen = false,
            manualTitle = initialTitle,
            manualYear = initialYear,
            manualSeason = initialSeason,
            manualEpisode = initialEpisode
        )
    }

    fun closeManualSearch() {
        _uiState.value = _uiState.value.copy(isManualSearchOpen = false)
    }

    fun updateManualTitle(title: String) {
        _uiState.value = _uiState.value.copy(manualTitle = title)
    }

    fun updateManualYear(year: String) {
        _uiState.value = _uiState.value.copy(manualYear = year)
    }

    fun updateManualSeason(season: String) {
        _uiState.value = _uiState.value.copy(manualSeason = season)
    }

    fun updateManualEpisode(episode: String) {
        _uiState.value = _uiState.value.copy(manualEpisode = episode)
    }

    fun submitManualSearch() {
        val title = _uiState.value.manualTitle.trim()
        if (title.length < 2) return

        val yearInt = _uiState.value.manualYear.trim().toIntOrNull()
        val seasonInt = _uiState.value.manualSeason.trim().toIntOrNull()
        val episodeInt = _uiState.value.manualEpisode.trim().toIntOrNull()

        val query = SubdlQuery(
            filmName = title,
            year = yearInt,
            type = if (currentContentType == "episode" || seasonInt != null) "tv" else "movie",
            season = seasonInt,
            episode = episodeInt
        )

        _uiState.value = _uiState.value.copy(isManualSearchOpen = false)
        searchSubdlSubtitles(queryOverride = query)
    }

    fun selectSubdlCandidate(candidate: SubdlItem) {
        val query = SubdlQuery(
            sdId = candidate.sd_id,
            filmName = candidate.name,
            year = candidate.year,
            type = candidate.type ?: if (currentContentType == "episode") "tv" else "movie",
            season = currentSeasonNumber,
            episode = currentEpisodeNumber
        )
        _uiState.value = _uiState.value.copy(isManualSearchOpen = false)
        searchSubdlSubtitles(queryOverride = query)
    }

    fun restoreOriginalTitle() {
        sessionQueryOverride = null
        _uiState.value = _uiState.value.copy(
            isUsingSessionOverride = false,
            isManualSearchOpen = false
        )
        searchSubdlSubtitles(queryOverride = null)
    }

    fun openAppearancePanel() {
        _uiState.value = _uiState.value.copy(
            isAppearancePanelOpen = true,
            isManualSearchOpen = false
        )
    }

    fun closeAppearancePanel() {
        _uiState.value = _uiState.value.copy(isAppearancePanelOpen = false)
    }

    fun selectSubtitleTrack(track: SubtitleTrack?) {
        _uiState.value = _uiState.value.copy(
            selectedSubtitle = track,
            isSubtitleEnabled = track != null,
            isSubtitlePickerVisible = false
        )
    }

    fun selectPreferredLanguage(targetLang: String) {
        if (targetLang.equals("off", ignoreCase = true)) {
            selectSubtitleTrack(null)
            return
        }

        val langCode = if (targetLang.equals("id", ignoreCase = true)) "id" else "en"
        val currentSubs = _uiState.value.resolvedStream?.subtitles.orEmpty()

        // 1. Check if track for this language is already present in resolvedStream.subtitles
        val existingTrack = currentSubs.firstOrNull {
            if (langCode == "id") {
                it.lang.equals("id", ignoreCase = true) || it.lang.equals("in", ignoreCase = true)
            } else {
                it.lang.equals("en", ignoreCase = true)
            }
        }

        if (existingTrack != null) {
            selectSubtitleTrack(existingTrack)
            return
        }

        // 2. Check if subdlSubtitles already loaded has a matching subtitle
        val existingSubdl = _uiState.value.subdlSubtitles.firstOrNull {
            if (langCode == "id") {
                it.language.equals("ID", ignoreCase = true) || it.lang.equals("indonesian", ignoreCase = true)
            } else {
                it.language.equals("EN", ignoreCase = true) || it.lang.equals("english", ignoreCase = true)
            }
        }

        if (existingSubdl != null) {
            selectSubdlSubtitle(existingSubdl) {}
            return
        }

        // 3. Not yet downloaded: fetch from SubDL in background for this specific language
        val requestContentId = currentContentId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingSubdl = true)
            val ctx = getApplication<Application>().applicationContext
            val downloaded = SubdlRepository.fetchSubtitleForLanguage(
                title = currentContentTitle,
                type = if (currentContentType == "episode") "tv" else "movie",
                season = currentSeasonNumber,
                episode = currentEpisodeNumber,
                targetLang = langCode,
                context = ctx
            )
            if (currentContentId != requestContentId) return@launch
            _uiState.value = _uiState.value.copy(isSearchingSubdl = false)

            if (downloaded != null) {
                val currentStream = _uiState.value.resolvedStream
                val updatedSubs = currentStream?.subtitles?.toMutableList() ?: mutableListOf()
                updatedSubs.removeAll { it.path == downloaded.path }
                updatedSubs.add(0, downloaded)
                _uiState.value = _uiState.value.copy(
                    resolvedStream = currentStream?.copy(subtitles = updatedSubs),
                    selectedSubtitle = downloaded,
                    isSubtitleEnabled = true,
                    isSubtitlePickerVisible = false
                )
            } else {
                // If direct auto-download didn't return a match, open the modal sheet so the user can pick/search
                openSubtitlePicker()
            }
        }
    }

    fun selectSubdlSubtitle(
        sub: SubdlSubtitle,
        onSubtitleReady: (SubtitleTrack) -> Unit
    ) {
        val requestContentId = currentContentId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingSubdl = true)
            val downloaded = SubdlRepository.downloadSubtitle(
                sub = sub,
                context = getApplication<Application>().applicationContext
            )
            if (currentContentId != requestContentId) return@launch
            _uiState.value = _uiState.value.copy(isSearchingSubdl = false)

            if (downloaded != null) {
                val currentStream = _uiState.value.resolvedStream
                val updatedSubs = currentStream?.subtitles?.toMutableList() ?: mutableListOf()
                updatedSubs.removeAll { it.path == downloaded.path }
                updatedSubs.add(0, downloaded)
                _uiState.value = _uiState.value.copy(
                    resolvedStream = currentStream?.copy(subtitles = updatedSubs),
                    selectedSubtitle = downloaded,
                    isSubtitleEnabled = true,
                    isSubtitlePickerVisible = false,
                    subdlError = null
                )
                onSubtitleReady(downloaded)
            } else {
                _uiState.value = _uiState.value.copy(
                    subdlError = SubdlSearchResult.Error(
                        SubdlSearchResult.Error.Reason.NETWORK,
                        "Gagal mengunduh subtitle SubDL. Silakan pilih alternatif subtitle lain."
                    )
                )
            }
        }
    }

    fun toggleSubtitle() {
        val current = _uiState.value.isSubtitleEnabled
        _uiState.value = _uiState.value.copy(isSubtitleEnabled = !current)
    }

    fun toggleBookmark(contentType: String, slug: String) {
        val bookmarkKey = "${if (contentType == "episode" || contentType == "tv_series") "series" else contentType}:$slug"
        val newValue = !_uiState.value.isBookmarked
        bookmarkPrefs.edit().putBoolean(bookmarkKey, newValue).apply()
        _uiState.value = _uiState.value.copy(isBookmarked = newValue)
    }

    fun saveProgress(currentPosMs: Long, durationMs: Long) {
        if (currentContentId.isNotBlank() && durationMs > 0) {
            historyStore.updateProgress(currentContentId, currentPosMs, durationMs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        resolveNextEpisodeJob?.cancel()
    }
}

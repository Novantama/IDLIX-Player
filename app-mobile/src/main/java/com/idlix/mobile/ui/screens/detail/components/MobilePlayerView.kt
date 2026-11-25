package com.idlix.mobile.ui.screens.detail.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.idlix.data.model.ResolvedPlayback
import com.idlix.data.model.SubtitleTrack
import com.idlix.data.local.PlayerAppearanceStore
import com.idlix.player.ui.SubtitleOverlay
import com.idlix.player.ui.rememberPlayerCues
import com.idlix.mobile.R
import com.idlix.mobile.player.MediaSourceHelper
import com.idlix.mobile.player.PipController
import com.idlix.mobile.ui.screens.detail.NextEpisodeInfo
import com.idlix.mobile.ui.theme.BackgroundElevated
import com.idlix.mobile.ui.theme.IdlixRed
import com.idlix.mobile.ui.theme.TextMuted
import com.idlix.mobile.ui.theme.TextWhite
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(UnstableApi::class)
@Composable
fun MobilePlayerView(
    title: String,
    subtitle: String?,
    resolvedStream: ResolvedPlayback?,
    isStreamLoading: Boolean,
    countdownSeconds: Int,
    streamError: String?,
    selectedSubtitle: SubtitleTrack?,
    isSubtitleEnabled: Boolean,
    initialPositionMs: Long,
    isFullscreen: Boolean,
    nextEpisode: NextEpisodeInfo?,
    isNextEpisodeDismissed: Boolean,
    previewImageUrl: String? = null,
    onPlayClick: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    onOpenSubtitlePicker: () -> Unit,
    onBackClick: () -> Unit,
    onPlayNextEpisode: () -> Unit,
    onDismissNextEpisode: () -> Unit,
    onRetryPlayback: () -> Unit,
    onSaveProgress: (currentPosMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isInPipMode by PipController.isInPipMode.collectAsState()
    val appearance by PlayerAppearanceStore.appearance.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }

    // Gestures feedback state
    var brightnessIndicator by remember { mutableStateOf<Float?>(null) }
    var volumeIndicator by remember { mutableStateOf<Float?>(null) }
    var doubleTapSeekFeedback by remember { mutableStateOf<Int?>(null) }

    var userInteractionCount by remember { mutableIntStateOf(0) }
    var activePlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(showControls, isPlaying, userInteractionCount) {
        if (showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    // Keep PipController in sync with playback status
    LaunchedEffect(isPlaying, resolvedStream) {
        PipController.isPlaybackActive = (resolvedStream != null && isPlaying)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(appearance.backgroundColor))
    ) {
        if (isStreamLoading) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = IdlixRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (countdownSeconds > 0) {
                    Text(
                        text = stringResource(R.string.loading_secure_stream),
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.starting_in_seconds, countdownSeconds),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.connecting_to_media),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (streamError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = streamError,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onRetryPlayback,
                        colors = ButtonDefaults.buttonColors(containerColor = IdlixRed)
                    ) {
                        Text(text = stringResource(R.string.try_again))
                    }
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(text = stringResource(R.string.back))
                    }
                }
            }
        } else if (resolvedStream != null) {
            val tokenQuery = remember(resolvedStream.hlsMasterUrl) {
                try {
                    Uri.parse(resolvedStream.hlsMasterUrl).query.orEmpty()
                } catch (_: Exception) {
                    ""
                }
            }

            val httpDataSourceFactory = remember(tokenQuery) {
                MediaSourceHelper.createHttpDataSourceFactory(tokenQuery)
            }

            val exoPlayer = remember(context, resolvedStream.hlsMasterUrl) {
                val renderersFactory = MediaSourceHelper.createRenderersFactory(context)
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15000, 50000, 2500, 5000)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()

                ExoPlayer.Builder(context, renderersFactory)
                    .setLoadControl(loadControl)
                    .build().apply {
                        val activeSub = if (isSubtitleEnabled) selectedSubtitle else null
                        val mediaSource = MediaSourceHelper.buildMediaSource(
                            context = context,
                            masterUrl = resolvedStream.hlsMasterUrl,
                            sub = activeSub,
                            httpDataSourceFactory = httpDataSourceFactory
                        )
                        setMediaSource(mediaSource)

                        val prefLang = com.idlix.data.util.LocaleHelper.getPreferredLanguage()
                        val targetLang = activeSub?.lang ?: prefLang
                        trackSelectionParameters = trackSelectionParameters.buildUpon()
                            .setPreferredTextLanguage(targetLang)
                            .setSelectUndeterminedTextLanguage(true)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !isSubtitleEnabled)
                            .build()

                        prepare()
                        if (initialPositionMs > 0L) {
                            seekTo(initialPositionMs)
                        }
                        playWhenReady = true
                    }
            }

            var lastMergedSubtitle by remember(exoPlayer) {
                mutableStateOf(if (isSubtitleEnabled) selectedSubtitle else null)
            }
            var isInitialSubtitleEffect by remember(exoPlayer) { mutableStateOf(true) }

            // Subtitle track updates without recreating ExoPlayer instance
            LaunchedEffect(exoPlayer, selectedSubtitle, isSubtitleEnabled) {
                if (isInitialSubtitleEffect) {
                    isInitialSubtitleEffect = false
                    return@LaunchedEffect
                }
                try {
                    val currentTrack = if (isSubtitleEnabled) selectedSubtitle else null
                    val needsMediaRebuild = currentTrack != lastMergedSubtitle && (currentTrack != null || lastMergedSubtitle != null)
                    if (needsMediaRebuild) {
                        val wasPlaying = exoPlayer.isPlaying
                        val currentPos = exoPlayer.currentPosition
                        val newSource = MediaSourceHelper.buildMediaSource(
                            context = context,
                            masterUrl = resolvedStream.hlsMasterUrl,
                            sub = currentTrack,
                            httpDataSourceFactory = httpDataSourceFactory
                        )
                        lastMergedSubtitle = currentTrack
                        exoPlayer.setMediaSource(newSource, currentPos)
                        exoPlayer.prepare()
                        if (wasPlaying) {
                            exoPlayer.play()
                        }
                    }

                    val selector = exoPlayer.trackSelectionParameters.buildUpon()
                    if (currentTrack != null) {
                        val lang = currentTrack.lang.lowercase()
                        val langs = if (lang == "id" || lang == "in" || lang == "ind") {
                            arrayOf("id", "in", "ind", "indonesian")
                        } else {
                            arrayOf(lang)
                        }
                        selector
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setPreferredTextLanguages(*langs)
                            .setSelectUndeterminedTextLanguage(true)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    } else {
                        selector
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    }
                    exoPlayer.trackSelectionParameters = selector.build()
                } catch (_: Exception) {}
            }

            LaunchedEffect(exoPlayer) {
                activePlayer = exoPlayer
            }

            // Sync current position and periodically record progress
            LaunchedEffect(exoPlayer) {
                var lastSavedPos = 0L
                while (true) {
                    currentPosMs = exoPlayer.currentPosition
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    isPlaying = exoPlayer.isPlaying
                    if (Math.abs(currentPosMs - lastSavedPos) >= 5000L && durationMs > 0) {
                        onSaveProgress(currentPosMs, durationMs)
                        lastSavedPos = currentPosMs
                    }
                    delay(500)
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            val latestNextEpisode by rememberUpdatedState(nextEpisode)
            val latestIsNextDismissed by rememberUpdatedState(isNextEpisodeDismissed)
            val latestOnPlayNextEpisode by rememberUpdatedState(onPlayNextEpisode)
            val latestOnSaveProgress by rememberUpdatedState(onSaveProgress)

            DisposableEffect(lifecycleOwner, exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            if (latestNextEpisode != null && !latestIsNextDismissed) {
                                latestOnPlayNextEpisode()
                            }
                        }
                    }
                }
                exoPlayer.addListener(listener)

                val lifecycleObserver = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        try {
                            val pos = exoPlayer.currentPosition
                            val dur = exoPlayer.duration
                            if (dur > 0L) {
                                latestOnSaveProgress(pos, dur)
                            }
                            if (exoPlayer.isPlaying || exoPlayer.playWhenReady) {
                                exoPlayer.pause()
                            }
                        } catch (_: Exception) {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

                onDispose {
                    try {
                        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                        exoPlayer.removeListener(listener)
                        val finalPos = exoPlayer.currentPosition
                        val finalDur = exoPlayer.duration
                        if (finalDur > 0) {
                            latestOnSaveProgress(finalPos, finalDur)
                        }
                    } catch (_: Exception) {}
                    PipController.isPlaybackActive = false
                    activePlayer = null
                    exoPlayer.release()
                }
            }

            val cues by rememberPlayerCues(exoPlayer)

            // ExoPlayer AndroidView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        setShutterBackgroundColor(appearance.backgroundColor)
                        subtitleView?.visibility = android.view.View.GONE
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.setShutterBackgroundColor(appearance.backgroundColor)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Custom Subtitle Overlay with free positioning & user appearance
            if (isSubtitleEnabled) {
                SubtitleOverlay(
                    cues = cues,
                    appearance = appearance,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Touch Gestures Layer (Single tap, Double tap seek, Vertical Drag for Brightness & Volume)
            if (!isInPipMode) {
                var dragStartX by remember { mutableFloatStateOf(0f) }
                var dragVolume by remember { mutableFloatStateOf(0f) }
                var lastAppliedVol by remember { mutableIntStateOf(-1) }
                var hideHudJob by remember { mutableStateOf<Job?>(null) }
                var doubleTapHudJob by remember { mutableStateOf<Job?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(exoPlayer) {
                            detectTapGestures(
                                onTap = {
                                    showControls = !showControls
                                    userInteractionCount++
                                },
                                onDoubleTap = { offset ->
                                    val isRight = offset.x >= size.width / 2
                                    val delta = if (isRight) 10_000L else -10_000L
                                    val dur = exoPlayer.duration
                                    val rawTarget = (exoPlayer.currentPosition + delta).coerceAtLeast(0L)
                                    val targetPos = if (dur > 0L) rawTarget.coerceAtMost(dur) else rawTarget
                                    exoPlayer.seekTo(targetPos)
                                    doubleTapSeekFeedback = if (isRight) 10 else -10
                                    userInteractionCount++
                                    doubleTapHudJob?.cancel()
                                    doubleTapHudJob = scope.launch {
                                        delay(800)
                                        doubleTapSeekFeedback = null
                                    }
                                }
                            )
                        }
                        .pointerInput(exoPlayer) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    dragStartX = offset.x
                                    if (audioManager != null) {
                                        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        dragVolume = curVol.toFloat()
                                        lastAppliedVol = curVol
                                    }
                                    userInteractionCount++
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val isRight = dragStartX >= size.width / 2

                                    if (isRight && audioManager != null) {
                                        // Adjust Volume (Right) smoothly proportional to drag distance
                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        if (maxVol > 0) {
                                            dragVolume = (dragVolume - dragAmount / 350f * maxVol).coerceIn(0f, maxVol.toFloat())
                                            val newVol = dragVolume.roundToInt().coerceIn(0, maxVol)
                                            if (newVol != lastAppliedVol) {
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                lastAppliedVol = newVol
                                            }
                                            volumeIndicator = dragVolume / maxVol.toFloat()
                                        }
                                    } else {
                                        // Adjust Brightness (Left)
                                        val activity = context.findActivity()
                                        val window = activity?.window
                                        if (window != null) {
                                            val lp = window.attributes
                                            val currentBright = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                            val delta = -dragAmount / 350f
                                            val newBright = (currentBright + delta).coerceIn(0.01f, 1f)
                                            lp.screenBrightness = newBright
                                            window.attributes = lp
                                            brightnessIndicator = newBright
                                        }
                                    }

                                    hideHudJob?.cancel()
                                    hideHudJob = scope.launch {
                                        delay(1500)
                                        volumeIndicator = null
                                        brightnessIndicator = null
                                    }
                                }
                            )
                        }
                )

                // Overlay Controls & HUDs
                MobilePlayerOverlay(
                    showControls = showControls,
                    title = title,
                    subtitle = subtitle,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosMs,
                    durationMs = durationMs,
                    isFullscreen = isFullscreen,
                    isSubtitleEnabled = isSubtitleEnabled,
                    selectedSubtitle = selectedSubtitle,
                    nextEpisode = nextEpisode,
                    isNextEpisodeDismissed = isNextEpisodeDismissed,
                    brightnessIndicator = brightnessIndicator,
                    volumeIndicator = volumeIndicator,
                    doubleTapSeekFeedback = doubleTapSeekFeedback,
                    onTogglePlayPause = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        userInteractionCount++
                    },
                    onSeekBy = { deltaMs ->
                        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
                        exoPlayer.seekTo(target)
                        userInteractionCount++
                    },
                    onSeekTo = { targetMs ->
                        exoPlayer.seekTo(targetMs)
                        userInteractionCount++
                    },
                    onToggleFullscreen = onToggleFullscreen,
                    onOpenSubtitlePicker = onOpenSubtitlePicker,
                    onBackClick = onBackClick,
                    onPlayNextEpisode = onPlayNextEpisode,
                    onDismissNextEpisode = onDismissNextEpisode,
                    onUserInteraction = { userInteractionCount++ }
                )
            }
        } else {
            // Idle state: Video not playing automatically, wait for manual user trigger
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPlayClick)
            ) {
                if (!previewImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = previewImageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundElevated)
                    )
                }

                // Dark gradient scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Top bar with Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Play Button
                Surface(
                    color = IdlixRed,
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Left Title / Subtitle preview badge
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = IdlixRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

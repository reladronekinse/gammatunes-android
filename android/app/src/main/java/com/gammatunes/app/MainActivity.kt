package com.gammatunes.app

import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import com.gammatunes.app.ui.components.LocalDockInset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import android.Manifest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.gammatunes.app.model.Track
import com.gammatunes.app.player.PlayerState
import com.gammatunes.app.player.rememberPlayerState
import com.gammatunes.app.ui.screens.AlbumDetailScreen
import com.gammatunes.app.ui.screens.ArtistDetailScreen
import com.gammatunes.app.ui.screens.ArtistReleaseKind
import com.gammatunes.app.ui.screens.ArtistAlbumsGridScreen
import com.gammatunes.app.ui.screens.ArtistSongsScreen
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.ui.screens.MoreScreen
import com.gammatunes.app.ui.screens.MusicSectionScreen
import com.gammatunes.app.ui.screens.SettingsSectionScreen
import com.gammatunes.app.ui.screens.CacheScreen
import com.gammatunes.app.ui.screens.AccountScreen
import com.gammatunes.app.ui.screens.AppearanceScreen
import com.gammatunes.app.ui.screens.PlaybackScreen
import com.gammatunes.app.ui.screens.EqualizerScreen
import com.gammatunes.app.ui.screens.TogetherScreen
import com.gammatunes.app.together.TogetherSession
import androidx.compose.runtime.DisposableEffect
import com.gammatunes.app.ui.screens.UpdateScreen
import com.gammatunes.app.ui.screens.OfflineTracksScreen
import com.gammatunes.app.ui.screens.OfflineAlbumDetailScreen
import com.gammatunes.app.ui.screens.OfflineAlbumsScreen
import com.gammatunes.app.ui.screens.TopTracksScreen
import com.gammatunes.app.ui.screens.TopArtistsScreen
import com.gammatunes.app.ui.screens.PlayerScreen
import com.gammatunes.app.ui.screens.HomeScreen
import com.gammatunes.app.ui.screens.QueueScreen
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.player.LocalPlayerState
import com.gammatunes.app.network.NetworkMonitor
import com.gammatunes.app.offline.OfflineModeRepository
import com.gammatunes.app.ui.screens.SearchScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import com.gammatunes.app.ui.theme.DynamicAccent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gammatunes.app.ui.i18n.LocalLanguage
import com.gammatunes.app.ui.i18n.LocalStrings
import com.gammatunes.app.ui.i18n.LocaleRepository
import com.gammatunes.app.ui.i18n.stringsFor
import com.gammatunes.app.ui.onboarding.OnboardingRepository
import com.gammatunes.app.ui.onboarding.OnboardingFlow
import com.gammatunes.app.ui.theme.GammaTunesTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Новым пользователям разрешения запрашивает экран онбординга; здесь — только для тех, кто его уже прошёл
        if (OnboardingRepository.completed.value) requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            GammaTunesTheme {
                App()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001,
                )
            }
        }
    }
}

private sealed class Screen(val route: String, val icon: ImageVector) {
    data object Home : Screen("home", Icons.Default.Home)
    data object Search : Screen("search", Icons.Default.Search)
    data object Player : Screen("player", Icons.Default.MusicNote)
    data object More : Screen("more", Icons.Default.MoreHoriz)
}

private val bottomTabs = listOf(Screen.Home, Screen.Search, Screen.Player, Screen.More)

// ---- Screen transitions -------------------------------------------------------------
private const val FADE_OUT_MS = 90
private const val FADE_IN_MS = 210
private const val SLIDE_MS = 320
// Fraction of the screen width the content travels while sliding (subtle, not a full page swipe).
private const val SLIDE_FRACTION = 8

private fun isTabSwitch(from: String?, to: String?, tabs: Set<String>): Boolean =
    from != null && to != null && from in tabs && to in tabs

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearOutSlowInEasing)) +
        scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearOutSlowInEasing),
        )

private fun fadeThroughOut(): ExitTransition =
    fadeOut(tween(FADE_OUT_MS, easing = FastOutLinearInEasing))

private fun pushEnter(): EnterTransition =
    slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it / SLIDE_FRACTION } +
        fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearOutSlowInEasing))

private fun pushExit(): ExitTransition =
    slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it / SLIDE_FRACTION } +
        fadeOut(tween(FADE_OUT_MS, easing = FastOutLinearInEasing))

private fun popEnter(): EnterTransition =
    slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it / SLIDE_FRACTION } +
        fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearOutSlowInEasing))

private fun popExit(): ExitTransition =
    slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it / SLIDE_FRACTION } +
        fadeOut(tween(FADE_OUT_MS, easing = FastOutLinearInEasing))

@Composable
fun App() {
    val lang by LocaleRepository.language.collectAsState()
    val strings = stringsFor(lang)
    val onboardingDone by OnboardingRepository.completed.collectAsState()
    val context = LocalContext.current
    CompositionLocalProvider(
        LocalLanguage provides lang,
        LocalStrings provides strings,
    ) {
        // Первичная настройка показывается только при первом запуске. Основной UI (плеер,
        // бэкенд-запросы и т.д.) не создаётся, пока пользователь не пройдёт или не пропустит её.
        Crossfade(targetState = onboardingDone, label = "onboarding") { done ->
            if (done) {
                AppContent()
            } else {
                OnboardingFlow(onFinish = { OnboardingRepository.complete(context) })
            }
        }
    }
}

@Composable
private fun AppContent() {
    val strings = LocalStrings.current
    val navController = rememberNavController()
    val playerState = rememberPlayerState()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route


    LaunchedEffect(playerState.currentTrack?.thumbnail) {
        val thumb = playerState.currentTrack?.thumbnail
        if (thumb.isNullOrBlank()) {
            DynamicAccent.clear()
        } else {
            DynamicAccent.updateFromThumbnail(context, thumb)
        }
    }

    // «Слушать вместе»: сессия живёт глобально и общается с плеером через хуки PlayerState
    DisposableEffect(playerState) {
        TogetherSession.attach(playerState)
        onDispose { TogetherSession.detach(playerState) }
    }

    val mainTabRoutes = setOf(Screen.Home.route, Screen.Search.route, Screen.Player.route, Screen.More.route)

    // Leave any detail screen (artist/album/more/...) then land on a bottom tab.
    fun navigateToTab(targetRoute: String) {
        // Explicitly pop detail destinations that sit on top of tabs.
        // Without this, artist/album can "trap" the stack so tab clicks appear to do nothing.
        var guard = 0
        while (guard++ < 40) {
            val route = navController.currentDestination?.route ?: break
            val isMainTab = route in mainTabRoutes
            if (isMainTab) break
            if (!navController.popBackStack()) break
        }
        val current = navController.currentDestination?.route
        if (current == targetRoute) return
        navController.navigate(targetRoute) {
            popUpTo(Screen.Home.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openPlayerTab() = navigateToTab(Screen.Player.route)




    val onTrackClick: (Track, List<Track>) -> Unit = { track, queue ->
        playerState.play(track, queue)
        openPlayerTab()
    }



    val online by NetworkMonitor.isOnline.collectAsState()
    val offlineMode by OfflineModeRepository.enabled.collectAsState()
    var offerDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(online) { if (online) offerDismissed = false }

    CompositionLocalProvider(LocalPlayerState provides playerState) {
    if (!online && !offlineMode && !offerDismissed) {
        AlertDialog(
            onDismissRequest = { offerDismissed = true },
            title = { Text(strings.offlineDialogTitle) },
            text = { Text(strings.offlineDialogText) },
            confirmButton = {
                TextButton(onClick = { OfflineModeRepository.set(true) }) {
                    Text(strings.offlineDialogEnable)
                }
            },
            dismissButton = {
                TextButton(onClick = { offerDismissed = true }) { Text(strings.offlineDialogStay) }
            },
        )
    }
    // Height of the floating dock (capsule + system nav inset); screens use it as scroll reserve.
    var dockHeightPx by remember { mutableIntStateOf(0) }
    val dockInset = with(LocalDensity.current) { dockHeightPx.toDp() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (offlineMode) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (online) strings.offlineBannerOnline else strings.offlineBannerOn,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (online) {
                        TextButton(onClick = { OfflineModeRepository.set(false) }) {
                            Text(strings.offlineGoOnline)
                        }
                    }
                }
            }
        }
        CompositionLocalProvider(LocalDockInset provides dockInset) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            // Tab <-> tab: "fade through" (old screen fades out first, the new one fades in
            // after it), so two screens are never visible on top of each other — that was
            // the old Player-over-More glitch with the default crossfade.
            // Anything else (artist, album, queue, settings sub-screens): shared-axis slide.
            enterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route, mainTabRoutes)) {
                    fadeThroughIn()
                } else {
                    pushEnter()
                }
            },
            exitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route, mainTabRoutes)) {
                    fadeThroughOut()
                } else {
                    pushExit()
                }
            },
            popEnterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route, mainTabRoutes)) {
                    fadeThroughIn()
                } else {
                    popEnter()
                }
            },
            popExitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route, mainTabRoutes)) {
                    fadeThroughOut()
                } else {
                    popExit()
                }
            },
        ) {
            paddedComposable(Screen.Home.route) {
                HomeScreen(
                    onTrackClick = { track, queue -> onTrackClick(track, queue) },
                )
            }
            paddedComposable(Screen.Search.route) {
                SearchScreen(
                    onArtistClick = { artist ->
                        val encodedId = URLEncoder.encode(artist.artistId, "UTF-8")
                        navController.navigate("artist/$encodedId")
                    },
                    onTrackClick = { track, queue -> onTrackClick(track, queue) },
                )
            }
            composable(
                route = "artist/{artistId}",
                arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val encodedId = backStackEntry.arguments?.getString("artistId").orEmpty()
                val artistIdDecoded = URLDecoder.decode(encodedId, "UTF-8")
                ArtistDetailScreen(
                    artistId = artistIdDecoded,
                    onAlbumClick = { album ->
                        val encodedAlbumId = URLEncoder.encode(album.albumId, "UTF-8")
                        navController.navigate("album/$encodedAlbumId")
                    },
                    onTrackClick = onTrackClick,
                    onOpenPopular = {
                        navController.navigate("artist/$encodedId/songs")
                    },
                    onOpenAlbums = {
                        navController.navigate("artist/$encodedId/albums")
                    },
                    onOpenSingles = {
                        navController.navigate("artist/$encodedId/singles")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable(
                route = "artist/{artistId}/songs",
                arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
            ) { entry ->
                val encodedId = entry.arguments?.getString("artistId").orEmpty()
                ArtistSongsScreen(
                    artistId = URLDecoder.decode(encodedId, "UTF-8"),
                    onTrackClick = onTrackClick,
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable(
                route = "artist/{artistId}/albums",
                arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
            ) { entry ->
                val encodedId = entry.arguments?.getString("artistId").orEmpty()
                ArtistAlbumsGridScreen(
                    artistId = URLDecoder.decode(encodedId, "UTF-8"),
                    kind = ArtistReleaseKind.ALBUMS,
                    onAlbumClick = { album ->
                        val encodedAlbumId = URLEncoder.encode(album.albumId, "UTF-8")
                        navController.navigate("album/$encodedAlbumId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable(
                route = "artist/{artistId}/singles",
                arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
            ) { entry ->
                val encodedId = entry.arguments?.getString("artistId").orEmpty()
                ArtistAlbumsGridScreen(
                    artistId = URLDecoder.decode(encodedId, "UTF-8"),
                    kind = ArtistReleaseKind.SINGLES,
                    onAlbumClick = { album ->
                        val encodedAlbumId = URLEncoder.encode(album.albumId, "UTF-8")
                        navController.navigate("album/$encodedAlbumId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "album/{albumId}",
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val encodedAlbumId = backStackEntry.arguments?.getString("albumId").orEmpty()
                AlbumDetailScreen(
                    albumId = URLDecoder.decode(encodedAlbumId, "UTF-8"),
                    onTrackClick = onTrackClick,
                    onBack = { navController.popBackStack() },
                )


            }
            paddedComposable("queue") {
                QueueScreen(player = playerState, onBack = { navController.popBackStack() })
            }
            paddedComposable(Screen.Player.route) {
                PlayerScreen(
                    player = playerState,
                    onOpenQueue = { navController.navigate("queue") },
                    onArtistClick = { artistId ->
                        val encodedId = URLEncoder.encode(artistId, "UTF-8")
                        navController.navigate("artist/$encodedId")
                    },
                    onAlbumClick = { albumId ->
                        val encodedAlbumId = URLEncoder.encode(albumId, "UTF-8")
                        navController.navigate("album/$encodedAlbumId")
                    },
                )
            }
            paddedComposable(Screen.More.route) {
                MoreScreen(
                    onOpenMusic = { navController.navigate("more/section/music") },
                    onOpenSettings = { navController.navigate("more/section/settings") },
                    onOpenUpdates = { navController.navigate("more/updates") },
                )
            }
            paddedComposable("more/section/music") {
                MusicSectionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenOfflineTracks = { navController.navigate("more/offline_tracks") },
                    onOpenOfflineAlbums = { navController.navigate("more/offline_albums") },
                    onOpenTopTracks = { navController.navigate("more/top_tracks") },
                    onOpenTopArtists = { navController.navigate("more/top_artists") },
                )
            }
            paddedComposable("more/section/settings") {
                SettingsSectionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAccount = { navController.navigate("more/account") },
                    onOpenAppearance = { navController.navigate("more/appearance") },
                    onOpenPlayback = { navController.navigate("more/playback") },
                    onOpenEqualizer = { navController.navigate("more/equalizer") },
                    onOpenTogether = { navController.navigate("more/together") },
                    onOpenCache = { navController.navigate("more/cache") },
                )
            }
            paddedComposable("more/cache") {
                CacheScreen(onBack = { navController.popBackStack() })
            }
            paddedComposable("more/playback") {
                PlaybackScreen(onBack = { navController.popBackStack() })
            }
            paddedComposable("more/together") {
                TogetherScreen(onBack = { navController.popBackStack() })
            }
            paddedComposable("more/equalizer") {
                EqualizerScreen(onBack = { navController.popBackStack() })
            }
            paddedComposable("more/updates") {
                UpdateScreen(onBack = { navController.popBackStack() })
            }
            paddedComposable("more/top_tracks") {
                TopTracksScreen(
                    onTrackClick = onTrackClick,
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable("more/top_artists") {
                TopArtistsScreen(
                    onArtistClick = { artistId ->
                        val encodedId = URLEncoder.encode(artistId, "UTF-8")
                        navController.navigate("artist/$encodedId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable("more/account") {
                AccountScreen(
                    onTrackClick = onTrackClick,
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable("more/appearance") {
                AppearanceScreen(onBack = { navController.popBackStack() })
            }
            paddedComposable("more/offline_tracks") {
                OfflineTracksScreen(
                    onTrackClick = onTrackClick,
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable("more/offline_albums") {
                OfflineAlbumsScreen(
                    onAlbumClick = { albumId ->

                        val encoded = URLEncoder.encode(albumId, "UTF-8")
                        navController.navigate("more/offline_album/$encoded")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            paddedComposable(
                route = "more/offline_album/{albumId}",
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
            ) { entry ->
                val encoded = entry.arguments?.getString("albumId").orEmpty()
                OfflineAlbumDetailScreen(
                    albumId = URLDecoder.decode(encoded, "UTF-8"),
                    onTrackClick = onTrackClick,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }


        val playingTrack = playerState.currentTrack
        val showMiniPlayer = playingTrack != null &&
                currentRoute != Screen.Player.route &&
                currentRoute != "queue"

        // Floating dock is an overlay, not a sibling below the content.
        // This lets the screen continue underneath the dock instead of creating
        // an opaque/empty strip at the bottom of the app.
        // Floating dock: the mini-player and bottom tabs share one glass capsule.
        // When there is no mini-player, the same capsule simply contains the tabs.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { dockHeightPx = it.height }
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    // Fully opaque neutral dock. Nothing from the screen underneath
                    // is allowed to bleed through the capsule.
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF242424),
                                Color(0xFF171717),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF4A4A4A),
                        shape = RoundedCornerShape(28.dp),
                    ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showMiniPlayer) {
                        MiniPlayerBar(
                            track = playingTrack!!,
                            player = playerState,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            thickness = 1.dp,
                        )
                    }
                    BottomBar(
                        currentRoute = currentRoute,
                        onTabSelected = { navigateToTab(it) },
                    )
                }
            }
        }
    }
    }
}

/**
 * Обычный экран: контент начинается под статус-баром.
 * Экраны с баннером (артист, альбом) регистрируются через обычный composable —
 * они сами рисуют картинку от самого верха и сами учитывают статус-бар.
 */
private fun NavGraphBuilder.paddedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments) { entry ->
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            content(entry)
        }
    }
}

/**
 * Мини-плеер:
 *  - тап по плееру — пауза/воспроизведение (в полный плеер не переходит);
 *  - горизонтальный свайп — следующий/предыдущий трек;
 *  - внизу по центру — seek bar.
 */
@Composable
private fun MiniPlayerBar(
    track: Track,
    player: PlayerState,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    val maxShiftPx = with(LocalDensity.current) { 36.dp.toPx() }

    var positionMs by remember(track.videoId) { mutableLongStateOf(0L) }
    var durationMs by remember(track.videoId) {
        mutableLongStateOf(track.durationSeconds?.toLong()?.times(1000L) ?: 0L)
    }
    var isSeeking by remember(track.videoId) { mutableStateOf(false) }
    var seekFraction by remember(track.videoId) { mutableFloatStateOf(0f) }

    LaunchedEffect(track.videoId) {
        while (true) {
            if (!isSeeking) {
                val d = player.durationMs
                if (d > 0L) durationMs = d
                positionMs = if (player.isLoadingStream) 0L else player.positionMs
            }
            delay(250)
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = if (isSeeking) {
        seekFraction
    } else {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { player.togglePlayPause() }
            .pointerInput(player) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val t = total
                        scope.launch { offsetX.animateTo(0f, tween(180)) }
                        if (t <= -swipeThresholdPx) {
                            if (player.hasNext) player.playNext()
                        } else if (t >= swipeThresholdPx) {
                            if (player.hasPrevious) player.playPrevious()
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, tween(180)) }
                    },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        total += dx
                        scope.launch {
                            offsetX.snapTo((total * 0.5f).coerceIn(-maxShiftPx, maxShiftPx))
                        }
                    },
                )
            },
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Пустое место размером с обложку справа — чтобы текст был по центру капсулы.
                Spacer(Modifier.width(50.dp))
            }

            MiniSeekBar(
                progress = progress,
                enabled = durationMs > 0L && !player.isLoadingStream,
                isDragging = isSeeking,
                onChange = { v ->
                    isSeeking = true
                    seekFraction = v
                },
                onFinished = {
                    val target = (seekFraction * durationMs.coerceAtLeast(1L)).toLong()
                    player.seekTo(target)
                    positionMs = target
                    isSeeking = false
                },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 2.dp),
            )
        }
    }
}

/** Тонкий seek bar для мини-плеера: тап и перетаскивание. */
@Composable
private fun MiniSeekBar(
    progress: Float,
    enabled: Boolean,
    isDragging: Boolean,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onChangeState by rememberUpdatedState(onChange)
    val onFinishedState by rememberUpdatedState(onFinished)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val activeColor = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f)
    val barHeight by animateDpAsState(if (isDragging) 5.dp else 3.dp, label = "miniSeekHeight")
    val thumbRadius by animateDpAsState(if (isDragging) 7.dp else 4.dp, label = "miniSeekThumb")

    Box(
        modifier = modifier
            .height(22.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onTap = { pos ->
                        onChangeState((pos.x / size.width).coerceIn(0f, 1f))
                        onFinishedState()
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { pos -> onChangeState((pos.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { onFinishedState() },
                    onDragCancel = { onFinishedState() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onChangeState((change.position.x / size.width).coerceIn(0f, 1f))
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
            val w = size.width
            val cy = size.height / 2f
            val h = barHeight.toPx()
            val x = progress.coerceIn(0f, 1f) * w
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, cy - h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2f),
            )
            if (x > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, cy - h / 2f),
                    size = Size(x, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
            }
            drawCircle(color = activeColor, radius = thumbRadius.toPx(), center = Offset(x, cy))
        }
    }
}

@Composable
private fun BottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    val strings = LocalStrings.current
    fun labelFor(screen: Screen): String = when (screen) {
        Screen.Home -> strings.tabHome
        Screen.Search -> strings.tabSearch
        Screen.Player -> strings.tabPlayer
        Screen.More -> strings.tabMore
    }
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        bottomTabs.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route ||
                        (screen.route == Screen.Player.route && currentRoute == "queue") ||
                        (screen.route == Screen.More.route && currentRoute?.startsWith("more/") == true),
                onClick = { onTabSelected(screen.route) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = labelFor(screen),
                    )
                },
                label = { Text(labelFor(screen)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

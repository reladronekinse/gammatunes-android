package com.gammatunes.app.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    RUSSIAN("ru");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.find { it.code == code } ?: ENGLISH
    }
}

/**
 * Строки UI. Сами значения живут в небольших data-классах-группах,
 * потому что ART отклоняет конструктор с ~287 параметрами (VerifyError).
 * Снаружи по-прежнему плоский доступ: strings.tabHome, strings.onbSkip, …
 */
class AppStrings internal constructor(
    private val g0: AppStringsG0,
    private val g1: AppStringsG1,
    private val g2: AppStringsG2,
    private val g3: AppStringsG3
) {
    val tabSearch: String get() = g0.tabSearch
    val searchHistoryTitle: String get() = g0.searchHistoryTitle
    val searchHistoryClear: String get() = g0.searchHistoryClear
    val searchHistoryRemove: String get() = g0.searchHistoryRemove
    val offlineDialogTitle: String get() = g0.offlineDialogTitle
    val offlineDialogText: String get() = g0.offlineDialogText
    val offlineDialogEnable: String get() = g0.offlineDialogEnable
    val offlineDialogStay: String get() = g0.offlineDialogStay
    val offlineBannerOn: String get() = g0.offlineBannerOn
    val offlineBannerOnline: String get() = g0.offlineBannerOnline
    val offlineGoOnline: String get() = g0.offlineGoOnline
    val offlineModeSwitch: String get() = g0.offlineModeSwitch
    val cachedSearchPlaceholder: String get() = g0.cachedSearchPlaceholder
    val offlineSearchHint: String get() = g0.offlineSearchHint
    val playNext: String get() = g0.playNext
    val playNextAdded: String get() = g0.playNextAdded
    val openQueue: String get() = g0.openQueue
    val queueTitle: String get() = g0.queueTitle
    val queueNowPlaying: String get() = g0.queueNowPlaying
    val queueEmpty: String get() = g0.queueEmpty
    val queueRemove: String get() = g0.queueRemove
    val queueReorder: String get() = g0.queueReorder
    val tabHome: String get() = g0.tabHome
    val homeTitle: String get() = g0.homeTitle
    val homeRecommended: String get() = g0.homeRecommended
    val homeRecommendedHint: String get() = g0.homeRecommendedHint
    val homeRecent: String get() = g0.homeRecent
    val homeTopTracks: String get() = g0.homeTopTracks
    val homeEmpty: String get() = g0.homeEmpty
    val homeError: String get() = g0.homeError
    val homeRetry: String get() = g0.homeRetry
    val homeRefresh: String get() = g0.homeRefresh
    val tabPlayer: String get() = g0.tabPlayer
    val tabLiked: String get() = g0.tabLiked
    val tabMore: String get() = g0.tabMore
    val searchPlaceholder: String get() = g0.searchPlaceholder
    val artists: String get() = g0.artists
    val tracks: String get() = g0.tracks
    val nothingFound: String get() = g0.nothingFound
    val sourceYtm: String get() = g0.sourceYtm
    val sourceSoundCloud: String get() = g0.sourceSoundCloud
    val searchPlaceholderSoundCloud: String get() = g0.searchPlaceholderSoundCloud
    val errorPrefix: String get() = g0.errorPrefix
    val album: String get() = g0.album
    val artist: String get() = g0.artist
    val albumsCount: String get() = g0.albumsCount
    val singlesCount: String get() = g0.singlesCount
    val popularTracks: String get() = g0.popularTracks
    val noAlbums: String get() = g0.noAlbums
    val offlineOrBackendError: String get() = g0.offlineOrBackendError
    val loginWithBrowser: String get() = g0.loginWithBrowser
    val browserLoginTitle: String get() = g0.browserLoginTitle
    val browserLoginHint: String get() = g0.browserLoginHint
    val browserBlockedHint: String get() = g0.browserBlockedHint
    val noTracksInAlbum: String get() = g0.noTracksInAlbum
    val downloadAlbum: String get() = g0.downloadAlbum
    val downloadingAlbum: String get() = g0.downloadingAlbum
    val albumDownloaded: String get() = g0.albumDownloaded
    val back: String get() = g0.back
    val playerEmptyTitle: String get() = g0.playerEmptyTitle
    val playerEmptyHint: String get() = g0.playerEmptyHint
    val pause: String get() = g0.pause
    val play: String get() = g0.play
    val previous: String get() = g0.previous
    val next: String get() = g0.next
    val repeatOff: String get() = g0.repeatOff
    val repeatAll: String get() = g0.repeatAll
    val repeatOne: String get() = g0.repeatOne
    val like: String get() = g0.like
    val unlike: String get() = g0.unlike
    val download: String get() = g0.download
    val downloaded: String get() = g0.downloaded
    val downloadError: String get() = g0.downloadError
    val moreTitle: String get() = g0.moreTitle
    val accountSection: String get() = g0.accountSection
    val guest: String get() = g0.guest
    val loggedIn: String get() = g0.loggedIn
    val needHeaders: String get() = g0.needHeaders
    val sessionActive: String get() = g0.sessionActive
    val loginHelp: String get() = g0.loginHelp
    val loginWithHeaders: String get() = g1.loginWithHeaders
    val saveAndLogin: String get() = g1.saveAndLogin
    val cancel: String get() = g1.cancel
    val logout: String get() = g1.logout
    val refreshLikes: String get() = g1.refreshLikes
    val likedSection: String get() = g1.likedSection
    val likedEmpty: String get() = g1.likedEmpty
    val likedNeedLogin: String get() = g1.likedNeedLogin
    val playlistsSection: String get() = g1.playlistsSection
    val playlistsEmpty: String get() = g1.playlistsEmpty
    val refreshPlaylists: String get() = g1.refreshPlaylists
    val soundCloudLoginTitle: String get() = g1.soundCloudLoginTitle
    val soundCloudLoginHint: String get() = g1.soundCloudLoginHint
    val soundCloudNoToken: String get() = g1.soundCloudNoToken
    val soundCloudLoginWithToken: String get() = g1.soundCloudLoginWithToken
    val soundCloudTokenPlaceholder: String get() = g1.soundCloudTokenPlaceholder
    val soundCloudPlaylistsEmpty: String get() = g1.soundCloudPlaylistsEmpty
    val soundCloudPlaylistsCount: String get() = g1.soundCloudPlaylistsCount
    val soundCloudPlaylistsSection: String get() = g1.soundCloudPlaylistsSection
    val playlistTracksEmpty: String get() = g1.playlistTracksEmpty
    val offlineSection: String get() = g1.offlineSection
    val offlineTracksTitle: String get() = g1.offlineTracksTitle
    val offlineAlbumsTitle: String get() = g1.offlineAlbumsTitle
    val appearanceSubtitle: String get() = g1.appearanceSubtitle
    val offlineAlbumsSection: String get() = g1.offlineAlbumsSection
    val offlineAlbumsEmpty: String get() = g1.offlineAlbumsEmpty
    val offlineTracksSection: String get() = g1.offlineTracksSection
    val offlineTracksEmpty: String get() = g1.offlineTracksEmpty
    val tracksCount: String get() = g1.tracksCount
    val deleteAlbum: String get() = g1.deleteAlbum
    val aboutSection: String get() = g1.aboutSection
    val appName: String get() = g1.appName
    val versionLabel: String get() = g1.versionLabel
    val licenseLabel: String get() = g1.licenseLabel
    val languageSection: String get() = g1.languageSection
    val uiSettingsSection: String get() = g1.uiSettingsSection
    val coverStyleLabel: String get() = g1.coverStyleLabel
    val coverSquare: String get() = g1.coverSquare
    val coverRounded: String get() = g1.coverRounded
    val coverCircle: String get() = g1.coverCircle
    val seekBarStyleLabel: String get() = g1.seekBarStyleLabel
    val seekDefault: String get() = g1.seekDefault
    val seekThin: String get() = g1.seekThin
    val seekWave: String get() = g1.seekWave
    val seekSquiggle: String get() = g1.seekSquiggle
    val accentColorLabel: String get() = g1.accentColorLabel
    val accentFromCoverLabel: String get() = g1.accentFromCoverLabel
    val showAllPopular: String get() = g1.showAllPopular
    val showLessPopular: String get() = g1.showLessPopular
    val albumsSection: String get() = g1.albumsSection
    val singlesSection: String get() = g1.singlesSection
    val recentlyPlayed: String get() = g1.recentlyPlayed
    val addToPlaylist: String get() = g1.addToPlaylist
    val addedToPlaylist: String get() = g1.addedToPlaylist
    val addToPlaylistFailed: String get() = g1.addToPlaylistFailed
    val lyrics: String get() = g1.lyrics
    val lyricsNotFound: String get() = g1.lyricsNotFound
    val backgroundStyleLabel: String get() = g1.backgroundStyleLabel
    val bgBlurArt: String get() = g1.bgBlurArt
    val bgSolid: String get() = g1.bgSolid
    val bgGradient: String get() = g1.bgGradient
    val bgFullCover: String get() = g1.bgFullCover
    val languageEnglish: String get() = g1.languageEnglish
    val languageRussian: String get() = g1.languageRussian
    val headersPlaceholder: String get() = g1.headersPlaceholder
    val cachedSectionTitle: String get() = g1.cachedSectionTitle
    val cachedTracksTitle: String get() = g1.cachedTracksTitle
    val cachedTracksSubtitle: String get() = g1.cachedTracksSubtitle
    val cachedTracksEmpty: String get() = g1.cachedTracksEmpty
    val cachedTracksEmptyHint: String get() = g1.cachedTracksEmptyHint
    val topsSectionTitle: String get() = g1.topsSectionTitle
    val topTracksTitle: String get() = g1.topTracksTitle
    val topTracksSubtitle: String get() = g1.topTracksSubtitle
    val topTracksEmpty: String get() = g1.topTracksEmpty
    val topArtistsTitle: String get() = g1.topArtistsTitle
    val topArtistsSubtitle: String get() = g1.topArtistsSubtitle
    val topArtistsEmpty: String get() = g1.topArtistsEmpty
    val playsCountLabel: String get() = g1.playsCountLabel
    val qualitySection: String get() = g1.qualitySection
    val qualityLabel: String get() = g2.qualityLabel
    val qualityHint: String get() = g2.qualityHint
    val qualityHigh: String get() = g2.qualityHigh
    val qualityMedium: String get() = g2.qualityMedium
    val qualityLow: String get() = g2.qualityLow
    val qualityHighDesc: String get() = g2.qualityHighDesc
    val qualityMediumDesc: String get() = g2.qualityMediumDesc
    val qualityLowDesc: String get() = g2.qualityLowDesc
    val updatesSection: String get() = g2.updatesSection
    val updatesSubtitle: String get() = g2.updatesSubtitle
    val checkForUpdates: String get() = g2.checkForUpdates
    val checkingForUpdates: String get() = g2.checkingForUpdates
    val upToDate: String get() = g2.upToDate
    val updateAvailable: String get() = g2.updateAvailable
    val releaseNotesLabel: String get() = g2.releaseNotesLabel
    val downloadAndInstall: String get() = g2.downloadAndInstall
    val downloading: String get() = g2.downloading
    val installing: String get() = g2.installing
    val updateError: String get() = g2.updateError
    val skipVersion: String get() = g2.skipVersion
    val allowInstallPermission: String get() = g2.allowInstallPermission
    val allowInstallPermissionHint: String get() = g2.allowInstallPermissionHint
    val eqTitle: String get() = g2.eqTitle
    val eqOff: String get() = g2.eqOff
    val eqEnable: String get() = g2.eqEnable
    val eqProfileLabel: String get() = g2.eqProfileLabel
    val eqBandsLabel: String get() = g2.eqBandsLabel
    val eqUnavailable: String get() = g2.eqUnavailable
    val eqFlat: String get() = g2.eqFlat
    val eqBassBoost: String get() = g2.eqBassBoost
    val eqTrebleBoost: String get() = g2.eqTrebleBoost
    val eqVocal: String get() = g2.eqVocal
    val eqRock: String get() = g2.eqRock
    val eqPop: String get() = g2.eqPop
    val eqJazz: String get() = g2.eqJazz
    val eqClassical: String get() = g2.eqClassical
    val eqElectronic: String get() = g2.eqElectronic
    val eqHipHop: String get() = g2.eqHipHop
    val eqAcoustic: String get() = g2.eqAcoustic
    val eqCustom: String get() = g2.eqCustom
    val iconSectionLabel: String get() = g2.iconSectionLabel
    val iconPresetHint: String get() = g2.iconPresetHint
    val iconChanged: String get() = g2.iconChanged
    val togetherTitle: String get() = g2.togetherTitle
    val togetherSubtitle: String get() = g2.togetherSubtitle
    val togetherInactive: String get() = g2.togetherInactive
    val togetherHowTo: String get() = g2.togetherHowTo
    val togetherPermHint: String get() = g2.togetherPermHint
    val togetherHost: String get() = g2.togetherHost
    val togetherJoin: String get() = g2.togetherJoin
    val togetherScanPrompt: String get() = g2.togetherScanPrompt
    val togetherStarting: String get() = g2.togetherStarting
    val togetherQrHint: String get() = g2.togetherQrHint
    val togetherWaiting: String get() = g2.togetherWaiting
    val togetherConnectedList: String get() = g2.togetherConnectedList
    val togetherStop: String get() = g2.togetherStop
    val togetherLeave: String get() = g2.togetherLeave
    val togetherConnecting: String get() = g2.togetherConnecting
    val togetherJoinedTo: String get() = g2.togetherJoinedTo
    val togetherHosting: String get() = g2.togetherHosting
    val togetherGuestHint: String get() = g2.togetherGuestHint
    val togetherErrNoNetwork: String get() = g2.togetherErrNoNetwork
    val togetherErrBadCode: String get() = g2.togetherErrBadCode
    val togetherErrConnect: String get() = g2.togetherErrConnect
    val togetherErrRejected: String get() = g2.togetherErrRejected
    val togetherErrDisconnected: String get() = g2.togetherErrDisconnected
    val onbWelcomeTitle: String get() = g2.onbWelcomeTitle
    val onbWelcomeSubtitle: String get() = g2.onbWelcomeSubtitle
    val onbLanguageLabel: String get() = g2.onbLanguageLabel
    val onbNext: String get() = g2.onbNext
    val onbBack: String get() = g2.onbBack
    val onbSkip: String get() = g2.onbSkip
    val onbStart: String get() = g2.onbStart
    val onbAppearanceTitle: String get() = g2.onbAppearanceTitle
    val onbAppearanceHint: String get() = g2.onbAppearanceHint
    val onbQualityTitle: String get() = g2.onbQualityTitle
    val onbQualityHint: String get() = g2.onbQualityHint
    val onbReadyTitle: String get() = g2.onbReadyTitle
    val onbReadySubtitle: String get() = g2.onbReadySubtitle
    val onbFeatureOfflineTitle: String get() = g2.onbFeatureOfflineTitle
    val onbFeatureOfflineDesc: String get() = g3.onbFeatureOfflineDesc
    val onbFeatureTogetherTitle: String get() = g3.onbFeatureTogetherTitle
    val onbFeatureTogetherDesc: String get() = g3.onbFeatureTogetherDesc
    val onbFeatureEqTitle: String get() = g3.onbFeatureEqTitle
    val onbFeatureEqDesc: String get() = g3.onbFeatureEqDesc
    val onbFeatureLyricsTitle: String get() = g3.onbFeatureLyricsTitle
    val onbFeatureLyricsDesc: String get() = g3.onbFeatureLyricsDesc
    val onbReadyHint: String get() = g3.onbReadyHint
    val onbPermTitle: String get() = g3.onbPermTitle
    val onbPermSubtitle: String get() = g3.onbPermSubtitle
    val onbPermNotifTitle: String get() = g3.onbPermNotifTitle
    val onbPermNotifDesc: String get() = g3.onbPermNotifDesc
    val onbPermNearbyTitle: String get() = g3.onbPermNearbyTitle
    val onbPermNearbyDesc: String get() = g3.onbPermNearbyDesc
    val onbPermNearbyLegacyHint: String get() = g3.onbPermNearbyLegacyHint
    val onbPermCameraTitle: String get() = g3.onbPermCameraTitle
    val onbPermCameraDesc: String get() = g3.onbPermCameraDesc
    val onbPermInstallTitle: String get() = g3.onbPermInstallTitle
    val onbPermInstallDesc: String get() = g3.onbPermInstallDesc
    val onbPermAllow: String get() = g3.onbPermAllow
    val onbPermAllowAll: String get() = g3.onbPermAllowAll
    val onbPermGranted: String get() = g3.onbPermGranted
    val onbPermOpenSettings: String get() = g3.onbPermOpenSettings
    val onbPermFooter: String get() = g3.onbPermFooter
    val moreSectionMusic: String get() = g3.moreSectionMusic
    val moreSectionMusicSubtitle: String get() = g3.moreSectionMusicSubtitle
    val moreSectionSettings: String get() = g3.moreSectionSettings
    val moreSectionSettingsSubtitle: String get() = g3.moreSectionSettingsSubtitle
    val cacheTitle: String get() = g3.cacheTitle
    val cacheSubtitle: String get() = g3.cacheSubtitle
    val cacheSummary: String get() = g3.cacheSummary
    val cacheExportTitle: String get() = g3.cacheExportTitle
    val cacheExportDesc: String get() = g3.cacheExportDesc
    val cacheExportAction: String get() = g3.cacheExportAction
    val cacheImportTitle: String get() = g3.cacheImportTitle
    val cacheImportDesc: String get() = g3.cacheImportDesc
    val cacheImportAction: String get() = g3.cacheImportAction
    val cacheEmpty: String get() = g3.cacheEmpty
    val cacheExporting: String get() = g3.cacheExporting
    val cacheImporting: String get() = g3.cacheImporting
    val cacheExportDone: String get() = g3.cacheExportDone
    val cacheImportDone: String get() = g3.cacheImportDone
    val cacheError: String get() = g3.cacheError
    val cacheInvalidFile: String get() = g3.cacheInvalidFile
    val cacheCancelled: String get() = g3.cacheCancelled
    val cacheOk: String get() = g3.cacheOk
    val cacheNote: String get() = g3.cacheNote
    val playerModeTrack: String get() = g3.playerModeTrack
    val playerModeClip: String get() = g3.playerModeClip
}

internal data class AppStringsG0(
    val tabSearch: String,
    val searchHistoryTitle: String,
    val searchHistoryClear: String,
    val searchHistoryRemove: String,
    val offlineDialogTitle: String,
    val offlineDialogText: String,
    val offlineDialogEnable: String,
    val offlineDialogStay: String,
    val offlineBannerOn: String,
    val offlineBannerOnline: String,
    val offlineGoOnline: String,
    val offlineModeSwitch: String,
    val cachedSearchPlaceholder: String,
    val offlineSearchHint: String,
    val playNext: String,
    val playNextAdded: String,
    val openQueue: String,
    val queueTitle: String,
    val queueNowPlaying: String,
    val queueEmpty: String,
    val queueRemove: String,
    val queueReorder: String,
    val tabHome: String,
    val homeTitle: String,
    val homeRecommended: String,
    val homeRecommendedHint: String,
    val homeRecent: String,
    val homeTopTracks: String,
    val homeEmpty: String,
    val homeError: String,
    val homeRetry: String,
    val homeRefresh: String,
    val tabPlayer: String,
    val tabLiked: String,
    val tabMore: String,
    val searchPlaceholder: String,
    val artists: String,
    val tracks: String,
    val nothingFound: String,
    val sourceYtm: String,
    val sourceSoundCloud: String,
    val searchPlaceholderSoundCloud: String,
    val errorPrefix: String,
    val album: String,
    val artist: String,
    val albumsCount: String,
    val singlesCount: String,
    val popularTracks: String,
    val noAlbums: String,
    val offlineOrBackendError: String,
    val loginWithBrowser: String,
    val browserLoginTitle: String,
    val browserLoginHint: String,
    val browserBlockedHint: String,
    val noTracksInAlbum: String,
    val downloadAlbum: String,
    val downloadingAlbum: String,
    val albumDownloaded: String,
    val back: String,
    val playerEmptyTitle: String,
    val playerEmptyHint: String,
    val pause: String,
    val play: String,
    val previous: String,
    val next: String,
    val repeatOff: String,
    val repeatAll: String,
    val repeatOne: String,
    val like: String,
    val unlike: String,
    val download: String,
    val downloaded: String,
    val downloadError: String,
    val moreTitle: String,
    val accountSection: String,
    val guest: String,
    val loggedIn: String,
    val needHeaders: String,
    val sessionActive: String,
    val loginHelp: String
)

internal data class AppStringsG1(
    val loginWithHeaders: String,
    val saveAndLogin: String,
    val cancel: String,
    val logout: String,
    val refreshLikes: String,
    val likedSection: String,
    val likedEmpty: String,
    val likedNeedLogin: String,
    val playlistsSection: String,
    val playlistsEmpty: String,
    val refreshPlaylists: String,
    val soundCloudLoginTitle: String,
    val soundCloudLoginHint: String,
    val soundCloudNoToken: String,
    val soundCloudLoginWithToken: String,
    val soundCloudTokenPlaceholder: String,
    val soundCloudPlaylistsEmpty: String,
    val soundCloudPlaylistsCount: String,
    val soundCloudPlaylistsSection: String,
    val playlistTracksEmpty: String,
    val offlineSection: String,
    val offlineTracksTitle: String,
    val offlineAlbumsTitle: String,
    val appearanceSubtitle: String,
    val offlineAlbumsSection: String,
    val offlineAlbumsEmpty: String,
    val offlineTracksSection: String,
    val offlineTracksEmpty: String,
    val tracksCount: String,
    val deleteAlbum: String,
    val aboutSection: String,
    val appName: String,
    val versionLabel: String,
    val licenseLabel: String,
    val languageSection: String,
    val uiSettingsSection: String,
    val coverStyleLabel: String,
    val coverSquare: String,
    val coverRounded: String,
    val coverCircle: String,
    val seekBarStyleLabel: String,
    val seekDefault: String,
    val seekThin: String,
    val seekWave: String,
    val seekSquiggle: String,
    val accentColorLabel: String,
    val accentFromCoverLabel: String,
    val showAllPopular: String,
    val showLessPopular: String,
    val albumsSection: String,
    val singlesSection: String,
    val recentlyPlayed: String,
    val addToPlaylist: String,
    val addedToPlaylist: String,
    val addToPlaylistFailed: String,
    val lyrics: String,
    val lyricsNotFound: String,
    val backgroundStyleLabel: String,
    val bgBlurArt: String,
    val bgSolid: String,
    val bgGradient: String,
    val bgFullCover: String,
    val languageEnglish: String,
    val languageRussian: String,
    val headersPlaceholder: String,
    val cachedSectionTitle: String,
    val cachedTracksTitle: String,
    val cachedTracksSubtitle: String,
    val cachedTracksEmpty: String,
    val cachedTracksEmptyHint: String,
    val topsSectionTitle: String,
    val topTracksTitle: String,
    val topTracksSubtitle: String,
    val topTracksEmpty: String,
    val topArtistsTitle: String,
    val topArtistsSubtitle: String,
    val topArtistsEmpty: String,
    val playsCountLabel: String,
    val qualitySection: String
)

internal data class AppStringsG2(
    val qualityLabel: String,
    val qualityHint: String,
    val qualityHigh: String,
    val qualityMedium: String,
    val qualityLow: String,
    val qualityHighDesc: String,
    val qualityMediumDesc: String,
    val qualityLowDesc: String,
    val updatesSection: String,
    val updatesSubtitle: String,
    val checkForUpdates: String,
    val checkingForUpdates: String,
    val upToDate: String,
    val updateAvailable: String,
    val releaseNotesLabel: String,
    val downloadAndInstall: String,
    val downloading: String,
    val installing: String,
    val updateError: String,
    val skipVersion: String,
    val allowInstallPermission: String,
    val allowInstallPermissionHint: String,
    val eqTitle: String,
    val eqOff: String,
    val eqEnable: String,
    val eqProfileLabel: String,
    val eqBandsLabel: String,
    val eqUnavailable: String,
    val eqFlat: String,
    val eqBassBoost: String,
    val eqTrebleBoost: String,
    val eqVocal: String,
    val eqRock: String,
    val eqPop: String,
    val eqJazz: String,
    val eqClassical: String,
    val eqElectronic: String,
    val eqHipHop: String,
    val eqAcoustic: String,
    val eqCustom: String,
    val iconSectionLabel: String,
    val iconPresetHint: String,
    val iconChanged: String,
    val togetherTitle: String,
    val togetherSubtitle: String,
    val togetherInactive: String,
    val togetherHowTo: String,
    val togetherPermHint: String,
    val togetherHost: String,
    val togetherJoin: String,
    val togetherScanPrompt: String,
    val togetherStarting: String,
    val togetherQrHint: String,
    val togetherWaiting: String,
    val togetherConnectedList: String,
    val togetherStop: String,
    val togetherLeave: String,
    val togetherConnecting: String,
    val togetherJoinedTo: String,
    val togetherHosting: String,
    val togetherGuestHint: String,
    val togetherErrNoNetwork: String,
    val togetherErrBadCode: String,
    val togetherErrConnect: String,
    val togetherErrRejected: String,
    val togetherErrDisconnected: String,
    val onbWelcomeTitle: String,
    val onbWelcomeSubtitle: String,
    val onbLanguageLabel: String,
    val onbNext: String,
    val onbBack: String,
    val onbSkip: String,
    val onbStart: String,
    val onbAppearanceTitle: String,
    val onbAppearanceHint: String,
    val onbQualityTitle: String,
    val onbQualityHint: String,
    val onbReadyTitle: String,
    val onbReadySubtitle: String,
    val onbFeatureOfflineTitle: String
)

internal data class AppStringsG3(
    val onbFeatureOfflineDesc: String,
    val onbFeatureTogetherTitle: String,
    val onbFeatureTogetherDesc: String,
    val onbFeatureEqTitle: String,
    val onbFeatureEqDesc: String,
    val onbFeatureLyricsTitle: String,
    val onbFeatureLyricsDesc: String,
    val onbReadyHint: String,
    val onbPermTitle: String,
    val onbPermSubtitle: String,
    val onbPermNotifTitle: String,
    val onbPermNotifDesc: String,
    val onbPermNearbyTitle: String,
    val onbPermNearbyDesc: String,
    val onbPermNearbyLegacyHint: String,
    val onbPermCameraTitle: String,
    val onbPermCameraDesc: String,
    val onbPermInstallTitle: String,
    val onbPermInstallDesc: String,
    val onbPermAllow: String,
    val onbPermAllowAll: String,
    val onbPermGranted: String,
    val onbPermOpenSettings: String,
    val onbPermFooter: String,
    val moreSectionMusic: String,
    val moreSectionMusicSubtitle: String,
    val moreSectionSettings: String,
    val moreSectionSettingsSubtitle: String,
    val cacheTitle: String,
    val cacheSubtitle: String,
    val cacheSummary: String,
    val cacheExportTitle: String,
    val cacheExportDesc: String,
    val cacheExportAction: String,
    val cacheImportTitle: String,
    val cacheImportDesc: String,
    val cacheImportAction: String,
    val cacheEmpty: String,
    val cacheExporting: String,
    val cacheImporting: String,
    val cacheExportDone: String,
    val cacheImportDone: String,
    val cacheError: String,
    val cacheInvalidFile: String,
    val cacheCancelled: String,
    val cacheOk: String,
    val cacheNote: String,
    val playerModeTrack: String,
    val playerModeClip: String,
)

val EnglishStrings = AppStrings(
    g0 = AppStringsG0(
        tabSearch = "Search",
        searchHistoryTitle = "Search history",
        searchHistoryClear = "Clear",
        searchHistoryRemove = "Remove from history",
        offlineDialogTitle = "No internet connection",
        offlineDialogText = "Switch to offline mode? Search will only look through your downloaded tracks.",
        offlineDialogEnable = "Offline mode",
        offlineDialogStay = "Not now",
        offlineBannerOn = "Offline mode: downloaded tracks only",
        offlineBannerOnline = "You're back online",
        offlineGoOnline = "Go online",
        offlineModeSwitch = "Offline mode",
        cachedSearchPlaceholder = "Search downloaded tracks…",
        offlineSearchHint = "Offline mode: searching downloaded tracks only",
        playNext = "Play next",
        playNextAdded = "Will play next",
        openQueue = "Queue",
        queueTitle = "Queue",
        queueNowPlaying = "Now playing",
        queueEmpty = "The queue is empty",
        queueRemove = "Remove from queue",
        queueReorder = "Drag to reorder",
        tabHome = "Home",
        homeTitle = "Home",
        homeRecommended = "Recommended for you",
        homeRecommendedHint = "Based on what you listen to",
        homeRecent = "Recently played",
        homeTopTracks = "Your top tracks",
        homeEmpty = "No recommendations yet. Play a few tracks and check back.",
        homeError = "Couldn't load recommendations",
        homeRetry = "Try again",
        homeRefresh = "Refresh",
        tabPlayer = "Player",
        tabLiked = "Liked",
        tabMore = "More",
        searchPlaceholder = "Find artist or song…",
        artists = "Artists",
        tracks = "Tracks",
        nothingFound = "Nothing found",
        sourceYtm = "YouTube Music",
        sourceSoundCloud = "SoundCloud",
        searchPlaceholderSoundCloud = "Find a track on SoundCloud…",
        errorPrefix = "Error: ",
        album = "Album",
        artist = "Artist",
        albumsCount = "Albums (%d)",
        singlesCount = "Singles & EPs (%d)",
        popularTracks = "Popular",
        noAlbums = "This artist has no albums",
        offlineOrBackendError = "No network or backend is not ready. Check connection and try again.",
        loginWithBrowser = "Sign in via browser",
        browserLoginTitle = "YouTube Music login",
        browserLoginHint = "Sign in with Google. After redirect to YouTube Music, the session is saved automatically.",
        browserBlockedHint = "Google blocked sign-in in the embedded browser. Use “Sign in with headers” below: open music.youtube.com in Chrome → F12 → Network → copy Request Headers (Cookie is required).",
        noTracksInAlbum = "No tracks in this album",
        downloadAlbum = "Download album",
        downloadingAlbum = "Downloading album…",
        albumDownloaded = "Album downloaded — remove",
        back = "Back",
        playerEmptyTitle = "Nothing playing",
        playerEmptyHint = "Pick a track on Search or Home",
        pause = "Pause",
        play = "Play",
        previous = "Previous",
        next = "Next",
        repeatOff = "Repeat off",
        repeatAll = "Repeat queue",
        repeatOne = "Repeat track",
        like = "Like",
        unlike = "Remove like",
        download = "Download for offline",
        downloaded = "Downloaded — tap to remove",
        downloadError = "Download failed — tap to retry",
        moreTitle = "More",
        accountSection = "Accounts",
        guest = "Guest",
        loggedIn = "Signed in",
        needHeaders = "Browser headers required",
        sessionActive = "Session active",
        loginHelp = "Sign in with Google in the built-in browser, or paste Request Headers manually as a fallback."
    ),
    g1 = AppStringsG1(
        loginWithHeaders = "Sign in with headers",
        saveAndLogin = "Save and sign in",
        cancel = "Cancel",
        logout = "Sign out",
        refreshLikes = "Refresh likes",
        likedSection = "Liked (%d)",
        likedEmpty = "Empty — tap “Refresh likes”",
        likedNeedLogin = "Sign in on the Account page to see liked songs.",
        playlistsSection = "Playlists (%d)",
        playlistsEmpty = "No playlists. Create one in YouTube Music, then refresh.",
        refreshPlaylists = "Refresh playlists",
        soundCloudLoginTitle = "SoundCloud login",
        soundCloudLoginHint = "Sign in with your e-mail and password; the session is saved automatically. Google / Apple / Facebook buttons usually don't work in an embedded browser — use “Sign in with token” instead.",
        soundCloudNoToken = "No SoundCloud session yet — sign in first",
        soundCloudLoginWithToken = "Sign in with token",
        soundCloudTokenPlaceholder = "oauth_token value (soundcloud.com cookie), or a Cookie header containing it",
        soundCloudPlaylistsEmpty = "No playlists. Create or like one in SoundCloud, then refresh.",
        soundCloudPlaylistsCount = "Playlists (%d)",
        soundCloudPlaylistsSection = "Playlists",
        playlistTracksEmpty = "Playlist is empty",
        offlineSection = "Downloads",
        offlineTracksTitle = "Downloaded tracks",
        offlineAlbumsTitle = "Downloaded albums",
        appearanceSubtitle = "Covers, seek bar, colors, background",
        offlineAlbumsSection = "Downloaded albums (%d)",
        offlineAlbumsEmpty = "No downloaded albums. Use the download icon in the top-right of an album page.",
        offlineTracksSection = "Offline tracks (%d)",
        offlineTracksEmpty = "No downloaded tracks. Long-press a track to download.",
        tracksCount = "%d tracks",
        deleteAlbum = "Delete album",
        aboutSection = "About",
        appName = "GammaTunes",
        versionLabel = "Version %s",
        licenseLabel = "License: Apache License 2.0",
        languageSection = "Language",
        uiSettingsSection = "Appearance",
        coverStyleLabel = "Cover style",
        coverSquare = "Square",
        coverRounded = "Rounded",
        coverCircle = "Circle",
        seekBarStyleLabel = "Seek bar",
        seekDefault = "Default",
        seekThin = "Slim",
        seekWave = "Waveform",
        seekSquiggle = "Wavy",
        accentColorLabel = "Accent color",
        accentFromCoverLabel = "Accent from cover art",
        showAllPopular = "Show all",
        showLessPopular = "Show less",
        albumsSection = "Albums",
        singlesSection = "Singles & EPs",
        recentlyPlayed = "Recently played",
        addToPlaylist = "Add to playlist",
        addedToPlaylist = "Added to playlist",
        addToPlaylistFailed = "Could not add to playlist",
        lyrics = "Lyrics",
        lyricsNotFound = "No lyrics found",
        backgroundStyleLabel = "Player background",
        bgBlurArt = "Blurred art",
        bgSolid = "Solid dark",
        bgGradient = "Accent gradient",
        bgFullCover = "Full cover",
        languageEnglish = "English",
        languageRussian = "Русский",
        headersPlaceholder = "Cookie: …\nAuthorization: …",
        cachedSectionTitle = "Cached",
        cachedTracksTitle = "Cached tracks",
        cachedTracksSubtitle = "Cached (%d) — available offline",
        cachedTracksEmpty = "No cached tracks yet",
        cachedTracksEmptyHint = "Tracks you download are cached here and stay playable offline.",
        topsSectionTitle = "Tops",
        topTracksTitle = "Top 15",
        topTracksSubtitle = "Your most played tracks",
        topTracksEmpty = "Play some tracks to see your top 15 here.",
        topArtistsTitle = "Top 3 artists",
        topArtistsSubtitle = "Your most played artists",
        topArtistsEmpty = "Play some tracks to see your top artists here.",
        playsCountLabel = "%d plays",
        qualitySection = "Audio quality"
    ),
    g2 = AppStringsG2(
        qualityLabel = "Streaming quality",
        qualityHint = "Choose how much data audio playback uses. Downloaded tracks always use the best available quality.",
        qualityHigh = "High",
        qualityMedium = "Normal",
        qualityLow = "Data saver",
        qualityHighDesc = "Best available audio quality. Uses the most mobile data.",
        qualityMediumDesc = "Balanced quality, capped around 128 kbps.",
        qualityLowDesc = "Lower bitrate (around 64 kbps or less) to save mobile data.",
        updatesSection = "Updates",
        updatesSubtitle = "Check GitHub for a newer version",
        checkForUpdates = "Check for updates",
        checkingForUpdates = "Checking for updates…",
        upToDate = "You're on the latest version.",
        updateAvailable = "Update available: %s",
        releaseNotesLabel = "What's new",
        downloadAndInstall = "Download & install",
        downloading = "Downloading… %d%%",
        installing = "Installing…",
        updateError = "Update failed: %s",
        skipVersion = "Skip this version",
        allowInstallPermission = "Allow installing updates",
        allowInstallPermissionHint = "To install the update, allow GammaTunes to install unknown apps in the next screen, then come back here.",
        eqTitle = "Equalizer",
        eqOff = "Off",
        eqEnable = "Enable equalizer",
        eqProfileLabel = "Sound profile",
        eqBandsLabel = "Frequency bands",
        eqUnavailable = "The equalizer becomes available once playback is ready (or it isn't supported on this device).",
        eqFlat = "Flat",
        eqBassBoost = "Bass boost",
        eqTrebleBoost = "Treble boost",
        eqVocal = "Vocal",
        eqRock = "Rock",
        eqPop = "Pop",
        eqJazz = "Jazz",
        eqClassical = "Classical",
        eqElectronic = "Electronic",
        eqHipHop = "Hip-Hop",
        eqAcoustic = "Acoustic",
        eqCustom = "Custom",
        iconSectionLabel = "App icon",
        iconPresetHint = "This changes the real app icon in your launcher. The launcher may take a few seconds to refresh it, and some launchers remove the old icon from the home screen — if so, drag the app out of the app drawer again.",
        iconChanged = "Icon changed. The launcher may need a moment to update.",
        togetherTitle = "Listen together",
        togetherSubtitle = "Sync music with a friend via QR",
        togetherInactive = "Not active",
        togetherHowTo = "Show your QR to a friend — they scan it and listen along. Only the host controls playback.",
        togetherPermHint = "",
        togetherHost = "Show my QR",
        togetherJoin = "Scan a friend's QR",
        togetherScanPrompt = "Point the camera at your friend's QR code",
        togetherStarting = "Starting…",
        togetherQrHint = "Ask your friend to open Listen together → Scan a friend's QR (works from any network)",
        togetherWaiting = "Waiting for friends to join…",
        togetherConnectedList = "Connected: %s",
        togetherStop = "Stop session",
        togetherLeave = "Leave session",
        togetherConnecting = "Connecting…",
        togetherJoinedTo = "Listening together with %s",
        togetherHosting = "Hosting a session",
        togetherGuestHint = "Only the host can change tracks, pause or seek. Your controls are locked while you listen together.",
        togetherErrNoNetwork = "Couldn't start. Check your internet connection and try again.",
        togetherErrBadCode = "This isn't a GammaTunes QR code.",
        togetherErrConnect = "Couldn't connect. Check your internet and that the host's session is still open.",
        togetherErrRejected = "The session was closed or the code is no longer valid.",
        togetherErrDisconnected = "The connection was lost.",
        onbWelcomeTitle = "Welcome to GammaTunes",
        onbWelcomeSubtitle = "Your music, your way. Let's set up a few things before you start.",
        onbLanguageLabel = "Language",
        onbNext = "Next",
        onbBack = "Back",
        onbSkip = "Skip",
        onbStart = "Start listening",
        onbAppearanceTitle = "Make it yours",
        onbAppearanceHint = "Choose how the player looks. You can change this any time in More → Appearance.",
        onbQualityTitle = "Sound quality",
        onbQualityHint = "Higher quality uses more mobile data. You can change this any time in More.",
        onbReadyTitle = "Discover GammaTunes",
        onbReadySubtitle = "A few things worth trying:",
        onbFeatureOfflineTitle = "Offline mode"
    ),
    g3 = AppStringsG3(
        onbFeatureOfflineDesc = "Download tracks and albums and listen without internet.",
        onbFeatureTogetherTitle = "Listen together",
        onbFeatureTogetherDesc = "Sync playback with a friend by scanning a QR code.",
        onbFeatureEqTitle = "Equalizer",
        onbFeatureEqDesc = "Shape the sound with presets or your own bands.",
        onbFeatureLyricsTitle = "Synced lyrics",
        onbFeatureLyricsDesc = "Follow the words in real time while a track plays.",
        onbReadyHint = "Everything here lives in the More tab.",
        onbPermTitle = "Permissions",
        onbPermSubtitle = "GammaTunes only asks for what its features need. You can skip any of them and allow them later.",
        onbPermNotifTitle = "Notifications",
        onbPermNotifDesc = "Playback controls in the notification shade and on the lock screen.",
        onbPermNearbyTitle = "Nearby devices",
        onbPermNearbyDesc = "Connect directly to a friend's phone in Listen together, with no shared Wi-Fi.",
        onbPermNearbyLegacyHint = "On this Android version the system calls it \"Location\" — it is only used to find nearby devices.",
        onbPermCameraTitle = "Camera",
        onbPermCameraDesc = "Scan a friend's QR code to join a Listen together session.",
        onbPermInstallTitle = "Install updates",
        onbPermInstallDesc = "Lets GammaTunes install new versions that you download inside the app.",
        onbPermAllow = "Allow",
        onbPermAllowAll = "Allow all",
        onbPermGranted = "Allowed",
        onbPermOpenSettings = "Open settings",
        onbPermFooter = "You can change these any time in your phone's system settings.",
        moreSectionMusic = "Music",
        moreSectionMusicSubtitle = "Cached tracks and albums, tops",
        moreSectionSettings = "Settings",
        moreSectionSettingsSubtitle = "Accounts, appearance, sound, equalizer, listen together",
        cacheTitle = "Cache",
        cacheSubtitle = "Export and import cached tracks",
        cacheSummary = "Tracks: %d · Albums: %d · %s",
        cacheExportTitle = "Export cache",
        cacheExportDesc = "Save all cached tracks and albums into one file — to move them to another phone or keep a backup.",
        cacheExportAction = "Export",
        cacheImportTitle = "Import cache",
        cacheImportDesc = "Add tracks and albums from a GammaTunes cache file. Tracks you already have are skipped.",
        cacheImportAction = "Import",
        cacheEmpty = "Nothing to export yet — cache some tracks first.",
        cacheExporting = "Exporting… %d / %d",
        cacheImporting = "Importing… %d / %d",
        cacheExportDone = "Exported tracks: %d.",
        cacheImportDone = "Imported tracks: %d, skipped: %d.",
        cacheError = "Something went wrong: %s",
        cacheInvalidFile = "This isn't a GammaTunes cache file.",
        cacheCancelled = "Cancelled.",
        cacheOk = "OK",
        cacheNote = "The file contains audio and track info. Cover images are loaded from the internet. Keep the app open until it finishes.",
        playerModeTrack = "Track",
        playerModeClip = "Clip",
    )
)

val RussianStrings = AppStrings(
    g0 = AppStringsG0(
        tabSearch = "Поиск",
        searchHistoryTitle = "История поиска",
        searchHistoryClear = "Очистить",
        searchHistoryRemove = "Убрать из истории",
        offlineDialogTitle = "Нет подключения к интернету",
        offlineDialogText = "Включить оффлайн-режим? Поиск будет идти только по скачанным трекам.",
        offlineDialogEnable = "Оффлайн-режим",
        offlineDialogStay = "Не сейчас",
        offlineBannerOn = "Оффлайн-режим: только скачанные треки",
        offlineBannerOnline = "Интернет снова доступен",
        offlineGoOnline = "Выйти в сеть",
        offlineModeSwitch = "Оффлайн-режим",
        cachedSearchPlaceholder = "Поиск по скачанным трекам…",
        offlineSearchHint = "Оффлайн-режим: поиск только по скачанным трекам",
        playNext = "Проиграть следующим",
        playNextAdded = "Будет играть следующим",
        openQueue = "Очередь",
        queueTitle = "Очередь",
        queueNowPlaying = "Сейчас играет",
        queueEmpty = "Очередь пуста",
        queueRemove = "Убрать из очереди",
        queueReorder = "Перетащите, чтобы изменить порядок",
        tabHome = "Главная",
        homeTitle = "Главная",
        homeRecommended = "Рекомендации для вас",
        homeRecommendedHint = "На основе того, что вы слушаете",
        homeRecent = "Недавно слушали",
        homeTopTracks = "Ваши топ-треки",
        homeEmpty = "Пока нет рекомендаций. Послушайте несколько треков и загляните снова.",
        homeError = "Не удалось загрузить рекомендации",
        homeRetry = "Повторить",
        homeRefresh = "Обновить",
        tabPlayer = "Плеер",
        tabLiked = "Лайки",
        tabMore = "Прочее",
        searchPlaceholder = "Найти артиста или песню…",
        artists = "Артисты",
        tracks = "Треки",
        nothingFound = "Ничего не найдено",
        sourceYtm = "YouTube Music",
        sourceSoundCloud = "SoundCloud",
        searchPlaceholderSoundCloud = "Найти трек в SoundCloud…",
        errorPrefix = "Ошибка: ",
        album = "Альбом",
        artist = "Артист",
        albumsCount = "Альбомы (%d)",
        singlesCount = "Синглы и EP (%d)",
        popularTracks = "Популярные",
        noAlbums = "У артиста нет альбомов",
        offlineOrBackendError = "Нет сети или бэкенд не готов. Проверьте соединение и попробуйте снова.",
        loginWithBrowser = "Войти через браузер",
        browserLoginTitle = "Вход в YouTube Music",
        browserLoginHint = "Войдите через Google. После перехода на YouTube Music сессия сохранится сама.",
        browserBlockedHint = "Google заблокировал вход во встроенном браузере. Используйте «Войти через headers»: откройте music.youtube.com в Chrome → F12 → Network → скопируйте Request Headers (нужен Cookie).",
        noTracksInAlbum = "В альбоме нет треков",
        downloadAlbum = "Скачать альбом",
        downloadingAlbum = "Скачивание альбома…",
        albumDownloaded = "Альбом скачан — удалить",
        back = "Назад",
        playerEmptyTitle = "Ничего не играет",
        playerEmptyHint = "Выберите трек в Поиске",
        pause = "Пауза",
        play = "Играть",
        previous = "Предыдущий",
        next = "Следующий",
        repeatOff = "Повтор выключен",
        repeatAll = "Повтор плейлиста",
        repeatOne = "Повтор трека",
        like = "Лайк",
        unlike = "Убрать лайк",
        download = "Скачать для оффлайн-прослушивания",
        downloaded = "Скачано — нажмите, чтобы удалить",
        downloadError = "Ошибка скачивания — нажмите, чтобы повторить",
        moreTitle = "Прочее",
        accountSection = "Аккаунты",
        guest = "Гость",
        loggedIn = "Вы вошли",
        needHeaders = "Нужны headers из браузера",
        sessionActive = "Сессия активна",
        loginHelp = "Войдите через Google во встроенном браузере. Если не получится — вставьте Request Headers вручную."
    ),
    g1 = AppStringsG1(
        loginWithHeaders = "Войти через headers",
        saveAndLogin = "Сохранить и войти",
        cancel = "Отмена",
        logout = "Выйти",
        refreshLikes = "Обновить лайки",
        likedSection = "Лайкнутые (%d)",
        likedEmpty = "Пока пусто — нажмите «Обновить лайки»",
        likedNeedLogin = "Войдите в аккаунт, чтобы видеть лайки.",
        playlistsSection = "Плейлисты (%d)",
        playlistsEmpty = "Нет плейлистов. Создайте в YouTube Music и обновите.",
        refreshPlaylists = "Обновить плейлисты",
        soundCloudLoginTitle = "Вход в SoundCloud",
        soundCloudLoginHint = "Войдите по e-mail и паролю — сессия сохранится сама. Кнопки Google / Apple / Facebook во встроенном браузере обычно не работают — используйте «Войти по токену».",
        soundCloudNoToken = "Сессии SoundCloud пока нет — сначала войдите",
        soundCloudLoginWithToken = "Войти по токену",
        soundCloudTokenPlaceholder = "Значение cookie oauth_token с soundcloud.com или заголовок Cookie с ним",
        soundCloudPlaylistsEmpty = "Нет плейлистов. Создайте или лайкните плейлист в SoundCloud и обновите.",
        soundCloudPlaylistsCount = "Плейлисты (%d)",
        soundCloudPlaylistsSection = "Плейлисты",
        playlistTracksEmpty = "Плейлист пуст",
        offlineSection = "Скачанное",
        offlineTracksTitle = "Скачанные треки",
        offlineAlbumsTitle = "Скачанные альбомы",
        appearanceSubtitle = "Обложки, seek bar, цвета, фон",
        offlineAlbumsSection = "Скачанные альбомы (%d)",
        offlineAlbumsEmpty = "Нет скачанных альбомов. На странице альбома нажмите иконку загрузки справа сверху.",
        offlineTracksSection = "Оффлайн-треки (%d)",
        offlineTracksEmpty = "Нет скачанных треков. Удерживайте трек, чтобы скачать.",
        tracksCount = "%d треков",
        deleteAlbum = "Удалить альбом",
        aboutSection = "О приложении",
        appName = "GammaTunes",
        versionLabel = "Версия %s",
        licenseLabel = "Лицензия: Apache License 2.0",
        languageSection = "Язык",
        uiSettingsSection = "Оформление",
        coverStyleLabel = "Обложка",
        coverSquare = "Квадрат",
        coverRounded = "Скруглённая",
        coverCircle = "Круг",
        seekBarStyleLabel = "Полоса перемотки",
        seekDefault = "Обычная",
        seekThin = "Тонкая",
        seekWave = "Аудиоволна",
        seekSquiggle = "Волнистая",
        accentColorLabel = "Акцентный цвет",
        accentFromCoverLabel = "Акцент с обложки трека",
        showAllPopular = "Показать все",
        showLessPopular = "Свернуть",
        albumsSection = "Альбомы",
        singlesSection = "Синглы и EP",
        recentlyPlayed = "Недавно слушали",
        addToPlaylist = "Добавить в плейлист",
        addedToPlaylist = "Добавлено в плейлист",
        addToPlaylistFailed = "Не удалось добавить в плейлист",
        lyrics = "Текст",
        lyricsNotFound = "Текст не найден",
        backgroundStyleLabel = "Фон плеера",
        bgBlurArt = "Размытая обложка",
        bgSolid = "Тёмный",
        bgGradient = "Градиент акцента",
        bgFullCover = "Обложка на весь экран",
        languageEnglish = "English",
        languageRussian = "Русский",
        headersPlaceholder = "Cookie: …\nAuthorization: …",
        cachedSectionTitle = "Кешированное",
        cachedTracksTitle = "Кешированные треки",
        cachedTracksSubtitle = "Кешировано (%d) — доступно оффлайн",
        cachedTracksEmpty = "Пока нет кешированных треков",
        cachedTracksEmptyHint = "Скачанные треки кешируются здесь и остаются доступны без интернета.",
        topsSectionTitle = "Топы",
        topTracksTitle = "Топ-15",
        topTracksSubtitle = "Ваши самые прослушиваемые треки",
        topTracksEmpty = "Послушайте треки, чтобы увидеть здесь свой топ-15.",
        topArtistsTitle = "Топ-3 артиста",
        topArtistsSubtitle = "Ваши самые прослушиваемые артисты",
        topArtistsEmpty = "Послушайте треки, чтобы увидеть здесь своих топ-артистов.",
        playsCountLabel = "%d прослушиваний",
        qualitySection = "Качество звука"
    ),
    g2 = AppStringsG2(
        qualityLabel = "Качество потока",
        qualityHint = "Выберите, сколько трафика тратить на воспроизведение. Скачанные треки всегда сохраняются в лучшем доступном качестве.",
        qualityHigh = "Высокое",
        qualityMedium = "Обычное",
        qualityLow = "Экономия трафика",
        qualityHighDesc = "Лучшее доступное качество звука. Расходует больше всего трафика.",
        qualityMediumDesc = "Сбалансированное качество, ограничено примерно 128 кбит/с.",
        qualityLowDesc = "Пониженный битрейт (около 64 кбит/с и ниже) для экономии мобильного трафика.",
        updatesSection = "Обновления",
        updatesSubtitle = "Проверить GitHub на новую версию",
        checkForUpdates = "Проверить обновления",
        checkingForUpdates = "Проверка обновлений…",
        upToDate = "У вас установлена последняя версия.",
        updateAvailable = "Доступно обновление: %s",
        releaseNotesLabel = "Что нового",
        downloadAndInstall = "Скачать и установить",
        downloading = "Загрузка… %d%%",
        installing = "Установка…",
        updateError = "Не удалось обновить: %s",
        skipVersion = "Пропустить эту версию",
        allowInstallPermission = "Разрешить установку обновлений",
        allowInstallPermissionHint = "Чтобы установить обновление, разрешите GammaTunes устанавливать неизвестные приложения на следующем экране, затем вернитесь сюда.",
        eqTitle = "Эквалайзер",
        eqOff = "Выключен",
        eqEnable = "Включить эквалайзер",
        eqProfileLabel = "Профиль звука",
        eqBandsLabel = "Полосы частот",
        eqUnavailable = "Эквалайзер станет доступен, когда плеер будет готов (или он не поддерживается на этом устройстве).",
        eqFlat = "Плоский",
        eqBassBoost = "Усиление басов",
        eqTrebleBoost = "Усиление высоких",
        eqVocal = "Вокал",
        eqRock = "Рок",
        eqPop = "Поп",
        eqJazz = "Джаз",
        eqClassical = "Классика",
        eqElectronic = "Электроника",
        eqHipHop = "Хип-хоп",
        eqAcoustic = "Акустика",
        eqCustom = "Своя",
        iconSectionLabel = "Иконка приложения",
        iconPresetHint = "Меняется настоящая иконка приложения в лаунчере. Лаунчеру может понадобиться несколько секунд, чтобы обновить её; некоторые лаунчеры убирают старую иконку с рабочего стола — тогда просто вытащите приложение из списка приложений заново.",
        iconChanged = "Иконка изменена. Лаунчеру может понадобиться немного времени на обновление.",
        togetherTitle = "Слушать вместе",
        togetherSubtitle = "Музыка на двоих по QR-коду",
        togetherInactive = "Не активно",
        togetherHowTo = "Покажите свой QR другу — он отсканирует его и будет слушать вместе с вами. Управляет только хозяин.",
        togetherPermHint = "",
        togetherHost = "Показать мой QR",
        togetherJoin = "Сканировать QR друга",
        togetherScanPrompt = "Наведите камеру на QR-код друга",
        togetherStarting = "Запуск…",
        togetherQrHint = "Попросите друга открыть «Слушать вместе» → «Сканировать QR друга» (работает из любой сети)",
        togetherWaiting = "Ждём, пока друзья подключатся…",
        togetherConnectedList = "Подключены: %s",
        togetherStop = "Завершить сессию",
        togetherLeave = "Выйти из сессии",
        togetherConnecting = "Подключение…",
        togetherJoinedTo = "Слушаете вместе с %s",
        togetherHosting = "Вы хозяин сессии",
        togetherGuestHint = "Только хозяин может менять треки, ставить паузу и перематывать. Ваше управление заблокировано, пока вы слушаете вместе.",
        togetherErrNoNetwork = "Не удалось запустить. Проверьте интернет и попробуйте снова.",
        togetherErrBadCode = "Это не QR-код GammaTunes.",
        togetherErrConnect = "Не удалось подключиться. Проверьте интернет и что сессия хозяина ещё открыта.",
        togetherErrRejected = "Сессия закрыта или код больше не действует.",
        togetherErrDisconnected = "Соединение потеряно.",
        onbWelcomeTitle = "Добро пожаловать в GammaTunes",
        onbWelcomeSubtitle = "Ваша музыка — по-вашему. Настроим пару вещей перед началом.",
        onbLanguageLabel = "Язык",
        onbNext = "Далее",
        onbBack = "Назад",
        onbSkip = "Пропустить",
        onbStart = "Начать слушать",
        onbAppearanceTitle = "Сделайте под себя",
        onbAppearanceHint = "Выберите внешний вид плеера. Это можно изменить в любой момент: Прочее → Оформление.",
        onbQualityTitle = "Качество звука",
        onbQualityHint = "Чем выше качество, тем больше мобильного трафика. Это можно изменить в любой момент в разделе «Прочее».",
        onbReadyTitle = "Знакомьтесь с GammaTunes",
        onbReadySubtitle = "Несколько вещей, которые стоит попробовать:",
        onbFeatureOfflineTitle = "Офлайн-режим"
    ),
    g3 = AppStringsG3(
        onbFeatureOfflineDesc = "Скачивайте треки и альбомы и слушайте без интернета.",
        onbFeatureTogetherTitle = "Слушать вместе",
        onbFeatureTogetherDesc = "Синхронизируйте воспроизведение с другом по QR-коду.",
        onbFeatureEqTitle = "Эквалайзер",
        onbFeatureEqDesc = "Настройте звук с помощью пресетов или своих полос.",
        onbFeatureLyricsTitle = "Синхронный текст",
        onbFeatureLyricsDesc = "Следите за словами в реальном времени, пока играет трек.",
        onbReadyHint = "Всё это находится на вкладке «Прочее».",
        onbPermTitle = "Разрешения",
        onbPermSubtitle = "GammaTunes просит только то, что нужно для работы функций. Любое можно пропустить и разрешить позже.",
        onbPermNotifTitle = "Уведомления",
        onbPermNotifDesc = "Управление воспроизведением в шторке и на экране блокировки.",
        onbPermNearbyTitle = "Устройства поблизости",
        onbPermNearbyDesc = "Прямое подключение к телефону друга в «Слушать вместе» без общей Wi-Fi сети.",
        onbPermNearbyLegacyHint = "На этой версии Android система называет его «Геолокация» — оно используется только для поиска устройств рядом.",
        onbPermCameraTitle = "Камера",
        onbPermCameraDesc = "Сканирование QR-кода друга, чтобы подключиться к сессии «Слушать вместе».",
        onbPermInstallTitle = "Установка обновлений",
        onbPermInstallDesc = "Позволяет GammaTunes устанавливать новые версии, скачанные внутри приложения.",
        onbPermAllow = "Разрешить",
        onbPermAllowAll = "Разрешить всё",
        onbPermGranted = "Разрешено",
        onbPermOpenSettings = "Открыть настройки",
        onbPermFooter = "Всё это можно изменить в любой момент в системных настройках телефона.",
        moreSectionMusic = "Музыка",
        moreSectionMusicSubtitle = "Кешированные треки и альбомы, топы",
        moreSectionSettings = "Настройки",
        moreSectionSettingsSubtitle = "Аккаунты, оформление, звук, эквалайзер, слушать вместе",
        cacheTitle = "Кеш",
        cacheSubtitle = "Экспорт и импорт кешированных треков",
        cacheSummary = "Треков: %d · Альбомов: %d · %s",
        cacheExportTitle = "Экспорт кеша",
        cacheExportDesc = "Сохраняет все кешированные треки и альбомы в один файл — для переноса на другой телефон или резервной копии.",
        cacheExportAction = "Экспортировать",
        cacheImportTitle = "Импорт кеша",
        cacheImportDesc = "Добавляет треки и альбомы из файла кеша GammaTunes. Уже имеющиеся треки пропускаются.",
        cacheImportAction = "Импортировать",
        cacheEmpty = "Экспортировать пока нечего — сначала скачайте треки.",
        cacheExporting = "Экспорт… %d / %d",
        cacheImporting = "Импорт… %d / %d",
        cacheExportDone = "Экспортировано треков: %d.",
        cacheImportDone = "Импортировано треков: %d, пропущено: %d.",
        cacheError = "Что-то пошло не так: %s",
        cacheInvalidFile = "Это не файл кеша GammaTunes.",
        cacheCancelled = "Отменено.",
        cacheOk = "ОК",
        cacheNote = "В файле — аудио и данные треков. Обложки подгружаются из интернета. Не закрывайте приложение, пока операция не завершится.",
        playerModeTrack = "Трек",
        playerModeClip = "Клип",
    )
)

fun stringsFor(lang: AppLanguage): AppStrings = when (lang) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.RUSSIAN -> RussianStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
val LocalLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

package com.gammatunes.app

import android.app.Application
import com.gammatunes.app.auth.AuthRepository
import com.gammatunes.app.auth.SoundCloudAuthRepository
import com.gammatunes.app.backend.LocalBackend
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.ui.i18n.LocaleRepository
import com.gammatunes.app.player.PlayHistoryRepository
import com.gammatunes.app.player.PlayStatsRepository
import com.gammatunes.app.player.PlaybackSettingsRepository
import com.gammatunes.app.ui.settings.UiSettingsRepository
import com.gammatunes.app.update.AppUpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GammaTunesApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LocalBackend.start(this)
        OfflineRepository.init(this)
        com.gammatunes.app.search.SearchHistoryRepository.init(this)
        com.gammatunes.app.network.NetworkMonitor.init(this)
        PlayHistoryRepository.init(this)
        PlayStatsRepository.init(this)
        com.gammatunes.app.ui.onboarding.OnboardingRepository.init(this)
        LocaleRepository.init(this)
        UiSettingsRepository.init(this)
        PlaybackSettingsRepository.init(this)
        com.gammatunes.app.player.EqualizerManager.init(this)
        AuthRepository.init(this)
        SoundCloudAuthRepository.init(this)
        com.gammatunes.app.together.TogetherSession.init(this)

        appScope.launch {
            if (LocalBackend.awaitReady(timeoutMs = 45_000)) {
                AuthRepository.restoreSessionIfNeeded()
                SoundCloudAuthRepository.restoreSessionIfNeeded()
            }
        }

        // Silent, throttled check for a newer GitHub release. Only surfaces in the UI
        // (More -> Updates); never interrupts the user on its own.
        AppUpdateRepository.autoCheck(this)
    }
}

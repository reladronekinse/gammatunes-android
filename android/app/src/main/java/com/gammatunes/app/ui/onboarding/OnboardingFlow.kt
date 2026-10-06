package com.gammatunes.app.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Весь онбординг целиком: сначала экран настройки (приветствие, оформление, качество, функции),
 * затем отдельный экран разрешений. [onFinish] вызывается, когда пользователь закончил или пропустил.
 * Состояние переживает поворот экрана.
 */
@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    var stage by rememberSaveable { mutableIntStateOf(STAGE_SETUP) }
    // Страница, на которой остановились, — чтобы «Назад» с экрана разрешений вернул на последнюю страницу
    var setupPage by rememberSaveable { mutableIntStateOf(0) }

    Crossfade(targetState = stage, label = "onboardingStage") { current ->
        when (current) {
            STAGE_SETUP -> OnboardingScreen(
                initialPage = setupPage,
                onContinue = {
                    setupPage = ONBOARDING_PAGE_COUNT - 1
                    stage = STAGE_PERMISSIONS
                },
                onSkip = onFinish,
            )
            else -> PermissionsScreen(
                onBack = { stage = STAGE_SETUP },
                onFinish = onFinish,
            )
        }
    }
}

private const val STAGE_SETUP = 0
private const val STAGE_PERMISSIONS = 1

package com.gammatunes.app.ui.onboarding

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Хранит флаг «первичная настройка пройдена». Экран онбординга показывается,
 * только пока [completed] == false, то есть один раз после первой установки.
 *
 * Пользователи, которые обновились с более старой версии (у них уже есть сохранённые
 * настройки), онбординг не видят: для них флаг сразу выставляется в true.
 */
object OnboardingRepository {
    private const val PREFS = "ytm_onboarding"
    private const val KEY_COMPLETED = "completed"
    private const val KEY_LEGACY_CHECKED = "legacy_checked"

    // Файлы настроек, которые создаются только после того, как пользователь что-то менял.
    private val LEGACY_PREFS = listOf("ytm_ui_settings", "ytm_locale", "ytm_playback_settings")

    // По умолчанию true, чтобы до init() случайно не мигнул онбординг.
    private val _completed = MutableStateFlow(true)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun init(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (!prefs.contains(KEY_LEGACY_CHECKED)) {
            val prefsDir = File(app.applicationInfo.dataDir, "shared_prefs")
            val isExistingUser = LEGACY_PREFS.any { File(prefsDir, "$it.xml").exists() }
            val editor = prefs.edit()
            editor.putBoolean(KEY_LEGACY_CHECKED, true)
            if (isExistingUser) editor.putBoolean(KEY_COMPLETED, true)
            editor.apply()
        }

        _completed.value = prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun complete(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .apply()
        _completed.value = true
    }
}

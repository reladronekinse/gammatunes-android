package com.gammatunes.app.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Варианты иконки приложения в лаунчере. Android не позволяет перекрашивать иконку на лету,
 * но позволяет включать и выключать компоненты манифеста — поэтому каждый вариант объявлен
 * как отдельный `activity-alias` (см. AndroidManifest.xml), а мы переключаем, какой из них включён.
 *
 * Цвета здесь должны совпадать с res/values/ic_launcher_colors.xml и ic_launcher_foreground*.xml.
 */
enum class AppIconPreset(val alias: String, val background: Long, val foreground: Long) {
    RED("IconRed", 0xFFEA3323L, 0xFFFFFFFFL), // по умолчанию
    ORANGE("IconOrange", 0xFFFF8A00L, 0xFFFFFFFFL),
    AMBER("IconAmber", 0xFFFFC107L, 0xFF212121L),
    GREEN("IconGreen", 0xFF43A047L, 0xFFFFFFFFL),
    TEAL("IconTeal", 0xFF00897BL, 0xFFFFFFFFL),
    BLUE("IconBlue", 0xFF1E88E5L, 0xFFFFFFFFL),
    PURPLE("IconPurple", 0xFF8E24AAL, 0xFFFFFFFFL),
    PINK("IconPink", 0xFFD81B60L, 0xFFFFFFFFL),
    DARK("IconDark", 0xFF212121L, 0xFFFFFFFFL),
    WHITE("IconWhite", 0xFFFFFFFFL, 0xFFEA3323L),
}

object AppIconManager {
    private val default = AppIconPreset.RED

    private fun component(context: Context, preset: AppIconPreset) =
        ComponentName(context.packageName, "com.gammatunes.app.${preset.alias}")

    /** Какой алиас сейчас включён. DEFAULT-состояние означает значение из манифеста (включён только RED). */
    fun current(context: Context): AppIconPreset {
        val pm = context.packageManager
        return AppIconPreset.values().firstOrNull { preset ->
            when (pm.getComponentEnabledSetting(component(context, preset))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> preset == default
                else -> false
            }
        } ?: default
    }

    /** Включает выбранный алиас и выключает остальные. Сначала включаем новый — чтобы иконка не пропадала. */
    fun apply(context: Context, preset: AppIconPreset): Boolean {
        if (current(context) == preset) return true
        val pm = context.packageManager
        return runCatching {
            pm.setComponentEnabledSetting(
                component(context, preset),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            AppIconPreset.values().filter { it != preset }.forEach { other ->
                pm.setComponentEnabledSetting(
                    component(context, other),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }.isSuccess
    }
}

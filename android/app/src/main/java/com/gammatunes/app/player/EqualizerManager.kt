package com.gammatunes.app.player

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ln

/**
 * Профили звука. [curveDb] — усиление в дБ на пяти опорных частотах
 * (60 Гц, 230 Гц, 910 Гц, 3.6 кГц, 14 кГц); для реального числа полос устройства
 * кривая интерполируется по логарифмической шкале частоты.
 * У [CUSTOM] кривой нет — там пользователь сам выставляет полосы.
 */
enum class EqProfile(val id: String, val curveDb: FloatArray?) {
    FLAT("flat", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("bass_boost", floatArrayOf(6f, 4f, 1f, 0f, 0f)),
    TREBLE_BOOST("treble_boost", floatArrayOf(0f, 0f, 0f, 3f, 6f)),
    VOCAL("vocal", floatArrayOf(-3f, -1f, 2f, 4f, 1f)),
    ROCK("rock", floatArrayOf(5f, 3f, -1f, 3f, 5f)),
    POP("pop", floatArrayOf(-1f, 2f, 4f, 2f, -1f)),
    JAZZ("jazz", floatArrayOf(4f, 2f, -2f, 2f, 4f)),
    CLASSICAL("classical", floatArrayOf(4f, 3f, -2f, 2f, 3f)),
    ELECTRONIC("electronic", floatArrayOf(5f, 2f, 0f, 3f, 5f)),
    HIP_HOP("hip_hop", floatArrayOf(5f, 4f, 0f, 1f, 3f)),
    ACOUSTIC("acoustic", floatArrayOf(4f, 3f, 2f, 2f, 3f)),
    CUSTOM("custom", null),
    ;

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: FLAT
    }
}

data class EqState(
    /** true, когда аудио-эффект создан и привязан к сессии плеера. */
    val available: Boolean = false,
    val enabled: Boolean = false,
    val profile: EqProfile = EqProfile.FLAT,
    val centerFreqsHz: List<Int> = emptyList(),
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val bandLevelsMb: List<Int> = emptyList(),
)

/**
 * Эквалайзер поверх системного [Equalizer]. Привязывается к audio session ExoPlayer
 * из PlaybackService и переподключается, если ExoPlayer сменит сессию.
 * Все вызовы — из главного потока (как и у ExoPlayer).
 */
object EqualizerManager {
    private const val TAG = "EqualizerManager"
    private const val PREFS = "ytm_equalizer"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PROFILE = "profile"
    private const val KEY_CUSTOM = "custom_levels_mb"

    private val REF_FREQS_HZ = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)

    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var enabled = false
    private var profile = EqProfile.FLAT
    private var customLevelsMb: List<Int> = emptyList()

    private var equalizer: Equalizer? = null
    private var boundPlayer: ExoPlayer? = null
    private var sessionListener: Player.Listener? = null

    private var centerFreqsHz: List<Int> = emptyList()
    private var minLevelMb = -1500
    private var maxLevelMb = 1500

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        enabled = p.getBoolean(KEY_ENABLED, false)
        profile = EqProfile.fromId(p.getString(KEY_PROFILE, null))
        customLevelsMb = p.getString(KEY_CUSTOM, null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            .orEmpty()
        publish()
    }

    fun attach(player: ExoPlayer) {
        detach()
        boundPlayer = player
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                rebuild(audioSessionId)
            }
        }
        sessionListener = listener
        player.addListener(listener)
        rebuild(player.audioSessionId)
    }

    fun detach() {
        sessionListener?.let { boundPlayer?.removeListener(it) }
        sessionListener = null
        boundPlayer = null
        releaseEffect()
        publish()
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        persist()
        applyToEffect()
        publish()
    }

    /** Выбор профиля сразу включает эквалайзер. */
    fun setProfile(p: EqProfile) {
        profile = p
        enabled = true
        persist()
        applyToEffect()
        publish()
    }

    /** Ручная правка полосы переводит эквалайзер в режим «Своя» с текущих значений. */
    fun setBandLevel(band: Int, levelMb: Int) {
        val levels = currentLevels().toMutableList()
        if (band !in levels.indices) return
        levels[band] = levelMb.coerceIn(minLevelMb, maxLevelMb)
        customLevelsMb = levels
        profile = EqProfile.CUSTOM
        enabled = true
        persist()
        applyToEffect()
        publish()
    }

    private fun rebuild(sessionId: Int) {
        releaseEffect()
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) {
            publish()
            return
        }
        try {
            val eq = Equalizer(0, sessionId)
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            minLevelMb = range[0].toInt()
            maxLevelMb = range[1].toInt()
            centerFreqsHz = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 }
            equalizer = eq
            applyToEffect()
        } catch (t: Throwable) {
            Log.w(TAG, "Equalizer unavailable for session $sessionId", t)
            equalizer = null
            centerFreqsHz = emptyList()
        }
        publish()
    }

    private fun releaseEffect() {
        runCatching { equalizer?.release() }
        equalizer = null
    }

    private fun applyToEffect() {
        val eq = equalizer ?: return
        try {
            val levels = currentLevels()
            levels.forEachIndexed { i, mb -> eq.setBandLevel(i.toShort(), mb.toShort()) }
            eq.setEnabled(enabled)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to apply equalizer settings", t)
        }
    }

    private fun currentLevels(): List<Int> {
        val count = centerFreqsHz.size
        val raw = if (profile == EqProfile.CUSTOM) {
            if (customLevelsMb.size == count) customLevelsMb else List(count) { 0 }
        } else {
            val curve = profile.curveDb ?: FloatArray(REF_FREQS_HZ.size)
            centerFreqsHz.map { hz -> (interpolate(curve, hz.toFloat()) * 100f).toInt() }
        }
        return raw.map { it.coerceIn(minLevelMb, maxLevelMb) }
    }

    private fun interpolate(curve: FloatArray, freqHz: Float): Float {
        if (freqHz <= REF_FREQS_HZ.first()) return curve.first()
        if (freqHz >= REF_FREQS_HZ.last()) return curve.last()
        for (i in 0 until REF_FREQS_HZ.size - 1) {
            val f0 = REF_FREQS_HZ[i]
            val f1 = REF_FREQS_HZ[i + 1]
            if (freqHz <= f1) {
                val t = ln(freqHz / f0) / ln(f1 / f0)
                return curve[i] + (curve[i + 1] - curve[i]) * t
            }
        }
        return curve.last()
    }

    private fun persist() {
        prefs?.edit()
            ?.putBoolean(KEY_ENABLED, enabled)
            ?.putString(KEY_PROFILE, profile.id)
            ?.putString(KEY_CUSTOM, customLevelsMb.joinToString(","))
            ?.apply()
    }

    private fun publish() {
        val available = equalizer != null
        _state.value = EqState(
            available = available,
            enabled = enabled,
            profile = profile,
            centerFreqsHz = if (available) centerFreqsHz else emptyList(),
            minLevelMb = minLevelMb,
            maxLevelMb = maxLevelMb,
            bandLevelsMb = if (available) currentLevels() else emptyList(),
        )
    }
}

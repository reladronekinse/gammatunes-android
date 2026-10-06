@file:OptIn(UnstableApi::class)

package com.gammatunes.app.together

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import com.gammatunes.app.model.Track
import com.gammatunes.app.player.PlayerState
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

enum class TogetherError { NO_NETWORK, BAD_CODE, CONNECT_FAILED, REJECTED, DISCONNECTED }

sealed interface TogetherState {
    data object Idle : TogetherState
    data object Starting : TogetherState

    /** Хозяин: [payload] — QR/ссылка, [guests] — имена подключённых. */
    data class Hosting(val payload: String, val guests: List<String>) : TogetherState
    data object Connecting : TogetherState
    data class Joined(val hostName: String) : TogetherState
    data class Failed(val error: TogetherError) : TogetherState
}

/** Сообщение протокола. */
private data class Msg(
    val t: String? = null,
    val name: String? = null,
    val track: Track? = null,
    val queue: List<Track>? = null,
    val index: Int? = null,
    val pos: Long? = null,
    val playing: Boolean? = null,
    val id: String? = null,
    val token: String? = null,
    /** Стабильный id участника (MQTT clientId), чтобы вести список гостей. */
    val gid: String? = null,
)

/**
 * «Слушать вместе»: синхронизация команд (трек, очередь, пауза, перемотка)
 * по MQTT; звук не передаётся. Управляет только хозяин.
 * QR: gammatunes://together?r=ROOM&t=TOKEN
 */
object TogetherSession {
    private const val HEARTBEAT_MS = 4_000L
    private const val DRIFT_MS = 1_500L
    private const val SEEK_ECHO_WINDOW_MS = 2_000L
    private const val CONNECT_TIMEOUT_MS = 20_000L
    private const val GUEST_STALE_MS = 45_000L
    private const val GUEST_SWEEP_MS = 10_000L

    /** Публичный MQTT-брокер (без своего сервера приложения). */
    private const val MQTT_URI = "tcp://broker.hivemq.com:1883"
    private const val TOPIC_PREFIX = "gammatunes/together/v1"

    private val main = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val deviceName: String get() = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"

    private val _state = MutableStateFlow<TogetherState>(TogetherState.Idle)
    val state: StateFlow<TogetherState> = _state.asStateFlow()

    private var player: PlayerState? = null
    private var appContext: Context? = null

    @Volatile private var mqtt: MqttClient? = null
    @Volatile private var hosting = false
    private var roomId: String? = null
    private var hostToken: String? = null
    private var hostPayload: String? = null
    private var myGid: String? = null
    private var topicDown: String? = null
    private var topicUp: String? = null

    /** gid → (name, lastSeenMs) */
    private val guests = ConcurrentHashMap<String, Pair<String, Long>>()

    private var generation = 0
    private var joinTimeout: Runnable? = null
    private var connectTimeout: Runnable? = null

    private var syncedTrackId: String? = null
    private var syncedPlaying: Boolean? = null
    private var expectedSeekPos = 0L
    private var expectedSeekAt = 0L

    private val active: Boolean
        get() = mqtt?.isConnected == true && (hosting || _state.value is TogetherState.Joined || _state.value is TogetherState.Connecting)

    fun isGuest(): Boolean =
        !hosting && (
            _state.value is TogetherState.Joined ||
                _state.value is TogetherState.Connecting
            )

    fun isHosting(): Boolean = hosting

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun attach(playerState: PlayerState) {
        player = playerState
        playerState.syncListener = listener
    }

    fun detach(playerState: PlayerState) {
        if (player === playerState) {
            playerState.syncListener = null
            player = null
        }
    }

    // ---------------------------------------------------------------- хозяин

    fun startHosting() {
        leave()
        _state.value = TogetherState.Starting
        val gen = ++generation
        hosting = true
        val token = newId(12)
        val room = newId(16)
        hostToken = token
        roomId = room
        myGid = "h-" + newId(8)
        topicDown = "$TOPIC_PREFIX/$room/d"
        topicUp = "$TOPIC_PREFIX/$room/u"
        hostPayload = Uri.Builder()
            .scheme("gammatunes")
            .authority("together")
            .appendQueryParameter("r", room)
            .appendQueryParameter("t", token)
            .build()
            .toString()

        connectTimeout = Runnable {
            if (gen == generation && _state.value is TogetherState.Starting) {
                leave()
                _state.value = TogetherState.Failed(TogetherError.CONNECT_FAILED)
            }
        }
        main.postDelayed(connectTimeout!!, CONNECT_TIMEOUT_MS)

        Thread {
            try {
                val client = openMqtt(myGid!!, gen) ?: run {
                    main.post {
                        if (gen == generation) {
                            leave()
                            _state.value = TogetherState.Failed(TogetherError.CONNECT_FAILED)
                        }
                    }
                    return@Thread
                }
                client.subscribe(topicUp, 1)
                main.post {
                    if (gen != generation) {
                        runCatching { client.disconnect() }
                        return@post
                    }
                    connectTimeout?.let { main.removeCallbacks(it) }
                    connectTimeout = null
                    mqtt = client
                    publishHosting()
                    main.postDelayed(heartbeat, HEARTBEAT_MS)
                    main.postDelayed(guestSweep, GUEST_SWEEP_MS)
                    // Сразу шлём снимок, если уже что-то играет
                    val snap = snapshot("welcome")
                    publishDown(snap)
                }
            } catch (t: Throwable) {
                main.post {
                    if (gen == generation) {
                        leave()
                        _state.value = TogetherState.Failed(TogetherError.CONNECT_FAILED)
                    }
                }
            }
        }.start()
    }

    private fun publishHosting() {
        val payload = hostPayload ?: return
        val names = guests.values.map { it.first }.sorted()
        _state.value = TogetherState.Hosting(payload, names)
    }

    private val heartbeat = object : Runnable {
        override fun run() {
            val p = player
            val track = p?.currentTrack
            if (hosting && mqtt?.isConnected == true && p != null && track != null && !p.isLoadingStream) {
                publishDown(Msg("hb", id = track.videoId, pos = p.positionMs, playing = p.playWhenReady))
            }
            if (hosting) main.postDelayed(this, HEARTBEAT_MS)
        }
    }

    private val guestSweep = object : Runnable {
        override fun run() {
            if (!hosting) return
            val now = System.currentTimeMillis()
            var changed = false
            val it = guests.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                if (now - e.value.second > GUEST_STALE_MS) {
                    it.remove()
                    changed = true
                }
            }
            if (changed) publishHosting()
            if (hosting) main.postDelayed(this, GUEST_SWEEP_MS)
        }
    }

    /** Гость периодически пингует хозяина, чтобы остаться в списке. */
    private val guestPing = object : Runnable {
        override fun run() {
            if (hosting || _state.value !is TogetherState.Joined) return
            publishUp(
                Msg(t = "ping", name = deviceName, token = hostToken, gid = myGid),
            )
            main.postDelayed(this, HEARTBEAT_MS * 2)
        }
    }

    // ---------------------------------------------------------------- гость

    fun join(payload: String) {
        leave()
        val uri = runCatching { Uri.parse(payload.trim()) }.getOrNull()
        if (uri == null || uri.scheme != "gammatunes" || uri.authority != "together") {
            _state.value = TogetherState.Failed(TogetherError.BAD_CODE)
            return
        }
        val room = uri.getQueryParameter("r")?.takeIf { it.isNotBlank() }
        val token = uri.getQueryParameter("t")?.takeIf { it.isNotBlank() }
        if (room == null || token == null) {
            _state.value = TogetherState.Failed(TogetherError.BAD_CODE)
            return
        }

        val gen = ++generation
        hosting = false
        roomId = room
        hostToken = token
        myGid = "g-" + newId(8)
        topicDown = "$TOPIC_PREFIX/$room/d"
        topicUp = "$TOPIC_PREFIX/$room/u"
        _state.value = TogetherState.Connecting

        joinTimeout = Runnable {
            if (gen == generation && _state.value is TogetherState.Connecting) {
                leave()
                _state.value = TogetherState.Failed(TogetherError.CONNECT_FAILED)
            }
        }
        main.postDelayed(joinTimeout!!, CONNECT_TIMEOUT_MS)

        Thread {
            try {
                val client = openMqtt(myGid!!, gen) ?: run {
                    main.post {
                        if (gen == generation) {
                            leave()
                            _state.value = TogetherState.Failed(TogetherError.CONNECT_FAILED)
                        }
                    }
                    return@Thread
                }
                client.subscribe(topicDown, 1)
                main.post {
                    if (gen != generation) {
                        runCatching { client.disconnect() }
                        return@post
                    }
                    mqtt = client
                    // Представляемся хозяину
                    publishUp(
                        Msg(
                            t = "hello",
                            name = deviceName,
                            token = token,
                            gid = myGid,
                        ),
                    )
                }
            } catch (t: Throwable) {
                main.post {
                    if (gen == generation) {
                        leave()
                        _state.value = TogetherState.Failed(TogetherError.CONNECT_FAILED)
                    }
                }
            }
        }.start()
    }

    // ---------------------------------------------------------------- MQTT

    private fun openMqtt(clientId: String, gen: Int): MqttClient? {
        val client = MqttClient(MQTT_URI, clientId, MemoryPersistence())
        val opts = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 15
            keepAliveInterval = 20
            maxReconnectDelay = 10_000
        }
        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                main.post {
                    if (gen != generation) return@post
                    // После reconnect нужно снова подписаться
                    runCatching {
                        if (hosting) {
                            client.subscribe(topicUp, 1)
                        } else {
                            client.subscribe(topicDown, 1)
                            publishUp(
                                Msg(t = "hello", name = deviceName, token = hostToken, gid = myGid),
                            )
                        }
                    }
                }
            }

            override fun connectionLost(cause: Throwable?) {
                main.post {
                    if (gen != generation) return@post
                    // automaticReconnect сам поднимет; если сессия уже Joined/Hosting — ждём
                    // Полный fail только если leave() не вызывали и долго нет связи — heartbeat
                    // на стороне гостя просто перестанет приходить, UI останется Joined.
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val raw = message?.payload?.toString(Charsets.UTF_8) ?: return
                main.post {
                    if (gen != generation) return@post
                    onMqttMessage(raw)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
        client.connect(opts)
        return client
    }

    private fun onMqttMessage(raw: String) {
        val m = parse(raw) ?: return
        if (hosting) {
            onHostSideMessage(m)
        } else {
            onGuestSideMessage(m)
        }
    }

    private fun onHostSideMessage(m: Msg) {
        when (m.t) {
            "hello" -> {
                if (m.token != hostToken) return
                val gid = m.gid ?: return
                val name = m.name?.take(40)?.takeIf { it.isNotBlank() } ?: "Guest"
                guests[gid] = name to System.currentTimeMillis()
                publishHosting()
                // Личный «welcome» для всех (новый гость подтянет состояние)
                publishDown(snapshot("welcome"))
            }
            "bye" -> {
                val gid = m.gid ?: return
                if (guests.remove(gid) != null) publishHosting()
            }
            "ping" -> {
                // Гость подтверждает, что ещё на линии
                val gid = m.gid ?: return
                if (m.token != hostToken) return
                val prev = guests[gid]
                val name = m.name?.take(40)?.takeIf { it.isNotBlank() }
                    ?: prev?.first
                    ?: "Guest"
                guests[gid] = name to System.currentTimeMillis()
            }
            // Управление от гостей игнорируем
            else -> Unit
        }
    }

    private fun onGuestSideMessage(m: Msg) {
        when (m.t) {
            "welcome", "track", "play", "seek", "queue", "hb" -> {
                if (_state.value is TogetherState.Connecting) {
                    joinTimeout?.let { main.removeCallbacks(it) }
                    joinTimeout = null
                    val hostName = m.name?.takeIf { it.isNotBlank() } ?: "Host"
                    _state.value = TogetherState.Joined(hostName)
                    main.postDelayed(guestPing, HEARTBEAT_MS)
                }
                applyMessage(m)
            }
            "kick" -> {
                leave()
                _state.value = TogetherState.Failed(TogetherError.REJECTED)
            }
            else -> Unit
        }
    }

    private fun publishDown(m: Msg) {
        val topic = topicDown ?: return
        val client = mqtt ?: return
        if (!client.isConnected) return
        val raw = gson.toJson(m)
        runCatching {
            client.publish(topic, MqttMessage(raw.toByteArray(Charsets.UTF_8)).apply {
                qos = 1
                isRetained = false
            })
        }
    }

    private fun publishUp(m: Msg) {
        val topic = topicUp ?: return
        val client = mqtt ?: return
        if (!client.isConnected) return
        val raw = gson.toJson(m)
        runCatching {
            client.publish(topic, MqttMessage(raw.toByteArray(Charsets.UTF_8)).apply {
                qos = 1
                isRetained = false
            })
        }
    }

    // ---------------------------------------------------------------- leave

    fun leave() {
        val gen = generation
        generation++
        main.removeCallbacks(heartbeat)
        main.removeCallbacks(guestSweep)
        main.removeCallbacks(guestPing)
        joinTimeout?.let { main.removeCallbacks(it) }
        joinTimeout = null
        connectTimeout?.let { main.removeCallbacks(it) }
        connectTimeout = null

        // Гость сообщает об уходе
        if (!hosting && mqtt?.isConnected == true) {
            runCatching {
                publishUp(Msg(t = "bye", gid = myGid, token = hostToken))
            }
        }

        val c = mqtt
        mqtt = null
        Thread {
            runCatching {
                if (c?.isConnected == true) c.disconnect()
                c?.close()
            }
        }.start()

        hosting = false
        roomId = null
        hostToken = null
        hostPayload = null
        myGid = null
        topicDown = null
        topicUp = null
        guests.clear()
        syncedTrackId = null
        syncedPlaying = null
        expectedSeekAt = 0L
        if (_state.value !is TogetherState.Failed) {
            _state.value = TogetherState.Idle
        }
        // generation already bumped; gen unused except to silence stale callbacks
        @Suppress("UNUSED_VARIABLE")
        val _g = gen
    }

    // ---------------------------------------------------------------- команды плеера → сеть (только хозяин)

    private val listener = object : PlayerState.SyncListener {
        override fun onTrackStarted(track: Track, queue: List<Track>, index: Int) {
            if (!hosting || track.videoId == syncedTrackId) return
            syncedTrackId = track.videoId
            syncedPlaying = true
            val (q, i) = window(queue, index)
            publishDown(Msg("track", track = track, queue = q, index = i, pos = 0L, playing = true, name = deviceName))
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, positionMs: Long) {
            if (!hosting || playWhenReady == syncedPlaying) return
            syncedPlaying = playWhenReady
            publishDown(Msg("play", playing = playWhenReady, pos = positionMs, name = deviceName))
        }

        override fun onSeeked(positionMs: Long) {
            if (!hosting) return
            val now = System.currentTimeMillis()
            if (now - expectedSeekAt < SEEK_ECHO_WINDOW_MS && abs(positionMs - expectedSeekPos) < 1_000) {
                expectedSeekAt = 0L
                return
            }
            publishDown(Msg("seek", pos = positionMs))
        }

        override fun onQueueChanged(queue: List<Track>, index: Int) {
            if (!hosting) return
            val (q, i) = window(queue, index)
            publishDown(Msg("queue", queue = q, index = i, id = player?.currentTrack?.videoId))
        }
    }

    // ---------------------------------------------------------------- сеть → плеер

    private fun applyMessage(m: Msg) {
        val p = player ?: return
        when (m.t) {
            "track", "welcome" -> applyTrack(p, m)
            "play" -> {
                val playing = m.playing ?: return
                syncedPlaying = playing
                if (p.playWhenReady != playing) p.setRemotePlaying(playing)
                m.pos?.let { seekIfFar(p, it, 700L) }
            }
            "seek" -> m.pos?.let { seekIfFar(p, it, 0L) }
            "queue" -> {
                val q = m.queue ?: return
                p.applyRemoteQueue(q, m.id ?: p.currentTrack?.videoId)
            }
            "hb" -> {
                val cur = p.currentTrack ?: return
                if (m.id != cur.videoId || p.isLoadingStream) return
                val hostPlaying = m.playing == true
                if (p.playWhenReady != hostPlaying) {
                    syncedPlaying = hostPlaying
                    p.setRemotePlaying(hostPlaying)
                }
                val pos = m.pos ?: return
                if (hostPlaying) seekIfFar(p, pos, DRIFT_MS)
            }
        }
    }

    private fun applyTrack(p: PlayerState, m: Msg) {
        val track = m.track ?: return
        val queue = m.queue?.takeIf { it.isNotEmpty() } ?: listOf(track)
        val playing = m.playing ?: true
        syncedTrackId = track.videoId
        syncedPlaying = playing
        if (p.currentTrack?.videoId == track.videoId) {
            p.applyRemoteQueue(queue, track.videoId)
            if (p.playWhenReady != playing) p.setRemotePlaying(playing)
            m.pos?.let { seekIfFar(p, it, DRIFT_MS) }
        } else {
            p.applyRemoteTrack(track, queue, m.index ?: 0, m.pos ?: 0L, playing)
        }
    }

    private fun seekIfFar(p: PlayerState, pos: Long, thresholdMs: Long) {
        if (abs(p.positionMs - pos) <= thresholdMs) return
        expectedSeekPos = pos
        expectedSeekAt = System.currentTimeMillis()
        p.forceSeekTo(pos)
    }

    // ---------------------------------------------------------------- утилиты

    private fun snapshot(type: String): Msg {
        val p = player
        val track = p?.currentTrack ?: return Msg(type, name = deviceName)
        val (q, i) = window(p.queueTracks, p.queueIndex)
        return Msg(
            type,
            name = deviceName,
            track = track,
            queue = q,
            index = i,
            pos = p.positionMs,
            playing = p.playWhenReady,
        )
    }

    private fun window(queue: List<Track>, index: Int): Pair<List<Track>, Int> {
        if (queue.isEmpty()) return queue to 0
        val from = (index - 10).coerceAtLeast(0)
        val to = (index + 90).coerceAtMost(queue.size)
        return queue.subList(from, to) to (index - from).coerceAtLeast(0)
    }

    private fun parse(raw: String): Msg? =
        runCatching { gson.fromJson(raw, Msg::class.java) }.getOrNull()

    private fun newId(len: Int): String {
        val alphabet = "abcdefghijkmnpqrstuvwxyz23456789"
        val rnd = SecureRandom()
        return buildString { repeat(len) { append(alphabet[rnd.nextInt(alphabet.length)]) } }
    }
}

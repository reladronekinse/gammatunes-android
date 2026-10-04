package com.gammatunes.app.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gammatunes.app.auth.SoundCloudAuthRepository
import com.gammatunes.app.ui.i18n.LocalStrings
import kotlinx.coroutines.delay
import java.net.URLDecoder

private const val SC_WEB = "https://soundcloud.com"
private const val SC_SIGN_IN_URL = "https://soundcloud.com/signin"

// A normal mobile Chrome UA (no "wv" marker) so the site serves its regular mobile layout.
private const val MOBILE_CHROME_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/131.0.0.0 Mobile Safari/537.36"

/** The `oauth_token` cookie soundcloud.com sets for a signed-in web session, if any. */
internal fun readSoundCloudToken(): String? {
    val cookies = CookieManager.getInstance().getCookie(SC_WEB).orEmpty()
    val raw = cookies.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("oauth_token=") }
        ?.substringAfter("=")
        ?.trim()
        ?: return null
    val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw).trim('"')
    return decoded.takeIf { it.length >= 10 }
}

/** Forget the web session so the next sign-in starts clean (leaves other sites' cookies alone). */
fun clearSoundCloudWebSession() {
    val cm = CookieManager.getInstance()
    cm.getCookie(SC_WEB).orEmpty()
        .split(";")
        .map { it.substringBefore("=").trim() }
        .filter { it.isNotEmpty() }
        .forEach { name ->
            cm.setCookie(SC_WEB, "$name=; Max-Age=0; Path=/")
            cm.setCookie(SC_WEB, "$name=; Max-Age=0; Path=/; Domain=.soundcloud.com")
        }
    cm.flush()
}

/**
 * Sign-in to SoundCloud in an embedded browser. The session token is picked up
 * from the cookie jar as soon as it appears and handed to [onTokenCaptured].
 *
 * Google/Apple/Facebook buttons open pop-up windows that a plain WebView can't
 * show, so e-mail sign-in is the reliable path here (the hint says so); users
 * of social login can paste the token instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SoundCloudLoginDialog(
    onDismiss: () -> Unit,
    onTokenCaptured: (token: String) -> Unit,
) {
    val strings = LocalStrings.current
    val busy by SoundCloudAuthRepository.isBusy.collectAsState()
    val repoMessage by SoundCloudAuthRepository.statusMessage.collectAsState()

    var status by remember { mutableStateOf(strings.soundCloudLoginHint) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoading by remember { mutableStateOf(true) }
    var lastSubmitted by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // [force] = user pressed the button: retry even the token that already failed.
    fun submit(force: Boolean) {
        if (busy) return
        val token = readSoundCloudToken()
        if (token == null) {
            if (force) status = strings.soundCloudNoToken
            return
        }
        if (!force && token == lastSubmitted) return
        lastSubmitted = token
        status = strings.sessionActive
        onTokenCaptured(token)
    }

    // Show backend feedback (e.g. "token rejected") in the status line.
    LaunchedEffect(repoMessage) {
        if (!repoMessage.isNullOrBlank()) status = repoMessage.orEmpty()
    }

    // SoundCloud is a single-page app: cookies appear without a page load event, so poll.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            submit(force = false)
        }
    }

    Dialog(
        onDismissRequest = {
            submit(force = false)
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(strings.soundCloudLoginTitle, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = {
                            submit(force = false)
                            onDismiss()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                        }
                    },
                    actions = {
                        TextButton(onClick = { submit(force = true) }, enabled = !busy) {
                            Text(strings.saveAndLogin)
                        }
                    },
                )
                if (pageLoading || busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.userAgentString = MOBILE_CHROME_UA

                            val cm = CookieManager.getInstance()
                            cm.setAcceptCookie(true)
                            cm.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?,
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    pageLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    pageLoading = false
                                }
                            }
                            webViewRef = this
                            loadUrl(SC_SIGN_IN_URL)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    update = { webViewRef = it },
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }
}

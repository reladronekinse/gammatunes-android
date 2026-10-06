package com.gammatunes.app.ui.screens

import com.gammatunes.app.ui.components.dockPadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gammatunes.app.together.TogetherError
import com.gammatunes.app.together.TogetherSession
import com.gammatunes.app.together.TogetherState
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.components.generateQrBitmap
import com.gammatunes.app.ui.i18n.LocalStrings
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TogetherScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current
    val state by TogetherSession.state.collectAsState()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { TogetherSession.join(it) }
    }
    val startScan = {
        scanLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(strings.togetherScanPrompt)
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    strings.togetherTitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = dockPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.togetherHowTo, style = MaterialTheme.typography.bodyMedium)
                }
            }

            when (val s = state) {
                is TogetherState.Idle, is TogetherState.Failed -> {
                    if (s is TogetherState.Failed) {
                        Text(
                            text = errorText(s.error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = { TogetherSession.startHosting() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.togetherHost) }
                    OutlinedButton(
                        onClick = { startScan() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.togetherJoin) }
                }

                is TogetherState.Starting, is TogetherState.Connecting -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            if (s is TogetherState.Starting) strings.togetherStarting else strings.togetherConnecting,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { TogetherSession.leave() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.togetherLeave) }
                }

                is TogetherState.Hosting -> {
                    val qr = remember(s.payload) { generateQrBitmap(s.payload).asImageBitmap() }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Image(
                            bitmap = qr,
                            contentDescription = null,
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White),
                        )
                    }
                    Text(
                        strings.togetherQrHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (s.guests.isEmpty()) {
                            strings.togetherWaiting
                        } else {
                            strings.togetherConnectedList.format(s.guests.joinToString(", "))
                        },
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { TogetherSession.leave() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.togetherStop) }
                }

                is TogetherState.Joined -> {
                    Text(
                        strings.togetherJoinedTo.format(s.hostName),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        strings.togetherGuestHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { TogetherSession.leave() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.togetherLeave) }
                }
            }
        }
    }
}

@Composable
private fun errorText(error: TogetherError): String {
    val strings = LocalStrings.current
    return when (error) {
        TogetherError.NO_NETWORK -> strings.togetherErrNoNetwork
        TogetherError.BAD_CODE -> strings.togetherErrBadCode
        TogetherError.CONNECT_FAILED -> strings.togetherErrConnect
        TogetherError.REJECTED -> strings.togetherErrRejected
        TogetherError.DISCONNECTED -> strings.togetherErrDisconnected
    }
}

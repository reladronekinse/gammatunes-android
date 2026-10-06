package com.gammatunes.app.ui.screens

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gammatunes.app.offline.BackupKind
import com.gammatunes.app.offline.BackupState
import com.gammatunes.app.offline.OfflineBackup
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.components.dockPadding
import com.gammatunes.app.ui.i18n.AppStrings
import com.gammatunes.app.ui.i18n.LocalStrings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val index by OfflineRepository.index.collectAsState()
    val albums by OfflineRepository.albums.collectAsState()
    val backup by OfflineBackup.state.collectAsState()
    val busy = backup is BackupState.Working

    val totalBytes = remember(index) { index.values.sumOf { File(it.filePath).length() } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) OfflineBackup.export(context, uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) OfflineBackup.import(context, uri) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    strings.cacheTitle,
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
            Text(
                strings.cacheSummary.format(
                    index.size,
                    albums.size,
                    Formatter.formatShortFileSize(context, totalBytes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BackupStatusCard(backup)

            ActionCard(
                icon = Icons.Default.FileUpload,
                title = strings.cacheExportTitle,
                description = strings.cacheExportDesc,
                actionLabel = strings.cacheExportAction,
                enabled = !busy && index.isNotEmpty(),
                hint = if (index.isEmpty()) strings.cacheEmpty else null,
                onClick = { exportLauncher.launch(defaultExportName()) },
            )
            ActionCard(
                icon = Icons.Default.FileDownload,
                title = strings.cacheImportTitle,
                description = strings.cacheImportDesc,
                actionLabel = strings.cacheImportAction,
                enabled = !busy,
                hint = null,
                onClick = {
                    importLauncher.launch(
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"),
                    )
                },
            )

            Text(
                strings.cacheNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun defaultExportName(): String {
    val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    return "gammatunes-cache-$date.zip"
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    enabled: Boolean,
    hint: String?,
    onClick: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.padding(top = 6.dp),
                ) { Text(actionLabel, maxLines = 1) }
            }
        }
    }
}

/** Прогресс текущей операции или итог последней. Пока ничего не происходило — не показывается. */
@Composable
private fun BackupStatusCard(state: BackupState) {
    val strings = LocalStrings.current
    if (state is BackupState.Idle) return

    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(statusText(strings, state), style = MaterialTheme.typography.bodyMedium)

            if (state is BackupState.Working) {
                if (state.total > 0) {
                    LinearProgressIndicator(
                        progress = { state.done.toFloat() / state.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = { OfflineBackup.cancel() }) { Text(strings.cancel) }
            } else {
                TextButton(onClick = { OfflineBackup.dismiss() }) { Text(strings.cacheOk) }
            }
        }
    }
}

private fun statusText(strings: AppStrings, state: BackupState): String = when (state) {
    BackupState.Idle -> ""
    is BackupState.Working -> when (state.kind) {
        BackupKind.EXPORT -> strings.cacheExporting
        BackupKind.IMPORT -> strings.cacheImporting
    }.format(state.done, state.total)
    is BackupState.Finished -> when (state.kind) {
        BackupKind.EXPORT -> strings.cacheExportDone.format(state.tracks)
        BackupKind.IMPORT -> strings.cacheImportDone.format(state.tracks, state.skipped)
    }
    is BackupState.Failed ->
        if (state.invalidFile) strings.cacheInvalidFile
        else strings.cacheError.format(state.message ?: "—")
    BackupState.Cancelled -> strings.cacheCancelled
}

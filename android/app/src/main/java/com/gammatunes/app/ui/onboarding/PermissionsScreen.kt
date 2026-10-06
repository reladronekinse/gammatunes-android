package com.gammatunes.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.i18n.LocalStrings
import com.gammatunes.app.update.AppUpdateRepository

private enum class Perm(val icon: ImageVector) {
    NOTIFICATIONS(Icons.Default.Notifications),
    CAMERA(Icons.Default.CameraAlt),
    INSTALL(Icons.Default.SystemUpdate),
}

/** Системные (runtime) разрешения, которые нужно запросить для пункта. Пусто — это особое разрешение или не нужно. */
private fun runtimePermissions(perm: Perm): Array<String> = when (perm) {
    Perm.NOTIFICATIONS ->
        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    Perm.CAMERA -> arrayOf(Manifest.permission.CAMERA)
    Perm.INSTALL -> emptyArray()
}

private fun isGranted(context: Context, perm: Perm): Boolean = when (perm) {
    // «Установка из неизвестных источников» — особое разрешение, выдаётся в системных настройках
    Perm.INSTALL ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    else -> runtimePermissions(perm).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

/** До Android 13 уведомления не требуют разрешения, поэтому пункт не показываем. */
private fun isRelevant(perm: Perm): Boolean =
    !(perm == Perm.NOTIFICATIONS && Build.VERSION.SDK_INT < 33)

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

/**
 * Вторая часть онбординга — отдельный экран, на котором приложение просит все нужные разрешения.
 * Ни одно из них не обязательно: можно пропустить и выдать позже в системных настройках.
 */
@Composable
fun PermissionsScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val perms = remember { Perm.entries.filter(::isRelevant) }

    // Счётчик нужен, чтобы пересчитать статусы после ответа системы и при возврате из настроек.
    var refreshTick by remember { mutableIntStateOf(0) }
    val granted = remember(refreshTick) { perms.associateWith { isGranted(context, it) } }

    // Разрешения, в которых пользователь отказал: повторный диалог система может не показать,
    // поэтому для них кнопка превращается в «Открыть настройки».
    var denied by remember { mutableStateOf(emptySet<Perm>()) }
    var lastRequested by remember { mutableStateOf(emptyList<Perm>()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        denied = denied + lastRequested.filter { !isGranted(context, it) }
        refreshTick++
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun request(list: List<Perm>) {
        val toAsk = list.flatMap { runtimePermissions(it).toList() }.distinct().toTypedArray()
        if (toAsk.isEmpty()) return
        lastRequested = list
        launcher.launch(toAsk)
    }

    fun onAllow(perm: Perm) {
        when {
            perm == Perm.INSTALL -> AppUpdateRepository.openInstallPermissionSettings(context)
            perm in denied -> openAppSettings(context)
            else -> request(listOf(perm))
        }
    }

    // Что ещё можно запросить одним нажатием «Разрешить всё» (без особых разрешений и без отказанных)
    val askable = perms.filter {
        granted[it] != true && it !in denied && runtimePermissions(it).isNotEmpty()
    }
    val anythingMissing = perms.any { granted[it] != true }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (anythingMissing) {
                TextButton(onClick = onFinish) { Text(strings.onbSkip) }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                strings.onbPermTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                strings.onbPermSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            perms.forEach { perm ->
                val (title, desc) = when (perm) {
                    Perm.NOTIFICATIONS -> strings.onbPermNotifTitle to strings.onbPermNotifDesc
                    Perm.CAMERA -> strings.onbPermCameraTitle to strings.onbPermCameraDesc
                    Perm.INSTALL -> strings.onbPermInstallTitle to strings.onbPermInstallDesc
                }
                PermissionCard(
                    perm = perm,
                    title = title,
                    description = desc,
                    granted = granted[perm] == true,
                    openSettings = perm == Perm.INSTALL || perm in denied,
                    onAllow = { onAllow(perm) },
                )
            }

            Text(
                strings.onbPermFooter,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(strings.onbBack, maxLines = 1)
            }
            Button(
                onClick = { if (askable.isNotEmpty()) request(askable) else onFinish() },
                modifier = Modifier.weight(2f),
            ) {
                Text(if (askable.isNotEmpty()) strings.onbPermAllowAll else strings.onbStart, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    perm: Perm,
    title: String,
    description: String,
    granted: Boolean,
    openSettings: Boolean,
    onAllow: () -> Unit,
) {
    val strings = LocalStrings.current
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
                    perm.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    if (granted) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                strings.onbPermGranted,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        FilledTonalButton(onClick = onAllow) {
                            Text(
                                if (openSettings) strings.onbPermOpenSettings else strings.onbPermAllow,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

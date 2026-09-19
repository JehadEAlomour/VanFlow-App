package com.jehadalomour.flowvan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jehadalomour.flowvan.core.data.update.AppInstaller
import com.jehadalomour.flowvan.core.data.update.InstallProgress
import com.jehadalomour.flowvan.core.data.update.InstallResult
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.update_failed
import com.jehadalomour.flowvan.core.designsystem.resources.update_required_body
import com.jehadalomour.flowvan.core.designsystem.resources.update_required_title
import com.jehadalomour.flowvan.core.designsystem.resources.update_retry
import com.jehadalomour.flowvan.core.designsystem.resources.update_state_confirm
import com.jehadalomour.flowvan.core.designsystem.resources.update_state_downloading
import com.jehadalomour.flowvan.core.designsystem.resources.update_state_installing
import com.jehadalomour.flowvan.core.designsystem.resources.update_state_preparing
import com.jehadalomour.flowvan.core.designsystem.resources.update_state_verifying
import com.jehadalomour.flowvan.core.designsystem.resources.update_version_line
import com.jehadalomour.flowvan.core.domain.update.AvailableUpdate
import com.jehadalomour.flowvan.core.domain.update.CheckForUpdateUseCase
import com.jehadalomour.flowvan.core.domain.update.UpdateStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Holds the app shut when the office has retired the build this handset is running,
 * and installs the replacement without asking anyone to do anything.
 *
 * WHY A WALL AND NOT A BANNER. The people carrying these handsets are not
 * choosing between versions; most will never notice a banner and none of them
 * can judge whether a given update matters. Meanwhile an out-of-date build is
 * not a cosmetic problem here — it prices goods, applies tax and writes vouchers
 * the server has to accept. A build the office has declared unfit must stop,
 * not nag.
 *
 * WHY IT SITS OUTSIDE SIGN-IN, like [LocationLock] and for a sharper reason: the
 * build that most needs replacing is the one the server has already stopped
 * accepting tokens from. An update path that runs only for a signed-in rep can
 * repair every build except the ones actually broken.
 *
 * ZERO TAPS ON A PROVISIONED HANDSET. Where the app is device owner the whole
 * sequence — check, download, verify, install — happens with nothing on screen
 * but a progress bar, and the app is killed mid-bar as the OS replaces it. On a
 * handset that was never provisioned the same sequence ends at the system
 * confirm dialog instead, and the screen changes what it says so the salesman is
 * told to tap rather than told to wait.
 *
 * FAILS OPEN. [CheckForUpdateUseCase] answers [UpdateStatus.None] to every error,
 * so a van with no signal is never walled off by its own inability to ask. See
 * the reasoning there — it is the load-bearing decision in this feature.
 */
@Composable
fun UpdateGate(content: @Composable () -> Unit) {
    val checkForUpdate = koinInject<CheckForUpdateUseCase>()
    val installer = koinInject<AppInstaller>()
    val scope = rememberCoroutineScope()

    var required by remember { mutableStateOf<AvailableUpdate?>(null) }
    var progress by remember { mutableStateOf<InstallProgress?>(null) }
    var failed by remember { mutableStateOf(false) }
    var awaitingUser by remember { mutableStateOf(false) }

    suspend fun check() {
        // Only Required raises the wall. Optional is deliberately ignored here:
        // an update that may be skipped has no business interrupting a round, and
        // it will be taken anyway the next time the floor moves past it.
        (checkForUpdate() as? UpdateStatus.Required)?.let { required = it.update }
    }

    // Asked at once, so a walled-off handset says so at the depot rather than in
    // front of a customer.
    LaunchedEffect(Unit) { check() }

    // And again on every resume. These handsets are rarely restarted — they are
    // pocketed and taken out again for days — so a check that only ran at process
    // start would, in practice, run once a week.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { check() } }

    val update = required
    if (update == null) {
        content()
        return
    }

    // The install drives itself. Keyed on the versionCode so a server that moves
    // the floor again mid-download starts the new one rather than finishing the
    // superseded file.
    LaunchedEffect(update.versionCode, failed) {
        if (failed) return@LaunchedEffect
        awaitingUser = false
        val result = installer.downloadAndInstall(
            url = update.apkUrl,
            sha256 = update.sha256,
            expectedSize = update.sizeBytes,
            onProgress = { progress = it },
        )
        when (result) {
            // Nothing follows Started — the process is killed as it is replaced.
            InstallResult.Started -> Unit
            InstallResult.AwaitingUser -> awaitingUser = true
            is InstallResult.Failed -> failed = true
        }
    }

    // A van drives out of coverage far more often than anything else goes wrong
    // here, so a failure retries itself rather than waiting to be noticed. The
    // button stays because the retry is slow on purpose and a salesman standing
    // still should not have to wait out the timer.
    LaunchedEffect(failed) {
        if (!failed) return@LaunchedEffect
        delay(RETRY_DELAY_MS)
        progress = null
        failed = false
    }

    UpdateRequiredScreen(
        update = update,
        installedVersionName = installer.installedVersionName,
        progress = progress,
        failed = failed,
        awaitingUser = awaitingUser,
        onRetry = {
            progress = null
            failed = false
        },
    )
}

/** How long a failed attempt waits before going round again. */
private const val RETRY_DELAY_MS = 15_000L

@Composable
private fun UpdateRequiredScreen(
    update: AvailableUpdate,
    installedVersionName: String,
    progress: InstallProgress?,
    failed: Boolean,
    awaitingUser: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FB))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.update_required_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A3D),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.height(12.dp))
            Text(
                // "Wait" and "tap" are opposite instructions and only one of them
                // is ever right. A salesman told to wait in front of a dialog that
                // is waiting for them will wait until someone drives out to help.
                text = when {
                    failed -> stringResource(Res.string.update_failed)
                    awaitingUser -> stringResource(Res.string.update_state_confirm)
                    else -> stringResource(Res.string.update_required_body)
                },
                fontSize = 14.sp,
                color = Color(0xFF5A6B80),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.height(20.dp))

            if (!failed && !awaitingUser) {
                val downloading = progress as? InstallProgress.Downloading
                val fraction = downloading?.fraction
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF1D74F5),
                        trackColor = Color(0xFFDDE4EE),
                    )
                } else {
                    // Indeterminate whenever the size is unknown or the step is not
                    // the download — a bar that sits at 0% reads as frozen, and a
                    // frozen-looking update is one a salesman reboots out of.
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF1D74F5),
                        trackColor = Color(0xFFDDE4EE),
                    )
                }
                Box(Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        when (progress) {
                            is InstallProgress.Downloading -> Res.string.update_state_downloading
                            InstallProgress.Verifying -> Res.string.update_state_verifying
                            InstallProgress.Installing -> Res.string.update_state_installing
                            else -> Res.string.update_state_preparing
                        },
                    ),
                    fontSize = 13.sp,
                    color = Color(0xFF5A6B80),
                )
            }

            if (awaitingUser) {
                Box(Modifier.height(4.dp))
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF9AA8B8),
                )
            }

            if (failed) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D74F5),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.update_retry),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Box(Modifier.height(16.dp))
            // Not for the salesman — for whoever they phone. "It says now 1.4.1,
            // new 1.4.3" is the one sentence that turns a support call into a
            // diagnosis instead of a guess.
            Text(
                text = stringResource(
                    Res.string.update_version_line,
                    installedVersionName.ifBlank { "—" },
                    update.versionName.ifBlank { update.versionCode.toString() },
                ),
                fontSize = 11.sp,
                color = Color(0xFF9AA8B8),
                textAlign = TextAlign.Center,
            )
        }
    }
}

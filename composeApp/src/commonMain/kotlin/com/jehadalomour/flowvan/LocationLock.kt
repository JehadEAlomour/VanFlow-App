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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jehadalomour.flowvan.core.data.location.SettingsOpener
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.location_lock_body_off
import com.jehadalomour.flowvan.core.designsystem.resources.location_lock_body_permission
import com.jehadalomour.flowvan.core.designsystem.resources.location_lock_recheck
import com.jehadalomour.flowvan.core.designsystem.resources.location_lock_sign_out
import com.jehadalomour.flowvan.core.designsystem.resources.location_lock_title
import com.jehadalomour.flowvan.core.designsystem.resources.login_open_location_settings
import com.jehadalomour.flowvan.core.domain.usecase.LocationBlock
import com.jehadalomour.flowvan.core.domain.usecase.LocationGate
import com.jehadalomour.flowvan.core.domain.usecase.LogoutUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RefreshLocationRequirementUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Holds the whole app shut while a location-locked rep has location off.
 *
 * WHY IT IS HERE AND NOT AT SIGN-IN. It was at sign-in, and that enforced almost
 * nothing: a rep signs in once and then stays signed in for weeks, so the check
 * ran on a day when location happened to be on and never again. Turn location
 * off afterwards — or have the office tick the requirement after they had
 * already signed in — and the app went on working normally. The requirement has
 * to hold while the app is being USED, not at the one moment it was entered.
 *
 * RE-CHECKED ON EVERY RESUME, which is exactly the journey this screen sends
 * people on: the rep taps through to settings, changes the switch, and comes
 * back. Coming back is a resume, so the block clears by itself and there is
 * nothing to tap. While it is showing it also re-checks on a slow timer, for the
 * platforms and paths where a resume is not delivered — polling costs nothing
 * here because it only runs while the app is already stopped.
 *
 * A rep who is signed OUT is never blocked: the session carries no requirement
 * then, so the gate says nothing and the login screen shows normally. Locking
 * the sign-in screen itself would strand a rep who needs to sign in as someone
 * else to fix it.
 */
@Composable
fun LocationLock(content: @Composable () -> Unit) {
    val gate = koinInject<LocationGate>()
    val settings = koinInject<SettingsOpener>()
    val refreshRequirement = koinInject<RefreshLocationRequirementUseCase>()
    val logout = koinInject<LogoutUseCase>()
    val scope = rememberCoroutineScope()

    var block by remember { mutableStateOf(gate.check()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { block = gate.check() }

    if (block != LocationBlock.NONE) {
        /**
         * While the lock is up this is the ONLY thing still running, so it has
         * to ask both questions — has the phone changed, and has the OFFICE
         * changed its mind.
         *
         * The second one is not optional. The requirement is cached in the
         * session and refreshed by the catalogue sync, which this screen has
         * stopped from running: an office that turned the requirement off could
         * not reach the handset at all, and the rep sat looking at a lock whose
         * cause had already been removed with no way out but signing out.
         */
        LaunchedEffect(Unit) {
            while (true) {
                delay(RECHECK_MS)
                refreshRequirement()
                block = gate.check()
            }
        }
        LocationBlockedScreen(
            block = block,
            onOpenSettings = {
                if (block == LocationBlock.SERVICE_OFF) {
                    settings.openLocationSettings()
                } else {
                    settings.openAppSettings()
                }
            },
            onRecheck = {
                scope.launch {
                    refreshRequirement()
                    block = gate.check()
                }
            },
            onSignOut = {
                // The way out when nothing else works.
                //
                // Everything else on this screen depends on something the rep may
                // not have: a settings toggle they are allowed to change, or a
                // server they can reach. A van with no signal whose requirement
                // was set by mistake would otherwise have NO route at all — the
                // app would be a wall until someone drove back to the office.
                //
                // It gives nothing away. A signed-out rep carries no session and
                // can do nothing; signing back in runs the same check again.
                scope.launch {
                    logout()
                    block = gate.check()
                }
            },
        )
        return
    }

    content()
}

/**
 * How often the lock re-asks. Slow on purpose: it is one small request, but it
 * is a request on a rep's own data allowance, and nothing behind this screen is
 * waiting on it.
 */
private const val RECHECK_MS = 5_000L

@Composable
private fun LocationBlockedScreen(
    block: LocationBlock,
    onOpenSettings: () -> Unit,
    onRecheck: () -> Unit,
    onSignOut: () -> Unit,
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
                text = stringResource(Res.string.location_lock_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A3D),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.height(12.dp))
            Text(
                // Which fault it is decides what to tell them to do — the two are
                // fixed on different screens and the wrong instruction sends a rep
                // somewhere that already looks correct.
                text = stringResource(
                    if (block == LocationBlock.SERVICE_OFF) {
                        Res.string.location_lock_body_off
                    } else {
                        Res.string.location_lock_body_permission
                    },
                ),
                fontSize = 14.sp,
                color = Color(0xFF5A6B80),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.height(24.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1D74F5),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.login_open_location_settings),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(Modifier.height(10.dp))
            // A way through for a rep whose phone did not deliver the resume, or
            // who fixed it from the notification shade without ever leaving the
            // app. Cheap, and the alternative is a rep stuck staring at a screen
            // that is already out of date.
            OutlinedButton(
                onClick = onRecheck,
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                Text(text = stringResource(Res.string.location_lock_recheck))
            }
            Box(Modifier.height(6.dp))
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.location_lock_sign_out),
                    fontSize = 13.sp,
                    color = Color(0xFF8A97A8),
                )
            }
            Box(Modifier.height(12.dp))
            CircularProgressIndicator(
                modifier = Modifier.height(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF9AA8B8),
            )
        }
    }
}

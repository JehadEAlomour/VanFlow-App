package com.jehadalomour.flowvan.feature.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.jehadalomour.flowvan.core.common.error.CashFlowError
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.feature.auth.LoginEffect
import com.jehadalomour.flowvan.feature.auth.LoginEvent
import com.jehadalomour.flowvan.feature.auth.LoginViewModel
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// ── Light palette ───────────────────────────────────────────────────────────
private val Ink = Color(0xFF14212E)
private val Muted = Color(0xFF64748B)
private val Accent = Color(0xFF1466B8)
private val FieldBg = Color(0xFFFFFFFF)
private val FieldBorder = Color(0xFFD9E2EC)

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateHome -> onLoggedIn()
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF5F8FC),
            Color(0xFFFFFFFF),
            Color(0xFFEDF3F9),
        ),
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(gradient)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(28.dp))
                LogoBlock(onLogoLongPress = { showSettingsDialog = true })
                Spacer(Modifier.height(32.dp))

                LabeledField(label = stringResource(Res.string.login_phone_label)) {
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = { viewModel.onEvent(LoginEvent.PhoneChanged(it)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = lightFieldColors(),
                    )
                }

                Spacer(Modifier.height(20.dp))

                LabeledField(label = stringResource(Res.string.login_password)) {
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.onEvent(LoginEvent.TogglePasswordVisibility) }) {
                                Icon(
                                    painter = painterResource(
                                        if (state.passwordVisible) Res.drawable.ic_visibility else Res.drawable.ic_key
                                    ),
                                    contentDescription = null,
                                    tint = Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = lightFieldColors(),
                    )
                }

                // A device-binding refusal is not a "try again" error — the rep
                // cannot fix it from this screen, only the office can. It gets a
                // dialog rather than a chip they might miss and keep retyping
                // their password against.
                if (state.error != null && !state.error!!.isDeviceBlock()) {
                    Spacer(Modifier.height(16.dp))
                    ErrorChip(messageAr = state.error!!.messageAr, messageEn = state.error!!.messageEn)
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { viewModel.onEvent(LoginEvent.Submit) },
                    enabled = !state.isSubmitting && state.phone.isNotBlank() && state.password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFDCE5EE),
                        disabledContentColor = Color(0xFF9AA8B8),
                    ),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.login_button),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))
                Text(
                    text = "by 7software",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsPasswordDialog(
            onDismiss = { showSettingsDialog = false },
            onSuccess = {
                showSettingsDialog = false
                onOpenSettings()
            },
        )
    }

    state.error?.takeIf { it.isDeviceBlock() }?.let { blocked ->
        DeviceBlockedDialog(
            error = blocked,
            onDismiss = { viewModel.onEvent(LoginEvent.DismissError) },
        )
    }
}

private const val SETTINGS_PASSWORD = "542001"

@Composable
private fun SettingsPasswordDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "كلمة المرور", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = false
                },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = if (error) {
                    { Text(text = "كلمة المرور غير صحيحة", color = Color(0xFFB42318), fontSize = 12.sp) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = lightFieldColors(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (password == SETTINGS_PASSWORD) onSuccess() else error = true
            }) {
                Text(text = "دخول", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "إلغاء", color = Muted)
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogoBlock(onLogoLongPress: () -> Unit) {
    Image(
        painter = painterResource(Res.drawable.logo_7software),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(124.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLogoLongPress,
            ),
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(Res.string.login_brand),
        color = Ink,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(Res.string.login_brand_subtitle),
        color = Muted,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Ink,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun ErrorChip(messageAr: String, messageEn: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDEBEC)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = messageAr, color = Color(0xFFB42318), fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(text = messageEn, color = Color(0xFFB42318), fontSize = 12.sp)
        }
    }
}

@Composable
private fun lightFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg,
    disabledContainerColor = FieldBg,
    focusedIndicatorColor = Accent,
    unfocusedIndicatorColor = FieldBorder,
    cursorColor = Accent,
)


/** The two refusals only the office can clear. */
private fun CashFlowError.isDeviceBlock(): Boolean =
    this is CashFlowError.Auth.DeviceBoundToOtherUser ||
        this is CashFlowError.Auth.UserActiveOnOtherDevice

/**
 * Blocking explanation for a handset/account mismatch.
 *
 * Deliberately has one button and no retry: retrying is exactly the wrong
 * instinct here, and the only route forward is the office releasing the device.
 * The title names which of the two situations it is so the rep can say it down
 * the phone without reading a paragraph.
 */
@Composable
private fun DeviceBlockedDialog(error: CashFlowError, onDismiss: () -> Unit) {
    val isDeviceTaken = error is CashFlowError.Auth.DeviceBoundToOtherUser
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isDeviceTaken) "الجهاز مسجّل لمستخدم آخر" else "حسابك مسجّل على جهاز آخر",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        },
        text = { Text(text = error.messageAr, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("حسناً") }
        },
    )
}

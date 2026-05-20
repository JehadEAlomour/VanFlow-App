package com.jehadalomour.flowvan.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.shared.presentation.feature.login.LoginEffect
import com.jehadalomour.flowvan.shared.presentation.feature.login.LoginEvent
import com.jehadalomour.flowvan.shared.presentation.feature.login.LoginViewModel
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateHome -> onLoggedIn()
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF02192B),
            Color(0xFF000000),
            Color(0xFF02192B),
        ),
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                LogoBlock()
                Spacer(Modifier.height(32.dp))

                LabeledField(label = "رقم الهاتف / Phone Number") {
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = { viewModel.onEvent(LoginEvent.PhoneChanged(it)) },
                        placeholder = { Text("07XXXXXXXX", color = Color(0xFF7B8BAA)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = darkFieldColors(),
                    )
                }

                Spacer(Modifier.height(20.dp))

                LabeledField(label = "كلمة المرور / Password") {
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                        placeholder = { Text("••••••", color = Color(0xFF7B8BAA)) },
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
                                    tint = Color(0xFFEDF0FA),
                                    modifier = androidx.compose.ui.Modifier.size(18.dp),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = darkFieldColors(),
                    )
                }

                if (state.error != null) {
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
                        containerColor = Color(0xFF4B8FF6),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF243044),
                        disabledContentColor = Color(0xFF7B8BAA),
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
                            text = "⇨  تسجيل الدخول / Login",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                DemoCredsCard()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Al-Madina Trading Co. © 2026",
                    color = Color(0xFF7B8BAA),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

}

@Composable
private fun LogoBlock() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4B8FF6), Color(0xFF22D3C2)),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "CF",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "كاش فلو",
        color = Color(0xFFEDF0FA),
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "CashFlow — نظام مندوب الفان",
        color = Color(0xFF7B8BAA),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFFEDF0FA),
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
        colors = CardDefaults.cardColors(containerColor = Color(0x29F04F4F)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(text = messageAr, color = Color(0xFFFFBABA), fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(text = messageEn, color = Color(0xFFFFBABA), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DemoCredsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2232)),
    ) {
        Column(modifier = Modifier.padding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "بيانات تجريبية / Demo Credentials",
                color = Color(0xFFEDF0FA),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            DemoRow("أحمد المصري", "0791234567 / 1234")
            DemoRow("محمد الخالد", "0799876543 / 1234")
            DemoRow("فيصل النمر", "0795551234 / 1234 (مشرف)")
            DemoRow("سارة الأحمد", "0797778899 / 1234")
        }
    }
}

@Composable
private fun DemoRow(name: String, creds: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(text = name, color = Color(0xFFEDF0FA), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterStart))
        Text(
            text = creds,
            color = Color(0xFF7B8BAA),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun darkFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color(0xFFEDF0FA),
    unfocusedTextColor = Color(0xFFEDF0FA),
    focusedContainerColor = Color(0xFF1A2232),
    unfocusedContainerColor = Color(0xFF1A2232),
    disabledContainerColor = Color(0xFF1A2232),
    focusedIndicatorColor = Color(0xFF4B8FF6),
    unfocusedIndicatorColor = Color(0xFF1E2A3A),
    cursorColor = Color(0xFF4B8FF6),
    focusedPlaceholderColor = Color(0xFF7B8BAA),
    unfocusedPlaceholderColor = Color(0xFF7B8BAA),
)


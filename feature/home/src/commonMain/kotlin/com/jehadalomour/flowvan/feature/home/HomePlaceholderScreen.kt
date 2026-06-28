package com.jehadalomour.flowvan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.model.User
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun HomePlaceholderScreen(onLogout: () -> Unit) {
    val getCurrentUser: GetCurrentUserUseCase = koinInject()
    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        user = getCurrentUser()
        loading = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF080B12),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF4B8FF6),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = stringResource(Res.string.home_greeting_name, user?.nameAr ?: ""),
                        color = Color(0xFFEDF0FA),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.home_welcome_name, user?.nameEn ?: ""),
                        color = Color(0xFF7B8BAA),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(32.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2232)),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(Res.string.placeholder_phase_title),
                                color = Color(0xFF1DC97A),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.placeholder_logged_in),
                                color = Color(0xFFEDF0FA),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(Res.string.placeholder_phase2_next),
                                color = Color(0xFF7B8BAA),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF243044),
                            contentColor = Color(0xFFEDF0FA),
                        ),
                    ) {
                        Text(stringResource(Res.string.placeholder_logout))
                    }
                }
            }
        }
    }
}

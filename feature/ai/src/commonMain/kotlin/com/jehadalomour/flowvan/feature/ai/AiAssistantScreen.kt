package com.jehadalomour.flowvan.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.fvFieldColors
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.model.AiMessage
import com.jehadalomour.flowvan.feature.ai.AiAssistantEvent
import com.jehadalomour.flowvan.feature.ai.AiAssistantViewModel
import com.jehadalomour.flowvan.feature.ai.quickChips
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AiAssistantScreen(
    customerId: String?,
    onBack: () -> Unit,
    viewModel: AiAssistantViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    val totalItems = state.messages.size + (if (state.isThinking || state.isStreaming) 1 else 0)
    LaunchedEffect(totalItems) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    if (state.showApiKeyDialog) {
        ApiKeyDialog(
            value = state.apiKeyInput,
            onValueChange = { viewModel.onEvent(AiAssistantEvent.ApiKeyInputChanged(it)) },
            onSave = { viewModel.onEvent(AiAssistantEvent.SaveApiKey) },
            onDismiss = { viewModel.onEvent(AiAssistantEvent.DismissApiKeyDialog) },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Icon(
                    painter = painterResource(Res.drawable.ic_ai_sparkle),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.ai_title), color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (state.apiKeySet) stringResource(Res.string.ai_connected_claude) else stringResource(Res.string.ai_demo_mode),
                        color = if (state.apiKeySet) Fv.Green else Fv.TextMid,
                        fontSize = 10.sp,
                    )
                }
                IconButton(onClick = { viewModel.onEvent(AiAssistantEvent.OpenApiKeyDialog) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_settings),
                        contentDescription = null,
                        tint = if (state.apiKeySet) Fv.Green else Fv.TextMid,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { msg -> MessageBubble(msg) }
                if (state.isThinking) {
                    item { ThinkingBubble() }
                }
                if (state.isStreaming && state.streamingContent.isNotBlank()) {
                    item { StreamingBubble(state.streamingContent) }
                } else if (state.isStreaming) {
                    item { ThinkingBubble() }
                }
            }

            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(quickChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.onEvent(AiAssistantEvent.ChipTapped(chip)) }
                            .background(Fv.SurfaceHigh, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(chip, color = Fv.Blue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { viewModel.onEvent(AiAssistantEvent.InputChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.ai_input_hint), color = Fv.TextMid, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = fvFieldColors(),
                    singleLine = true,
                    enabled = !state.isThinking && !state.isStreaming,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { viewModel.onEvent(AiAssistantEvent.Send) }
                        .background(Fv.Blue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isStreaming) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Fv.TextHigh, strokeWidth = 2.dp)
                    } else {
                        Text("↑", color = Fv.TextHigh, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fv.Surface,
        title = { Text(stringResource(Res.string.ai_api_key_title), color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(stringResource(Res.string.ai_api_key_dialog_message), color = Fv.TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(Res.string.ai_api_key_hint), color = Fv.TextMid, fontSize = 12.sp) },
                    colors = fvFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.ai_api_key_local_note), color = Fv.TextMid, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(Res.string.save), color = Fv.Blue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = Fv.TextMid)
            }
        },
    )
}

@Composable
private fun MessageBubble(msg: AiMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Fv.Blue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_ai_sparkle),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .background(
                    if (isUser) Fv.Blue else Fv.Surface,
                    RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = msg.content, color = Fv.TextHigh, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun StreamingBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Fv.Purple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_ai_sparkle),
                contentDescription = null,
                tint = Fv.Purple,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .background(Fv.Surface, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = "$content ▌", color = Fv.TextHigh, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Fv.Blue.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_ai_sparkle),
                contentDescription = null,
                tint = Fv.Blue,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(Fv.Surface, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Fv.Blue, strokeWidth = 2.dp)
        }
    }
}

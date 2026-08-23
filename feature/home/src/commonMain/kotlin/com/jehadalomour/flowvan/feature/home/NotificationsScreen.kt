package com.jehadalomour.flowvan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.ReportTopBar
import com.jehadalomour.flowvan.core.network.dto.AppNotificationDto
import org.koin.compose.viewmodel.koinViewModel

/**
 * The rep's notifications. Tapping a stock-request alert marks it read and opens
 * that request so the rep can confirm receipt.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenStockRequest: (requestId: String) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        ReportTopBar(title = "الإشعارات", onBack = onBack)

        if (state.unread > 0) {
            Text(
                "تعليم الكل مقروء",
                color = Fv.Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { viewModel.onEvent(NotificationsEvent.MarkAllRead) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        if (state.items.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا إشعارات", color = Fv.TextMid, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.id }) { n ->
                    NotificationRow(n) {
                        viewModel.onEvent(NotificationsEvent.MarkRead(n.id))
                        if (n.refType == "stock-request" && n.refId != null) {
                            onOpenStockRequest(n.refId!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotificationDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (n.isUnread) Fv.Blue.copy(alpha = 0.08f) else Fv.Surface,
                RoundedCornerShape(10.dp),
            )
            .border(1.dp, if (n.isUnread) Fv.Blue.copy(alpha = 0.4f) else Fv.Border, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Unread dot
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .background(if (n.isUnread) Fv.Blue else Fv.Border, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                n.titleAr.ifEmpty { n.titleEn },
                color = Fv.TextHigh,
                fontSize = 13.sp,
                fontWeight = if (n.isUnread) FontWeight.Bold else FontWeight.Medium,
            )
            (n.bodyAr ?: n.bodyEn)?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Fv.TextMid, fontSize = 11.sp)
            }
            Text(n.createdAt.take(16).replace("T", " "), color = Fv.TextLow, fontSize = 10.sp)
        }
    }
}

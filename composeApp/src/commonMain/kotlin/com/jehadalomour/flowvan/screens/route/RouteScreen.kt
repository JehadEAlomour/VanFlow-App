package com.jehadalomour.flowvan.screens.route

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.ChurnChip
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.OverdueChip
import com.jehadalomour.flowvan.screens.components.TierBadge
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.presentation.feature.route.RouteEvent
import com.jehadalomour.flowvan.shared.presentation.feature.route.RouteViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RouteScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    viewModel: RouteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val visible = if (state.searchQuery.isBlank()) state.routeCustomers else state.searchResults

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                    Text(
                        "مسار اليوم",
                        color = Fv.TextHigh,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${state.visitedCount} / ${state.plannedCount}",
                        color = Fv.TextMid,
                        fontSize = 13.sp,
                    )
                }
            }
            item {
                ProgressBlock(visited = state.visitedCount, planned = state.plannedCount)
            }
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(RouteEvent.SearchChanged(it)) },
                    placeholder = { Text("ابحث بالاسم أو الكود أو المنطقة", color = Fv.TextMid) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors(),
                )
            }
            items(visible, key = { it.id }) { customer ->
                RouteCustomerCard(
                    customer,
                    onClick = { onOpenCustomer(customer.id) },
                    onNavigate = if (customer.lat != null && customer.lng != null) {
                        { onNavigateTo(customer.id) }
                    } else null,
                )
            }
            if (visible.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = if (state.searchQuery.isBlank()) "لا يوجد عملاء في المسار" else "لا نتائج",
                        color = Fv.TextMid,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBlock(visited: Int, planned: Int) {
    val ratio = if (planned > 0) visited.toFloat() / planned.toFloat() else 0f
    Column {
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Fv.Green,
            trackColor = Fv.SurfaceHigh,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "تمت زيارة $visited من أصل $planned",
            color = Fv.TextMid,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun RouteCustomerCard(customer: Customer, onClick: () -> Unit, onNavigate: (() -> Unit)?) {
    val cardBg = if (customer.churnRisk >= 0.80) Color(0x1AF04F4F) else Fv.Surface
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Fv.SurfaceTop, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(customer.visitOrder.toString(), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.nameAr, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${customer.code} · ${customer.area}", color = Fv.TextMid, fontSize = 11.sp)
                }
                TierBadge(customer.tier)
                if (onNavigate != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clickable { onNavigate() }
                            .background(Fv.Blue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text("🚗", fontSize = 16.sp)
                    }
                }
            }
            if (customer.overdueAmount > 0 || customer.churnRisk >= 0.60) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (customer.overdueAmount > 0) OverdueChip(customer.overdueAmount.formatJod(AppLanguage.AR))
                    if (customer.churnRisk >= 0.60) ChurnChip(customer.churnRisk)
                }
            }
        }
    }
}

@Composable
private fun darkFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Fv.TextHigh,
    unfocusedTextColor = Fv.TextHigh,
    focusedContainerColor = Fv.Surface,
    unfocusedContainerColor = Fv.Surface,
    focusedIndicatorColor = Fv.Blue,
    unfocusedIndicatorColor = Fv.Border,
    cursorColor = Fv.Blue,
    focusedPlaceholderColor = Fv.TextMid,
    unfocusedPlaceholderColor = Fv.TextMid,
)

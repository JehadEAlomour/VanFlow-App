package com.jehadalomour.flowvan.screens.home

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import com.jehadalomour.flowvan.screens.components.OverdueChip
import com.jehadalomour.flowvan.screens.components.TierBadge
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.presentation.feature.home.HomeEvent
import com.jehadalomour.flowvan.shared.presentation.feature.home.HomeViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.format.formatLevantine
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalTime::class)
@Composable
fun HomeScreen(
    onOpenRoute: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenVanStock: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenEndOfDay: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val today = Instant
        .fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TopBar(
                    onOpenAi = onOpenAi,
                    onLogout = onLogout,
                )
            }
            item {
                GreetingBlock(
                    name = state.user?.nameAr ?: "...",
                    dateText = today.formatLevantine(AppLanguage.AR),
                )
            }
            item {
                ShiftStatusCard(
                    isActive = state.activeShift != null,
                    onStartShift = { viewModel.onEvent(HomeEvent.StartShift) },
                )
            }
            item {
                KpiRow(
                    sales = state.kpi?.salesTotal ?: 0.0,
                    collections = state.kpi?.collectionsTotal ?: 0.0,
                    returns = state.kpi?.returnsTotal ?: 0.0,
                    visited = state.kpi?.customersVisited ?: 0,
                    planned = state.kpi?.customersPlanned ?: 0,
                )
            }
            item {
                ActionTilesGrid(
                    onOpenRoute = onOpenRoute,
                    onOpenCustomers = onOpenCustomers,
                    onOpenVanStock = onOpenVanStock,
                    onOpenAi = onOpenAi,
                    onOpenEndOfDay = onOpenEndOfDay,
                    onOpenReports = onOpenReports,
                )
            }
            item {
                RouteSectionHeader(onSeeAll = onOpenRoute)
            }
            items(state.routeTopFive, key = { it.id }) { customer ->
                CustomerRow(customer, onClick = { onOpenCustomer(customer.id) })
            }
        }
    }
}

@Composable
private fun ShiftStatusCard(isActive: Boolean, onStartShift: () -> Unit) {
    if (isActive) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Fv.Green, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "الوردية نشطة — يتم تتبع الموقع",
                    color = Fv.Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onStartShift),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Fv.Green.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_play_circle),
                        contentDescription = null,
                        tint = Fv.Green,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("بدء اليوم", color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("ابدأ الوردية لتفعيل تتبع الموقع", color = Fv.TextMid, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TopBar(onOpenAi: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "كاش فلو",
            color = Fv.TextHigh,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenAi) {
            Icon(
                painter = painterResource(Res.drawable.ic_ai_sparkle),
                contentDescription = null,
                tint = Fv.Purple,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onLogout) {
            Icon(
                painter = painterResource(Res.drawable.ic_logout),
                contentDescription = null,
                tint = Fv.TextMid,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun GreetingBlock(name: String, dateText: String) {
    Column {
        Text("أهلاً، $name", color = Fv.TextHigh, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(dateText, color = Fv.TextMid, fontSize = 13.sp)
    }
}

@Composable
private fun KpiRow(
    sales: Double, collections: Double, returns: Double,
    visited: Int, planned: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KpiCard("المبيعات", sales.formatJod(AppLanguage.AR), Fv.Blue, Modifier.weight(1f))
        KpiCard("التحصيلات", collections.formatJod(AppLanguage.AR), Fv.Green, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KpiCard("المرتجعات", returns.formatJod(AppLanguage.AR), Fv.Red, Modifier.weight(1f))
        KpiCard("الزيارات", "$visited / $planned", Fv.Amber, Modifier.weight(1f))
    }
}

@Composable
private fun KpiCard(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Fv.TextMid, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionTilesGrid(
    onOpenRoute: () -> Unit, onOpenCustomers: () -> Unit, onOpenVanStock: () -> Unit,
    onOpenAi: () -> Unit, onOpenEndOfDay: () -> Unit, onOpenReports: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(painterResource(Res.drawable.ic_truck), "مسار اليوم", Fv.Blue, Modifier.weight(1f), onOpenRoute)
            ActionTile(painterResource(Res.drawable.ic_customers), "قائمة العملاء", Fv.Teal, Modifier.weight(1f), onOpenCustomers)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(painterResource(Res.drawable.ic_inventory), "مخزون الفان", Fv.Amber, Modifier.weight(1f), onOpenVanStock)
            ActionTile(painterResource(Res.drawable.ic_moon), "نهاية اليوم", Fv.Red, Modifier.weight(1f), onOpenEndOfDay)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(painterResource(Res.drawable.ic_bar_chart), "التقارير", Fv.Green, Modifier.weight(1f), onOpenReports)
            ActionTile(painterResource(Res.drawable.ic_ai_sparkle), "المساعد الذكي", Fv.Purple, Modifier.weight(1f), onOpenAi)
        }
    }
}

@Composable
private fun ActionTile(icon: Painter, label: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RouteSectionHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("مسار اليوم", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(
            "عرض الكل",
            color = Fv.Blue,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onSeeAll).padding(8.dp),
        )
    }
}

@Composable
private fun CustomerRow(customer: Customer, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                Text(customer.area, color = Fv.TextMid, fontSize = 11.sp)
            }
            TierBadge(customer.tier)
        }
        if (customer.overdueAmount > 0 || customer.churnRisk >= 0.60) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (customer.overdueAmount > 0) OverdueChip(customer.overdueAmount.formatJod(AppLanguage.AR))
            }
        }
    }
}

package com.jehadalomour.flowvan.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.CustomerTier
import com.jehadalomour.flowvan.feature.home.HomeEvent
import com.jehadalomour.flowvan.feature.home.HomeViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.format.formatLevantine
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenRoute: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenVanStock: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenEndOfDay: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val today = Instant
        .fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    // Every time the home screen is shown (each ON_RESUME), pull fresh permissions,
    // company info + tax mode (and catalog) from the server.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(HomeEvent.Refresh)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.onEvent(HomeEvent.Refresh)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                TopBar(onLogout = onLogout, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
            item {
                HeroCard(
                    name = state.user?.nameAr ?: "...",
                    dateText = today.formatLevantine(AppLanguage.AR),
                    isShiftActive = state.activeShift != null,
                    pendingPings = state.pendingPings,
                    lastSyncAt = state.lastSyncAt,
                    onStartShift = { viewModel.onEvent(HomeEvent.StartShift) },
                    modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 12.dp),
                )
            }
            item {
                StatsGrid(
                    sales = state.kpi?.salesTotal ?: 0.0,
                    collections = state.kpi?.collectionsTotal ?: 0.0,
                    returns = state.kpi?.returnsTotal ?: 0.0,
                    visited = state.kpi?.customersVisited ?: 0,
                    planned = state.kpi?.customersPlanned ?: 0,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            item {
                VisitsProgressCard(
                    visited = state.kpi?.customersVisited ?: 0,
                    planned = state.kpi?.customersPlanned ?: 0,
                    modifier = Modifier.padding(horizontal = 14.dp).padding(top = 10.dp),
                )
            }
            item {
                SectionHeader(
                    title = stringResource(Res.string.home_quick_access),
                    modifier = Modifier.padding(horizontal = 14.dp).padding(top = 18.dp, bottom = 10.dp),
                )
            }
            item {
                ActionGrid(
                    onOpenRoute = onOpenRoute,
                    onOpenCustomers = onOpenCustomers,
                    onOpenVanStock = onOpenVanStock,
                    onOpenAi = onOpenAi,
                    onOpenEndOfDay = onOpenEndOfDay,
                    onOpenReports = onOpenReports,
                    onOpenOffers = onOpenOffers,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 18.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.route_title),
                        color = Fv.TextHigh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(Res.string.view_all),
                        color = Fv.Blue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onOpenRoute).padding(8.dp),
                    )
                }
            }
            item {
                RoutePreviewCard(
                    customers = state.routeTopFive,
                    onOpenCustomer = onOpenCustomer,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
        }
        } // PullToRefreshBox
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "كاش فلو",
            color = Fv.Blue,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        TopBarIconBtn(painterResource(Res.drawable.ic_logout), onClick = onLogout)
    }
}

@Composable
private fun TopBarIconBtn(icon: Painter, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Fv.SurfaceTop)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Fv.TextMid, modifier = Modifier.size(18.dp))
    }
}

// ── Hero Card ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    name: String,
    dateText: String,
    isShiftActive: Boolean,
    pendingPings: Int,
    lastSyncAt: Long?,
    onStartShift: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroGradient = Brush.linearGradient(listOf(Color(0xFF2265CD), Color(0xFF0C447C)))
    val infiniteTransition = rememberInfiniteTransition(label = "shift_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(heroGradient)
            .padding(20.dp),
    ) {
        Column {
            Text(
                stringResource(Res.string.home_greeting_name, name),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(dateText, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            if (isShiftActive) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4ADE80).copy(alpha = dotAlpha), CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.home_shift_active_tracking),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (pendingPings == 0) {
                            stringResource(Res.string.home_sync_synced)
                        } else {
                            stringResource(Res.string.home_sync_pending, pendingPings)
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    lastSyncAt?.let { ts ->
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(Res.string.home_sync_last, formatHhMm(ts)),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onStartShift)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_play_circle),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.home_shift_start), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Epoch-millis → "HH:mm" in the device timezone (mono-LTR digits). */
@OptIn(ExperimentalTime::class)
private fun formatHhMm(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "$h:$m"
}

// ── Stats Grid ────────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid(
    sales: Double,
    collections: Double,
    returns: Double,
    visited: Int,
    planned: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                label = stringResource(Res.string.home_stat_sales),
                value = sales.formatJod(AppLanguage.AR),
                subLabel = stringResource(Res.string.currency_jod),
                accent = Fv.Blue,
                icon = painterResource(Res.drawable.ic_cart),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(Res.string.home_stat_collections),
                value = collections.formatJod(AppLanguage.AR),
                subLabel = stringResource(Res.string.currency_jod),
                accent = Fv.Green,
                icon = painterResource(Res.drawable.ic_payment),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                label = stringResource(Res.string.home_stat_returns),
                value = returns.formatJod(AppLanguage.AR),
                subLabel = stringResource(Res.string.currency_jod),
                accent = Fv.Red,
                icon = painterResource(Res.drawable.ic_return_arrow),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(Res.string.home_stat_visits),
                value = "$visited / $planned",
                subLabel = stringResource(Res.string.home_stat_customers_today),
                accent = Fv.Amber,
                icon = painterResource(Res.drawable.ic_map),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    subLabel: String,
    accent: Color,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = Fv.TextMid,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(accent.copy(alpha = 0.14f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 22.sp)
            Spacer(Modifier.height(2.dp))
            Text(subLabel, color = Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Visits Progress Card ──────────────────────────────────────────────────────

@Composable
private fun VisitsProgressCard(visited: Int, planned: Int, modifier: Modifier = Modifier) {
    val ratio = if (planned > 0) visited.toFloat() / planned.toFloat() else 0f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(Res.drawable.ic_map),
                    contentDescription = null,
                    tint = Fv.Amber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(Res.string.home_visits_today),
                    color = Fv.TextMid,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text("$visited / $planned", color = Fv.Amber, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(10.dp))
            // Progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Fv.SurfaceTop),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(Fv.Amber, Color(0xFFF59E0B)))),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.home_visits_completed, visited), color = Fv.TextMid, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(Res.string.home_visits_remaining, (planned - visited).coerceAtLeast(0)), color = Fv.TextMid, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier)
}

// ── Action Grid ───────────────────────────────────────────────────────────────

@Composable
private fun ActionGrid(
    onOpenRoute: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenVanStock: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenEndOfDay: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenOffers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(painterResource(Res.drawable.ic_cart), stringResource(Res.string.offers_title), stringResource(Res.string.home_action_offers_sub), Fv.Purple, Modifier.weight(1f), onOpenOffers)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(painterResource(Res.drawable.ic_truck), stringResource(Res.string.route_title), stringResource(Res.string.home_action_route_sub), Fv.Blue, Modifier.weight(1f), onOpenRoute)
            ActionTile(painterResource(Res.drawable.ic_customers), stringResource(Res.string.home_action_customers), stringResource(Res.string.home_action_customers_sub), Fv.Teal, Modifier.weight(1f), onOpenCustomers)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(painterResource(Res.drawable.ic_inventory), stringResource(Res.string.van_stock_title), stringResource(Res.string.home_action_van_stock_sub), Fv.Amber, Modifier.weight(1f), onOpenVanStock)
            ActionTile(painterResource(Res.drawable.ic_moon), stringResource(Res.string.home_quick_end_of_day), stringResource(Res.string.home_action_end_of_day_sub), Fv.Red, Modifier.weight(1f), onOpenEndOfDay)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(painterResource(Res.drawable.ic_bar_chart), stringResource(Res.string.reports_title), stringResource(Res.string.home_action_reports_sub), Fv.Green, Modifier.weight(1f), onOpenReports)
            ActionTile(painterResource(Res.drawable.ic_ai_sparkle), stringResource(Res.string.ai_title), stringResource(Res.string.home_action_ai_sub), Fv.Purple, Modifier.weight(1f), onOpenAi)
        }
    }
}

@Composable
private fun ActionTile(
    icon: Painter,
    label: String,
    subLabel: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subLabel, color = Fv.TextMid, fontSize = 11.sp)
            }
        }
    }
}

// ── Route Preview Card ────────────────────────────────────────────────────────

@Composable
private fun RoutePreviewCard(
    customers: List<Customer>,
    onOpenCustomer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (customers.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            customers.forEachIndexed { index, customer ->
                RoutePreviewItem(customer = customer, onClick = { onOpenCustomer(customer.id) })
                if (index < customers.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Fv.Border))
                }
            }
        }
    }
}

@Composable
private fun RoutePreviewItem(customer: Customer, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Fv.SurfaceTop, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(customer.visitOrder.toString(), color = Fv.TextMid, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    customer.nameAr,
                    color = Fv.TextHigh,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(customer.area, color = Fv.TextMid, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            TierCategoryBadge(customer.tier)
        }
        if (customer.overdueAmount > 0) {
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .background(Fv.Red.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(
                        painterResource(Res.drawable.ic_warning),
                        contentDescription = null,
                        tint = Fv.Red,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        "${stringResource(Res.string.receivables_overdue_label)} ${customer.overdueAmount.formatJod(AppLanguage.AR)} ${stringResource(Res.string.currency_jod)}",
                        color = Fv.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TierCategoryBadge(tier: CustomerTier) {
    val (gradient, label) = when (tier) {
        CustomerTier.A -> Pair(
            Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))),
            stringResource(Res.string.home_category_a),
        )
        CustomerTier.B -> Pair(
            Brush.linearGradient(listOf(Color(0xFFC97B1A), Color(0xFF9A5C10))),
            stringResource(Res.string.home_category_b),
        )
        CustomerTier.C -> Pair(
            Brush.linearGradient(listOf(Color(0xFF637181), Color(0xFF3E4D5C))),
            stringResource(Res.string.home_category_c),
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(gradient)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

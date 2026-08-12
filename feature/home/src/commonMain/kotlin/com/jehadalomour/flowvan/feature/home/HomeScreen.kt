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
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.format.formatLevantine
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.BorderStroke

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
    onOpenReturnByItem: () -> Unit,
    onOpenStockRequest: () -> Unit,
    onOpenNewCustomer: () -> Unit,
    onOpenVoucherSummary: () -> Unit,
    onOpenSettings: () -> Unit,
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
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                DashboardHeader(
                    name = state.user?.nameAr ?: "…",
                    dateText = today.formatLevantine(AppLanguage.AR),
                    pendingPings = state.pendingPings,
                    onLogout = onLogout,
                )
            }

            // Starting the shift is the one thing that must happen before
            // anything else works, so it is a bar across the top rather than a
            // tile in the grid — and it disappears the moment it is done.
            if (state.activeShift == null) {
                item {
                    StartShiftBar(
                        onStartShift = { viewModel.onEvent(HomeEvent.StartShift) },
                        modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 12.dp),
                    )
                }
            }
            // ── The readout, below the fold on purpose ────────────────────────
            item {
                FiguresBlock(
                    sales = state.kpi?.salesTotal ?: 0.0,
                    collections = state.kpi?.collectionsTotal ?: 0.0,
                    visited = state.kpi?.customersVisited ?: 0,
                    planned = state.kpi?.customersPlanned ?: 0,
                    returns = state.kpi?.returnsTotal ?: 0.0,
                    modifier = Modifier.padding(horizontal = 14.dp).padding(top = 18.dp),
                )
            }
            item{ Spacer(modifier = Modifier.height(12.dp)) }
            // ── Actions, first and without scrolling ──────────────────────────
            item {
                FunctionGrid(
                    onOpenCustomers = onOpenCustomers,
                    onOpenRoute = onOpenRoute,
                    onOpenNewCustomer = onOpenNewCustomer,
                    onOpenVanStock = onOpenVanStock,
                    onOpenStockRequest = onOpenStockRequest,
                    onOpenReturnByItem = onOpenReturnByItem,
                    onOpenOffers = onOpenOffers,
                    onOpenReports = onOpenReports,
                    onOpenVoucherSummary = onOpenVoucherSummary,
                    onOpenEndOfDay = onOpenEndOfDay,
                    onOpenAi = onOpenAi,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }


        }
        } // PullToRefreshBox
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────


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


/** Epoch-millis → "HH:mm" in the device timezone (mono-LTR digits). */
@OptIn(ExperimentalTime::class)
private fun formatHhMm(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "$h:$m"
}

// ── Stats Grid ────────────────────────────────────────────────────────────────



// ── Visits Progress Card ──────────────────────────────────────────────────────


// ── Section Header ────────────────────────────────────────────────────────────


// ── Action Grid ───────────────────────────────────────────────────────────────



// ── Route Preview Card ────────────────────────────────────────────────────────


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

// ── Redesigned dashboard ─────────────────────────────────────────────────────
// Actions before information. The old layout opened on a hero card, a stats
// grid and a progress card, and put the buttons five blocks down — so a rep
// standing in a shop doorway scrolled before they could start working. The grid
// is now the first thing under the header and the figures moved beneath it:
// scrolling is for reading, never for acting.

/** Name, date, sync state, sign out. 64dp, and not a card. */
@Composable
private fun DashboardHeader(
    name: String,
    dateText: String,
    pendingPings: Int,
    onLogout: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(dateText, color = Fv.TextMid, fontSize = 11.sp, maxLines = 1)
        }
        // Unsent GPS pings are the honest proxy for "is this device reaching the
        // server", which is what a rep actually needs to know before they sell.
        val synced = pendingPings == 0
        Row(
            modifier = Modifier
                .background(
                    if (synced) Fv.Green.copy(alpha = 0.10f) else Fv.Amber.copy(alpha = 0.12f),
                    RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(if (synced) Fv.Green else Fv.Amber, RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (synced) stringResource(Res.string.home_synced) else stringResource(Res.string.home_pending_sync),
                color = if (synced) Fv.Green else Fv.Amber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onLogout, modifier = Modifier.size(40.dp)) {
            Icon(
                painterResource(Res.drawable.ic_logout),
                contentDescription = stringResource(Res.string.home_logout),
                tint = Fv.TextMid,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StartShiftBar(onStartShift: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onStartShift,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Fv.Blue,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(Res.drawable.ic_play_circle),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.home_start_shift),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/**
 * Every destination, three across.
 *
 * Nine tiles — a clean 3×3.
 *
 * بيع, مرتجع and تحصيل are absent because they need a customer before they mean
 * anything; they live on the customer page, where one is already chosen. Putting
 * them here would have meant inventing a "pick a shop first" flow, which is a
 * behaviour change rather than a redesign.
 *
 * الإعدادات, المساعد الذكي and إرجاع بالصنف were removed at the client's request.
 * Their callbacks stay in the signature so restoring a tile is one line — and so
 * the routes keep a caller, since dropping the parameters would make it look as
 * though those destinations were deleted rather than hidden.
 */
@Composable
private fun FunctionGrid(
    onOpenCustomers: () -> Unit,
    onOpenRoute: () -> Unit,
    onOpenNewCustomer: () -> Unit,
    onOpenVanStock: () -> Unit,
    onOpenStockRequest: () -> Unit,
    onOpenReturnByItem: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenVoucherSummary: () -> Unit,
    onOpenEndOfDay: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        Triple(Res.drawable.ic_customers, Res.string.customers_title, Fv.Blue) to onOpenCustomers,
        Triple(Res.drawable.ic_map, Res.string.route_title, Fv.Blue) to onOpenRoute,
        Triple(Res.drawable.ic_customers, Res.string.home_new_customer, Fv.Blue) to onOpenNewCustomer,
        Triple(Res.drawable.ic_truck, Res.string.van_stock_title, Fv.Teal) to onOpenVanStock,
        Triple(Res.drawable.ic_inventory, Res.string.stock_request_title, Fv.Teal) to onOpenStockRequest,
        Triple(Res.drawable.ic_cart, Res.string.offers_title, Fv.Amber) to onOpenOffers,
        Triple(Res.drawable.ic_bar_chart, Res.string.reports_title, Fv.TextHigh) to onOpenReports,
        Triple(Res.drawable.ic_receipt, Res.string.voucher_summary_title, Fv.TextHigh) to onOpenVoucherSummary,
        Triple(Res.drawable.ic_payment, Res.string.end_of_day_title, Fv.Amber) to onOpenEndOfDay,
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (spec, onClick) ->
                    FunctionTile(
                        iconRes = spec.first,
                        labelRes = spec.second,
                        tint = spec.third,
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a short final row aligned to the same column widths.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FunctionTile(
    iconRes: DrawableResource,
    labelRes: StringResource,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        color = Fv.Surface,
        border = BorderStroke(1.dp, Fv.Border),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(labelRes),
                color = Fv.TextHigh,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp,
            )
        }
    }
}

/** The day's numbers as a readout: one bordered block, no cards, no icons. */
@Composable
private fun FiguresBlock(
    sales: Double,
    collections: Double,
    returns: Double,
    visited: Int,
    planned: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Fv.Surface,
            border = BorderStroke(1.dp, Fv.Border),
        ) {
            Column {
                Row {
                    FigureCell(stringResource(Res.string.home_sales_today), sales.formatJod(AppLanguage.AR), Fv.TextHigh, Modifier.weight(1f))
                    VDivider()
                    FigureCell(stringResource(Res.string.home_collections_today), collections.formatJod(AppLanguage.AR), Fv.Green, Modifier.weight(1f))
                }
                HorizontalDivider(color = Fv.Border)
                Row {
                    FigureCell(stringResource(Res.string.home_returns_today), returns.formatJod(AppLanguage.AR), Fv.Red, Modifier.weight(1f))
                    VDivider()
                    FigureCell(stringResource(Res.string.home_visits_today), "$visited / $planned", Fv.TextHigh, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.dp).height(58.dp).background(Fv.Border))
}

@Composable
private fun FigureCell(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, color = Fv.TextMid, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

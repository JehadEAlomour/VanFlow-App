package com.jehadalomour.flowvan.screens.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.CustomerSegment
import com.jehadalomour.flowvan.shared.domain.model.CustomerTier
import com.jehadalomour.flowvan.shared.presentation.feature.route.RouteEvent
import com.jehadalomour.flowvan.shared.presentation.feature.route.RouteViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RouteScreen(
    onBack: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    viewModel: RouteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val visible = if (state.searchQuery.isBlank()) state.routeCustomers else state.searchResults

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Sticky Top Bar + Progress ─────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Fv.Surface,
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp).padding(top = 14.dp)) {
                // Title row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Fv.SurfaceTop)
                            .border(0.5.dp, Fv.Border, RoundedCornerShape(11.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_back),
                            contentDescription = null,
                            tint = Fv.TextHigh,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        stringResource(Res.string.route_title),
                        color = Fv.TextHigh,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(Fv.Blue)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${state.visitedCount} / ${state.plannedCount}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Fv.SurfaceTop)
                            .border(0.5.dp, Fv.Border, RoundedCornerShape(11.dp))
                            .clickable(onClick = onOpenCustomers),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(Res.string.all),
                            color = Fv.TextHigh,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Progress bar
                ProgressSection(visited = state.visitedCount, planned = state.plannedCount)
            }
        }
        // Divider below top bar
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))

        // ── Sticky Search ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Fv.Surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(RouteEvent.SearchChanged(it)) },
                placeholder = { Text(stringResource(Res.string.route_search_hint), color = Fv.TextMid, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Fv.TextHigh,
                    unfocusedTextColor = Fv.TextHigh,
                    focusedContainerColor = Fv.Surface,
                    unfocusedContainerColor = Fv.Surface,
                    focusedIndicatorColor = Fv.Blue,
                    unfocusedIndicatorColor = Fv.Border,
                    cursorColor = Fv.Blue,
                    focusedPlaceholderColor = Fv.TextMid,
                    unfocusedPlaceholderColor = Fv.TextMid,
                ),
            )
        }

        // ── Scrollable Customer List ──────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Fv.BgDeepest),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(visible, key = { it.id }) { customer ->
                RouteCustomerCard(
                    customer = customer,
                    onClick = { onOpenCustomer(customer.id) },
                    onNavigate = if (customer.lat != null && customer.lng != null) {
                        { onNavigateTo(customer.id) }
                    } else null,
                )
            }
            if (visible.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = if (state.searchQuery.isBlank()) stringResource(Res.string.route_empty) else stringResource(Res.string.route_no_results),
                        color = Fv.TextMid,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

// ── Progress Section ──────────────────────────────────────────────────────────

@Composable
private fun ProgressSection(visited: Int, planned: Int) {
    val ratio = if (planned > 0) (visited.toFloat() / planned.toFloat()).coerceIn(0f, 1f) else 0f
    val remaining = (planned - visited).coerceAtLeast(0)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Fv.SurfaceTop),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1D9E75), Color(0xFF4ADE80)))),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painterResource(Res.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = Fv.Green,
                    modifier = Modifier.size(13.dp),
                )
                Text(stringResource(Res.string.route_visited_count, visited), color = Fv.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(Res.string.route_remaining_count, remaining), color = Fv.TextLow, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Route Customer Card ───────────────────────────────────────────────────────

@Composable
private fun RouteCustomerCard(customer: Customer, onClick: () -> Unit, onNavigate: (() -> Unit)?) {
    val tierBrush = when (customer.tier) {
        CustomerTier.A -> Brush.verticalGradient(listOf(Color(0xFF1D9E75), Color(0xFF4ADE80)))
        CustomerTier.B -> Brush.verticalGradient(listOf(Color(0xFFB36C00), Color(0xFFF59E0B)))
        CustomerTier.C -> Brush.verticalGradient(listOf(Color(0xFF637181), Color(0xFF8A9AB0)))
    }
    val hasFooter = customer.overdueAmount > 0 ||
        customer.churnRisk >= 0.60 ||
        customer.segment == CustomerSegment.CHAMPIONS

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Fv.Surface)
            .border(0.5.dp, Fv.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        // In RTL: Row lays children start→end = right→left.
        // Stripe is the first child → placed at the right edge (start side in RTL).
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Accent stripe (right/start side in RTL)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(tierBrush),
            )
            // Card content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
            ) {
                // ── Top Row ──────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Visit order bubble
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Fv.SurfaceTop)
                            .border(0.5.dp, Fv.Border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            customer.visitOrder.toString(),
                            color = Fv.TextMid,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // Name + meta
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            customer.nameAr,
                            color = Fv.TextHigh,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${customer.area} · ${customer.code}",
                            color = Fv.TextLow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Tier badge + nav button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        RouteTierBadge(customer.tier)
                        if (onNavigate != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C))),
                                    )
                                    .clickable(onClick = onNavigate),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_map),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }

                // ── Footer Tags ───────────────────────────────────────────────
                if (hasFooter) {
                    Spacer(Modifier.height(9.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.SurfaceTop))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (customer.overdueAmount > 0) {
                            RouteTag(
                                bg = Fv.Red.copy(alpha = 0.12f),
                                fg = Fv.Red,
                                icon = Res.drawable.ic_warning,
                                label = stringResource(Res.string.route_overdue_amount, customer.overdueAmount.formatJod(AppLanguage.AR)),
                            )
                        }
                        if (customer.churnRisk >= 0.60) {
                            RouteTag(
                                bg = Fv.Amber.copy(alpha = 0.14f),
                                fg = Fv.Amber,
                                icon = Res.drawable.ic_alarm,
                                label = stringResource(Res.string.route_churn_pct, (customer.churnRisk * 100).toInt()),
                            )
                        }
                        if (customer.segment == CustomerSegment.CHAMPIONS) {
                            RouteTag(
                                bg = Fv.Purple.copy(alpha = 0.12f),
                                fg = Fv.Purple,
                                icon = Res.drawable.ic_ai_sparkle,
                                label = stringResource(Res.string.route_champion),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tier Badge ────────────────────────────────────────────────────────────────

@Composable
private fun RouteTierBadge(tier: CustomerTier) {
    val (label, colors) = when (tier) {
        CustomerTier.A -> stringResource(Res.string.route_tier_a) to listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))
        CustomerTier.B -> stringResource(Res.string.route_tier_b) to listOf(Color(0xFFC97B1A), Color(0xFF9A5C10))
        CustomerTier.C -> stringResource(Res.string.route_tier_c) to listOf(Color(0xFF637181), Color(0xFF3E4D5C))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(colors))
            .padding(horizontal = 13.dp, vertical = 5.dp),
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Route Tag ─────────────────────────────────────────────────────────────────

@Composable
private fun RouteTag(bg: Color, fg: Color, icon: DrawableResource, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(11.dp),
        )
        Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

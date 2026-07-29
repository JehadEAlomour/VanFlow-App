package com.jehadalomour.flowvan.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.CustomerSegment
import com.jehadalomour.flowvan.core.model.CustomerTier
import com.jehadalomour.flowvan.feature.customer.CustomerListEvent
import com.jehadalomour.flowvan.feature.customer.CustomerListViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    onAddCustomer: () -> Unit = {},
    viewModel: CustomerListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Sticky Top Bar ────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Fv.Surface,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Fv.SurfaceTop)
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
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(Res.string.customers_list_title),
                    color = Fv.TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                // Only reps with the canAddCustomer permission see the create button.
                if (state.canAddCustomer) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Fv.Blue)
                            .clickable(onClick = onAddCustomer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Fv.SurfaceTop)
                        .padding(horizontal = 13.dp, vertical = 5.dp),
                ) {
                    Text(
                        "${state.visible.size}",
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        // ── Scrollable List ───────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Fv.BgDeepest),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Search
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(CustomerListEvent.SearchChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.customers_search_name_code_area_hint), color = Fv.TextMid, fontSize = 13.sp) },
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

            // Tier/segment filters removed — the list shows all customers (search only).

            // Customer cards
            items(state.visible, key = { it.id }) { customer ->
                CustomerCard(
                    customer = customer,
                    onClick = { onOpenCustomer(customer.id) },
                    onNavigate = if (customer.lat != null && customer.lng != null) {
                        { onNavigateTo(customer.id) }
                    } else null,
                )
            }

            if (state.visible.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        stringResource(Res.string.customers_no_results),
                        color = Fv.TextMid,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

// ── Tier Filter Pill ──────────────────────────────────────────────────────────

@Composable
private fun TierPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Fv.Blue else Fv.Surface)
            .border(0.5.dp, if (active) Fv.Blue else Fv.Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active) Color.White else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Segment Filter Pill ───────────────────────────────────────────────────────

@Composable
private fun SegmentPill(
    label: String,
    active: Boolean,
    activeBg: Color,
    activeFg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) activeBg else Fv.Surface)
            .border(0.5.dp, if (active) activeFg.copy(alpha = 0.4f) else Fv.Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active) activeFg else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Customer Card ─────────────────────────────────────────────────────────────

@Composable
private fun CustomerCard(customer: Customer, onClick: () -> Unit, onNavigate: (() -> Unit)?) {
    val hasFooter = customer.overdueAmount > 0 || !customer.isOnRoute || customer.segment != CustomerSegment.REGULAR
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Fv.Surface)
            .border(0.5.dp, Fv.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            // ── Top Section ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CustomerAvatar(seed = customer.code, letter = customer.nameAr.firstOrNull()?.toString() ?: "؟")
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        customer.nameAr,
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${customer.area} · ${customer.code}",
                        color = Fv.TextMid,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.width(10.dp))
                // Right column
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TierGradientBadge(customer.tier)
                    val hasDebt = customer.overdueAmount > 0
                    val amountColor = when {
                        hasDebt -> Fv.Red
                        customer.balance > 0 -> Fv.Green
                        else -> Fv.TextMid
                    }
                    if (customer.balance != 0.0) {
                        Text(
                            customer.balance.formatJod(AppLanguage.AR),
                            color = amountColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    if (onNavigate != null) {
                        NavButton(onNavigate)
                    }
                }
            }

            // ── Footer Tags ───────────────────────────────────────────────────
            if (hasFooter) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(0.5.dp).background(Fv.Border))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SegmentTag(customer.segment)
                    if (!customer.isOnRoute) {
                        FooterTag(
                            bg = Fv.SurfaceTop,
                            fg = Fv.TextMid,
                            icon = painterResource(Res.drawable.ic_map),
                            label = stringResource(Res.string.customers_off_route),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (customer.overdueAmount > 0) {
                        FooterTag(
                            bg = Fv.Red.copy(alpha = 0.12f),
                            fg = Fv.Red,
                            icon = painterResource(Res.drawable.ic_warning),
                            label = stringResource(Res.string.customers_overdue_amount, customer.overdueAmount.formatJod(AppLanguage.AR)),
                        )
                    }
                }
            }
        }
    }
}

// ── Customer Avatar ───────────────────────────────────────────────────────────

@Composable
private fun CustomerAvatar(seed: String, letter: String) {
    val gradients = listOf(
        listOf(Color(0xFF185FA5), Color(0xFF0C447C)),
        listOf(Color(0xFF1D9E75), Color(0xFF0F6E56)),
        listOf(Color(0xFFC97B1A), Color(0xFF9A5C10)),
        listOf(Color(0xFF7F5FD4), Color(0xFF534AB7)),
        listOf(Color(0xFFD85A30), Color(0xFF993C1D)),
        listOf(Color(0xFFD4537E), Color(0xFF993556)),
    )
    val colors = gradients[abs(seed.hashCode()) % gradients.size]
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Tier Badge ────────────────────────────────────────────────────────────────

@Composable
private fun TierGradientBadge(tier: CustomerTier) {
    val gradient = when (tier) {
        CustomerTier.A -> Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF0F6E56)))
        CustomerTier.B -> Brush.linearGradient(listOf(Color(0xFFC97B1A), Color(0xFF9A5C10)))
        CustomerTier.C -> Brush.linearGradient(listOf(Color(0xFF637181), Color(0xFF3E4D5C)))
    }
    val label = when (tier) {
        CustomerTier.A -> stringResource(Res.string.customers_tier_prefix, "A")
        CustomerTier.B -> stringResource(Res.string.customers_tier_prefix, "B")
        CustomerTier.C -> stringResource(Res.string.customers_tier_prefix, "C")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(gradient)
            .padding(horizontal = 13.dp, vertical = 5.dp),
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Navigation Button ─────────────────────────────────────────────────────────

@Composable
private fun NavButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2265CD), Color(0xFF0C447C))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(Res.drawable.ic_map),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ── Footer Tag ────────────────────────────────────────────────────────────────

@Composable
private fun FooterTag(bg: Color, fg: Color, icon: Painter, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentTag(segment: CustomerSegment) {
    val (bg, fg, icon, label) = when (segment) {
        CustomerSegment.CHAMPIONS -> Quad(
            Fv.Purple.copy(alpha = 0.14f), Fv.Purple,
            Res.drawable.ic_ai_sparkle, stringResource(Res.string.customers_segment_champions),
        )
        CustomerSegment.LOYAL -> Quad(
            Fv.Blue.copy(alpha = 0.12f), Fv.Blue,
            Res.drawable.ic_check_circle, stringResource(Res.string.customers_segment_loyal),
        )
        CustomerSegment.AT_RISK -> Quad(
            Fv.Red.copy(alpha = 0.12f), Fv.Red,
            Res.drawable.ic_warning, stringResource(Res.string.customers_segment_at_risk),
        )
        CustomerSegment.PROMISING -> Quad(
            Fv.Amber.copy(alpha = 0.14f), Fv.Amber,
            Res.drawable.ic_alarm, stringResource(Res.string.customers_segment_promising),
        )
        CustomerSegment.DORMANT -> Quad(
            Fv.SurfaceTop, Fv.TextMid,
            Res.drawable.ic_radio_button_off, stringResource(Res.string.customers_segment_dormant),
        )
        CustomerSegment.REGULAR -> Quad(
            Fv.SurfaceTop, Fv.TextMid,
            Res.drawable.ic_check, stringResource(Res.string.customers_segment_regular),
        )
    }
    FooterTag(bg = bg, fg = fg, icon = painterResource(icon), label = label)
}

private data class Quad(
    val bg: Color,
    val fg: Color,
    val iconRes: DrawableResource,
    val label: String,
)

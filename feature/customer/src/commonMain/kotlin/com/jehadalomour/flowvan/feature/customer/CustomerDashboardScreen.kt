package com.jehadalomour.flowvan.feature.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.jehadalomour.flowvan.core.model.CustomerSegment
import com.jehadalomour.flowvan.core.model.CustomerTier
import com.jehadalomour.flowvan.feature.customer.CustomerDashboardState
import com.jehadalomour.flowvan.feature.customer.CustomerDashboardViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CustomerDashboardScreen(
    customerId: String,
    onBack: () -> Unit,
    onOpenSale: (String) -> Unit,
    onOpenReturn: (String) -> Unit,
    onOpenRequest: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenAi: (String) -> Unit,
    onOpenVoucherReport: (String) -> Unit,
    onOpenPaymentReport: (String) -> Unit,
    onOpenAccountStatement: (String) -> Unit,
    onOpenDetailedTxnReport: (String) -> Unit = {},
    viewModel: CustomerDashboardViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    // The salesman "started a transaction" if they opened any create action this visit.
    var startedTxn by remember { mutableStateOf(false) }
    // Pop back once the visit has been recorded.
    LaunchedEffect(state.navigateBack) { if (state.navigateBack) onBack() }
    // Refresh the board every time it becomes visible again (e.g. returning from a
    // sale/return/collection or the invoice print screen) so balances/totals are current.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val requestLeave = { viewModel.onEvent(CustomerDashboardEvent.LeaveRequested(startedTxn)) }
    // System back (Android) mirrors the top-bar back button: run the leave flow so the
    // visit is recorded / the confirm-reason dialog shows — instead of a silent pop. If a
    // leave dialog is already open, back dismisses it.
    AppBackHandler {
        if (state.leaveDialog != LeaveDialog.NONE) {
            viewModel.onEvent(CustomerDashboardEvent.DismissLeave)
        } else {
            requestLeave()
        }
    }

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
                        .clickable(onClick = requestLeave),
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
                    stringResource(Res.string.customer_card_title),
                    color = Fv.TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Fv.Purple.copy(alpha = 0.12f))
                        .clickable { onOpenAi(customerId) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_ai_sparkle),
                        contentDescription = null,
                        tint = Fv.Purple,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ── Scrollable Content ────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f).background(Fv.BgDeepest),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HeroCard(state) }
            item { AccountSummaryCard(state) }
            item {
                ReportCardsGrid(
                    state = state,
                    onOpenVoucherReport = { onOpenVoucherReport(customerId) },
                    onOpenPaymentReport = { onOpenPaymentReport(customerId) },
                )
            }
            item { StatementCard(onOpenAccountStatement = { onOpenAccountStatement(customerId) }) }
            item {
                DetailedTxnCard(onOpen = { onOpenDetailedTxnReport(customerId) })
            }
        }

        // ── Bottom Action Bar ─────────────────────────────────────────────────
        // Location-locked reps can't act unless they're at the customer (~1 km).
        BottomActionBar(
            enabled = state.actionsEnabled,
            blockReason = when (state.proximityBlock) {
                ProximityBlock.NO_GPS -> stringResource(Res.string.proximity_blocked_gps)
                ProximityBlock.TOO_FAR -> stringResource(Res.string.proximity_blocked_far)
                ProximityBlock.NONE -> null
            },
            onSale = { startedTxn = true; onOpenSale(customerId) },
            onReturn = { startedTxn = true; onOpenReturn(customerId) },
            onRequest = { startedTxn = true; onOpenRequest(customerId) },
            onCollection = { startedTxn = true; onOpenCollection(customerId) },
        )
    }

    // Leaving a customer with no transaction → reason note (if required) or confirm.
    LeaveCustomerDialog(
        dialog = state.leaveDialog,
        onConfirm = { reason -> viewModel.onEvent(CustomerDashboardEvent.ConfirmLeave(reason)) },
        onDismiss = { viewModel.onEvent(CustomerDashboardEvent.DismissLeave) },
    )
}

@Composable
private fun LeaveCustomerDialog(
    dialog: LeaveDialog,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    if (dialog == LeaveDialog.NONE) return
    var reason by remember(dialog) { mutableStateOf("") }
    val needsReason = dialog == LeaveDialog.REASON
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (needsReason) stringResource(Res.string.dlg_leave_reason_title) else stringResource(Res.string.dlg_leave_confirm_title)) },
        text = {
            if (needsReason) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text(stringResource(Res.string.dlg_leave_reason_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            } else {
                Text(stringResource(Res.string.dlg_leave_confirm_msg))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (needsReason) reason else null) },
                enabled = !needsReason || reason.isNotBlank(),
            ) { Text(stringResource(Res.string.action_exit)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

// ── Hero Card ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(state: CustomerDashboardState) {
    val c = state.customer ?: return
    val heroGradient = Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(heroGradient),
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(80.dp)),
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50.dp)),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Tier badge + Segment tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroTierBadge(c.tier)
                SegmentStatusTag(c.segment)
            }
            Spacer(Modifier.height(16.dp))
            // Name
            Text(
                c.nameAr,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${c.code} · ${c.area}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(20.dp))
            // Frosted stat boxes
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrostedStatBox(
                    label = stringResource(Res.string.balance_label),
                    value = c.balance.formatJod(AppLanguage.AR),
                    valueColor = if (c.balance < 0) Color(0xFFFF8080) else Color.White,
                    modifier = Modifier.weight(1f),
                )
                FrostedStatBox(
                    label = stringResource(Res.string.receivables_overdue_label),
                    value = c.overdueAmount.formatJod(AppLanguage.AR),
                    valueColor = if (c.overdueAmount > 0) Color(0xFFFFB570) else Color.White,
                    modifier = Modifier.weight(1f),
                )
                FrostedStatBox(
                    label = stringResource(Res.string.customer_credit_ceiling),
                    value = c.creditLimit.formatJod(AppLanguage.AR),
                    valueColor = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroTierBadge(tier: CustomerTier) {
    val (label, colors) = when (tier) {
        CustomerTier.A -> stringResource(Res.string.customer_tier_a) to listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))
        CustomerTier.B -> stringResource(Res.string.customer_tier_b) to listOf(Color(0xFFC97B1A), Color(0xFF9A5C10))
        CustomerTier.C -> stringResource(Res.string.customer_tier_c) to listOf(Color(0xFF637181), Color(0xFF3E4D5C))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(colors))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SegmentStatusTag(segment: CustomerSegment) {
    val (dotColor, label) = when (segment) {
        CustomerSegment.CHAMPIONS -> Color(0xFFB5F5D5) to stringResource(Res.string.customer_segment_champions)
        CustomerSegment.LOYAL -> Color(0xFFAACAFF) to stringResource(Res.string.customer_segment_loyal)
        CustomerSegment.AT_RISK -> Color(0xFFFFAAAA) to stringResource(Res.string.customer_segment_at_risk)
        CustomerSegment.PROMISING -> Color(0xFFFFD9A0) to stringResource(Res.string.customer_segment_promising)
        CustomerSegment.DORMANT -> Color(0xFFBBCCDD) to stringResource(Res.string.customer_segment_dormant)
        CustomerSegment.REGULAR -> Color(0xFFBBCCDD) to stringResource(Res.string.customer_segment_regular)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, RoundedCornerShape(4.dp)),
        )
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FrostedStatBox(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Account Summary Card ──────────────────────────────────────────────────────

@Composable
private fun AccountSummaryCard(state: CustomerDashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        border = BorderStroke(0.5.dp, Fv.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // Section header
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Fv.Blue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_receipt),
                        contentDescription = null,
                        tint = Fv.Blue,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Text(stringResource(Res.string.customer_account_summary), color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryIconRow(
                    iconRes = Res.drawable.ic_receipt,
                    label = stringResource(Res.string.voucher_report_count),
                    value = "${state.sales.size + state.returns.size + state.requests.size}",
                    accent = Fv.Blue,
                )
                SummaryIconRow(
                    iconRes = Res.drawable.ic_cart,
                    label = stringResource(Res.string.all_sales_total_sales),
                    value = state.salesTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Green,
                )
                SummaryIconRow(
                    iconRes = Res.drawable.ic_return_arrow,
                    label = stringResource(Res.string.all_sales_total_returns),
                    value = state.returnsTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Red,
                )
                SummaryIconRow(
                    iconRes = Res.drawable.ic_payment,
                    label = stringResource(Res.string.all_payments_total),
                    value = state.collectionsTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Teal,
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
                // Highlighted balance row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Fv.SurfaceTop)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Fv.Blue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_bar_chart),
                            contentDescription = null,
                            tint = Fv.Blue,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(Res.string.customer_current_balance),
                        color = Fv.TextHigh,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    val balance = state.customer?.balance ?: 0.0
                    Text(
                        balance.formatJod(AppLanguage.AR),
                        color = when {
                            balance < 0 -> Fv.Red
                            balance > 0 -> Fv.Green
                            else -> Fv.TextMid
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryIconRow(iconRes: DrawableResource, label: String, value: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Report Cards Grid ─────────────────────────────────────────────────────────

@Composable
private fun ReportCardsGrid(
    state: CustomerDashboardState,
    onOpenVoucherReport: () -> Unit,
    onOpenPaymentReport: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReportCard(
            iconRes = Res.drawable.ic_receipt,
            iconGradient = Brush.linearGradient(listOf(Color(0xFF2C6FE4), Color(0xFF185FA5))),
            label = stringResource(Res.string.customer_report_vouchers),
            value = state.salesTotal.formatJod(AppLanguage.AR),
            accent = Fv.Blue,
            modifier = Modifier.weight(1f),
            onClick = onOpenVoucherReport,
        )
        ReportCard(
            iconRes = Res.drawable.ic_payment,
            iconGradient = Brush.linearGradient(listOf(Color(0xFF0FA968), Color(0xFF0A7A4B))),
            label = stringResource(Res.string.customer_payments_report),
            value = state.collectionsTotal.formatJod(AppLanguage.AR),
            accent = Fv.Green,
            modifier = Modifier.weight(1f),
            onClick = onOpenPaymentReport,
        )
    }
}

@Composable
private fun ReportCard(
    iconRes: DrawableResource,
    iconGradient: Brush,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        border = BorderStroke(0.5.dp, Fv.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(label, color = Fv.TextMid, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = Fv.TextHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(Res.string.customer_view_report), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Statement Card ────────────────────────────────────────────────────────────

@Composable
private fun StatementCard(onOpenAccountStatement: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAccountStatement),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        border = BorderStroke(0.5.dp, Fv.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7757D4), Color(0xFF4E3598)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_bar_chart),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.statement_title), color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(Res.string.customer_statement_subtitle), color = Fv.TextMid, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Fv.Purple.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(stringResource(Res.string.filter), color = Fv.Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Bottom Action Bar ─────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(
    enabled: Boolean,
    blockReason: String?,
    onSale: () -> Unit,
    onReturn: () -> Unit,
    onRequest: () -> Unit,
    onCollection: () -> Unit,
) {
    Surface(color = Fv.Surface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Location-lock banner: explains why the actions below are disabled.
            if (!enabled && blockReason != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Fv.Red.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .padding(bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_map),
                        contentDescription = null,
                        tint = Fv.Red,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        blockReason,
                        color = Fv.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                stringResource(Res.string.customer_quick_actions),
                color = Fv.TextMid,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(
                    iconRes = Res.drawable.ic_cart,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFF0FA968), Color(0xFF0A7A4B))),
                    label = stringResource(Res.string.customer_action_sale),
                    labelColor = Fv.Green,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onSale,
                )
                ActionTile(
                    iconRes = Res.drawable.ic_return_arrow,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFFD63B3B), Color(0xFF992828))),
                    label = stringResource(Res.string.customer_action_return),
                    labelColor = Fv.Red,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onReturn,
                )
                ActionTile(
                    iconRes = Res.drawable.ic_receipt,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFF0E9E91), Color(0xFF0A6E66))),
                    label = stringResource(Res.string.customer_action_request),
                    labelColor = Fv.Teal,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onRequest,
                )
                ActionTile(
                    iconRes = Res.drawable.ic_payment,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFFB36C00), Color(0xFF7A4A00))),
                    label = stringResource(Res.string.customer_action_collection),
                    labelColor = Fv.Amber,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onCollection,
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    iconRes: DrawableResource,
    iconGradient: Brush,
    label: String,
    labelColor: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .then(if (enabled) Modifier else Modifier.alpha(0.4f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** التقرير المفصل للحركات — every voucher in a period with its item lines. */
@Composable
private fun DetailedTxnCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        border = BorderStroke(0.5.dp, Fv.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1FA4A4), Color(0xFF127070)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_receipt),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.detailed_txn_title),
                    color = Fv.TextHigh,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(Res.string.detailed_txn_subtitle),
                    color = Fv.TextMid,
                    fontSize = 11.sp,
                )
            }
            Icon(
                painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Fv.TextMid,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

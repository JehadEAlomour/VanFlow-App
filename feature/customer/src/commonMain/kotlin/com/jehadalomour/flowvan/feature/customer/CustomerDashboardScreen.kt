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
import com.jehadalomour.flowvan.core.common.format.formatAsOf
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.resources.StringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.aspectRatio

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
    onOpenTxnReport: (String) -> Unit = {},
    onOpenDetailedTxnReport: (String) -> Unit = {},
    viewModel: CustomerDashboardViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()
    var startedTxn by remember { mutableStateOf(false) }
    LaunchedEffect(state.navigateBack) { if (state.navigateBack) onBack() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val requestLeave = { viewModel.onEvent(CustomerDashboardEvent.LeaveRequested(startedTxn)) }
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
                        .background(Fv.Blue.copy(alpha = 0.10f))
                        .clickable { onOpenAi(customerId) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_ai_sparkle),
                        contentDescription = null,
                        tint = Fv.Blue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ── Content: identity, balance, then everything you can do ────────────
        LazyColumn(
            modifier = Modifier.weight(1f).background(Fv.BgDeepest),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { IdentityBlock(state) }
            item { BalanceStrip(state) }


            // Why the four transaction tiles are dead, said once and in place of
            // them — a disabled tile with no explanation reads as a broken app.
            val block = when (state.proximityBlock) {
                ProximityBlock.NO_GPS -> Res.string.proximity_blocked_gps
                ProximityBlock.TOO_FAR -> Res.string.proximity_blocked_far
                ProximityBlock.NONE -> null
            }
            if (!state.actionsEnabled && block != null) {
                item { ProximityNotice(stringResource(block)) }
            }
            item { CustomerFigures(state) }
            item{Spacer(Modifier.height(12.dp))}

            item {
                CustomerGrid(
                    actionsEnabled = state.actionsEnabled,
                    onSale = { startedTxn = true; onOpenSale(customerId) },
                    onReturn = { startedTxn = true; onOpenReturn(customerId) },
                    onRequest = { startedTxn = true; onOpenRequest(customerId) },
                    onCollection = { startedTxn = true; onOpenCollection(customerId) },
                    onStatement = { onOpenAccountStatement(customerId) },
                    onTxnReport = { onOpenTxnReport(customerId) },
                    onDetailedReport = { onOpenDetailedTxnReport(customerId) },
                    onVoucherReport = { onOpenVoucherReport(customerId) },
                    onPaymentReport = { onOpenPaymentReport(customerId) },
                )
            }

        }
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






/** التقرير المفصل للحركات — every voucher in a period with its item lines. */

// ── Redesigned customer page ─────────────────────────────────────────────────
// Actions before information, like the dashboard. The old page led with a hero
// card and an account summary and put the four transaction buttons in a bottom
// bar, with the reports as full-width cards below — so opening a statement meant
// scrolling. Now: who they are, what they owe, then a 3×3 grid of everything
// this screen can do, and the totals underneath.

@Composable
private fun IdentityBlock(state: CustomerDashboardState) {
    val c = state.customer
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            c?.nameAr ?: "…",
            color = Fv.TextHigh,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            listOfNotNull(c?.code?.takeIf { it.isNotBlank() }, c?.area?.takeIf { it.isNotBlank() })
                .joinToString(" · "),
            color = Fv.TextMid,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/**
 * The one large figure on the screen.
 *
 * A rep's next move is decided by this number — sell, collect, or walk — so it
 * gets the only 24sp on the page. Amber rather than red once the balance passes
 * the credit limit, because that is a different instruction: not "they owe", but
 * "do not sell to them on account".
 */
@Composable
private fun BalanceStrip(state: CustomerDashboardState) {
    val c = state.customer
    val balance = c?.balance ?: 0.0
    val overLimit = c != null && c.creditLimit > 0 && balance > c.creditLimit
    val accent = when {
        overLimit -> Fv.Amber
        balance > 0 -> Fv.Red
        else -> Fv.Green
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Fv.Surface,
        border = BorderStroke(1.dp, if (overLimit) Fv.Amber else Fv.Border),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.customer_balance_label),
                        color = Fv.TextMid,
                        fontSize = 11.sp,
                    )
                    if (overLimit) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(Res.string.customer_over_limit),
                            color = Fv.Amber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    balance.formatJod(AppLanguage.AR),
                    color = accent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            // The ERP's own figure, right from the book of record — live when online,
            // last-known (dated) when offline.
            ErpBalanceLine(
                balance = state.erpBalance,
                available = state.erpAvailable,
                asOfMillis = state.erpAsOfMillis,
            )
        }
    }
}

/** A subtle second line under the local balance carrying the ERP's own figure. */
@Composable
private fun ErpBalanceLine(balance: Double?, available: Boolean, asOfMillis: Long) {
    // Nothing cached yet and nothing fetched → don't draw an empty row.
    if (!available && asOfMillis == 0L) return
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.erp_balance_label),
                color = Fv.TextMid,
                fontSize = 11.sp,
            )
            if (asOfMillis > 0L) {
                Text(
                    stringResource(Res.string.erp_as_of, formatAsOf(asOfMillis)),
                    color = Fv.TextLow,
                    fontSize = 10.sp,
                )
            }
        }
        Text(
            if (available && balance != null) {
                balance.formatJod(AppLanguage.AR)
            } else {
                stringResource(Res.string.erp_unavailable)
            },
            color = if (available) Fv.Blue else Fv.TextLow,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProximityNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Fv.Amber.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Fv.Amber),
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            color = Fv.Amber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Nine tiles, 3×3.
 *
 * The first four are the transaction actions and carry the proximity lock: a rep
 * restricted to the customer's location cannot sell from the next street, so
 * those tiles go flat and unclickable while the rest of the page stays usable.
 * Reports are never gated — reading an account is not acting on it.
 */
@Composable
private fun CustomerGrid(
    actionsEnabled: Boolean,
    onSale: () -> Unit,
    onReturn: () -> Unit,
    onRequest: () -> Unit,
    onCollection: () -> Unit,
    onStatement: () -> Unit,
    onTxnReport: () -> Unit,
    onDetailedReport: () -> Unit,
    onVoucherReport: () -> Unit,
    onPaymentReport: () -> Unit,
) {
    data class Tile(
        val icon: DrawableResource,
        val label: StringResource,
        val tint: Color,
        val onClick: () -> Unit,
        val gated: Boolean = false,
    )
    val tiles = listOf(
        Tile(Res.drawable.ic_cart, Res.string.action_sale, Fv.Green, onSale, gated = true),
        Tile(Res.drawable.ic_return_arrow, Res.string.action_return, Fv.Red, onReturn, gated = true),
        Tile(Res.drawable.ic_inventory, Res.string.action_request, Fv.Teal, onRequest, gated = true),
        Tile(Res.drawable.ic_payment, Res.string.action_collection, Fv.Green, onCollection, gated = true),
        Tile(Res.drawable.ic_bar_chart, Res.string.statement_title, Fv.Blue, onStatement),
        Tile(Res.drawable.ic_receipt, Res.string.txn_report_title, Fv.Blue, onTxnReport),
        Tile(Res.drawable.ic_receipt, Res.string.detailed_txn_title, Fv.Blue, onDetailedReport),
        Tile(Res.drawable.ic_receipt, Res.string.customer_report_vouchers, Fv.TextHigh, onVoucherReport),
        Tile(Res.drawable.ic_payment, Res.string.customer_payments_report, Fv.TextHigh, onPaymentReport),
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    CustTile(
                        iconRes = t.icon,
                        labelRes = t.label,
                        tint = t.tint,
                        enabled = !t.gated || actionsEnabled,
                        onClick = t.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CustTile(
    iconRes: DrawableResource,
    labelRes: StringResource,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
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
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) tint else Fv.TextLow,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(labelRes),
                color = if (enabled) Fv.TextHigh else Fv.TextLow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp,
            )
        }
    }
}

/** The totals, as a readout below the grid — not cards, not a hero. */
@Composable
private fun CustomerFigures(state: CustomerDashboardState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.customer_totals_label),
            color = Fv.TextMid,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Fv.Surface,
            border = BorderStroke(1.dp, Fv.Border),
        ) {
            Column {
                Row {
                    FigCell(stringResource(Res.string.all_sales_total_sales), state.salesTotal.formatJod(AppLanguage.AR), Fv.TextHigh, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(58.dp).background(Fv.Border))
                    FigCell(stringResource(Res.string.all_sales_total_returns), state.returnsTotal.formatJod(AppLanguage.AR), Fv.Red, Modifier.weight(1f))
                }
                HorizontalDivider(color = Fv.Border)
                Row {
                    FigCell(stringResource(Res.string.txn_report_total_collections), state.collectionsTotal.formatJod(AppLanguage.AR), Fv.Green, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(58.dp).background(Fv.Border))
                    FigCell(stringResource(Res.string.customer_vouchers_count), state.sales.size.toString(), Fv.TextHigh, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FigCell(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, color = Fv.TextMid, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

package com.jehadalomour.flowvan.feature.customer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.common.format.formatAmount
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.model.ledger.StatementDocType
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * كشف الحساب — a balance document, not a second transaction list.
 *
 * تقرير الحركات already lists movement over a period. This screen exists to
 * answer *how much is owed right now, and how it got there*, so it is built
 * around three figures the other report does not carry: the balance before the
 * period, the balance after every single line, and the balance at the end.
 *
 * The rep hands this to a shopkeeper who argues with it, and the argument is
 * never about the total — so the closing balance sits above the list, and every
 * row states where the balance stood once that document landed.
 */
@Composable
fun AccountStatementScreen(
    customerId: String,
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit = {},
    onOpenReceipt: (String) -> Unit = {},
    /** Open the printable/shareable statement for the range currently on screen. */
    onPrint: (fromMillis: Long, toMillis: Long) -> Unit = { _, _ -> },
    viewModel: AccountStatementViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {

            ReportTopBar(
                title = stringResource(Res.string.statement_title),
                subtitle = state.customer?.nameAr,
                onBack = onBack,
                // Carries the range on screen, so the paper matches what the rep
                // was just looking at rather than a period recomputed elsewhere.
                onPrint = { onPrint(state.fromMillis, state.toMillis) },
                printEnabled = !state.isLoading,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DateRangeBar(
                            fromMillis = state.fromMillis,
                            toMillis = state.toMillis,
                            onRangeSelected = { from, to ->
                                viewModel.onEvent(AccountStatementEvent.DateRangeChanged(from, to))
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // The account belongs to the office. When it could not be reached
                // the figures below are only this device's own vouchers, and a rep
                // about to hand the paper over has to know that before they do —
                // the shopkeeper will name the invoice that is missing.
                if (state.isLocalOnly && !state.isLoading) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            FvNotice(
                                title = stringResource(Res.string.statement_local_only_title),
                                body = stringResource(Res.string.statement_local_only_body),
                                tone = FvTone.Warning,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                // Above the list, not below it: this block is the reason the
                // screen is opened, and a rep should never scroll to reach it.
                item {
                    val inCredit = state.closingBalance < 0
                    ReportTotals(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        figures = listOf(
                            ReportFigure(
                                stringResource(Res.string.statement_opening_balance),
                                state.openingBalance.formatJod(AppLanguage.AR),
                                Fv.TextHigh,
                            ),
                            ReportFigure(
                                stringResource(Res.string.statement_debits),
                                state.totalDebits.formatJod(AppLanguage.AR),
                                Fv.Red,
                            ),
                            ReportFigure(
                                stringResource(Res.string.statement_credits),
                                state.totalCredits.formatJod(AppLanguage.AR),
                                Fv.Green,
                            ),
                            ReportFigure(
                                // A credit balance shown in the debt colour is read
                                // as debt at a glance, so both the label and the
                                // colour change when the customer is ahead.
                                label = if (inCredit) {
                                    stringResource(Res.string.statement_in_credit)
                                } else {
                                    stringResource(Res.string.statement_closing_balance)
                                },
                                value = state.closingBalance.formatJod(AppLanguage.AR),
                                accent = if (inCredit) Fv.Green else Fv.Red,
                                emphasis = true,
                            ),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    state.isLoading -> item {
                        Box(Modifier.fillMaxWidth().height(160.dp)) { ReportLoading() }
                    }

                    // The balance block stays visible above this: an empty month
                    // does not mean a zero balance, and hiding it here would say
                    // it did.
                    state.lines.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(140.dp)) {
                            ReportEmpty(stringResource(Res.string.statement_period_empty))
                        }
                    }

                    else -> items(state.lines, key = { it.key }) { line ->
                        StatementRow(
                            line = line,
                            // A server row names a voucher this handset never created,
                            // so there is nothing local to open — the row stays inert
                            // rather than opening a blank document.
                            onClick = if (!line.entry.isLocal) null else ({
                                val entry = line.entry
                                if (entry.docType == StatementDocType.PAYMENT) {
                                    onOpenReceipt(entry.id)
                                } else {
                                    onOpenInvoice(entry.id)
                                }
                            }),
                        )
                    }
                }
            }

            // A full-width bar rather than a floating circle: it never covers a
            // row, and printing is what this screen is opened to end in.
            Surface(
                onClick = { onPrint(state.fromMillis, state.toMillis) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (state.isLoading) Fv.Border else Fv.Blue,
            ) {
                Text(
                    stringResource(Res.string.statement_print_action),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                    color = if (state.isLoading) Fv.TextLow else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatementRow(line: StatementLine, onClick: (() -> Unit)?) {
    val entry = line.entry
    val badge: String
    val color: Color
    var detail: String? = null

    // No order branch: an order is not on a statement — see CustomerStatement.
    when (entry.docType) {
        StatementDocType.SALE -> {
            badge = stringResource(Res.string.voucher_type_sale); color = Fv.Red
        }

        StatementDocType.RETURN -> {
            badge = stringResource(Res.string.voucher_type_return); color = Fv.Green
        }

        StatementDocType.PAYMENT -> {
            badge = stringResource(Res.string.receipt_voucher_title)
            color = Fv.Blue
            val method = when (entry.method) {
                "CASH" -> stringResource(Res.string.method_cash_label)
                "CHEQUE" -> stringResource(Res.string.method_cheque_label)
                "TRANSFER" -> stringResource(Res.string.method_transfer_label)
                else -> entry.method
            }
            // A post-dated cheque is already counted in the balance — the app
            // posts collections on receipt — but the rep still needs to know the
            // money is not in hand yet, so the due date rides on the row.
            val due = entry.chequeDate?.takeIf { entry.method == "CHEQUE" }
                ?.let { stringResource(Res.string.statement_cheque_due, it.toDateString()) }
            detail = listOfNotNull(method, due).joinToString(" · ")
        }
    }

    val subtitle = listOfNotNull(entry.createdAt.toDateTimeString(), detail).joinToString(" · ")
    // Signed, not just coloured: colour is the first thing daylight takes, and a
    // payment read as a charge is the expensive mistake on this screen.
    val signed = entry.movement

    ReportRow(
        title = entry.number,
        subtitle = subtitle,
        value = signed.formatJod(AppLanguage.AR),
        valueCaption = stringResource(Res.string.statement_col_balance) +
            " " + line.balanceAfter.formatAmount(),
        edgeColor = color,
        valueColor = color,
        badge = badge,
        badgeColor = color,
        onClick = onClick,
    )
}

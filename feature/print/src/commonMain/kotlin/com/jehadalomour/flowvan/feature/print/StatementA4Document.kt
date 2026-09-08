package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.method_cash_label
import com.jehadalomour.flowvan.core.designsystem.resources.method_cheque_label
import com.jehadalomour.flowvan.core.designsystem.resources.method_transfer_label
import com.jehadalomour.flowvan.core.designsystem.resources.print_customer_tax_number
import com.jehadalomour.flowvan.core.designsystem.resources.print_footer_thanks
import com.jehadalomour.flowvan.core.designsystem.resources.print_voucher_type_return
import com.jehadalomour.flowvan.core.designsystem.resources.print_voucher_type_sale
import com.jehadalomour.flowvan.core.designsystem.resources.statement_closing_balance
import com.jehadalomour.flowvan.core.designsystem.resources.statement_col_balance
import com.jehadalomour.flowvan.core.designsystem.resources.statement_col_credit
import com.jehadalomour.flowvan.core.designsystem.resources.statement_col_date
import com.jehadalomour.flowvan.core.designsystem.resources.statement_col_debit
import com.jehadalomour.flowvan.core.designsystem.resources.statement_col_doc
import com.jehadalomour.flowvan.core.designsystem.resources.statement_col_doc_no
import com.jehadalomour.flowvan.core.designsystem.resources.statement_credits
import com.jehadalomour.flowvan.core.designsystem.resources.statement_customer
import com.jehadalomour.flowvan.core.designsystem.resources.statement_customer_code
import com.jehadalomour.flowvan.core.designsystem.resources.statement_debits
import com.jehadalomour.flowvan.core.designsystem.resources.statement_empty
import com.jehadalomour.flowvan.core.designsystem.resources.statement_in_credit
import com.jehadalomour.flowvan.core.designsystem.resources.statement_local_only_body
import com.jehadalomour.flowvan.core.designsystem.resources.statement_local_only_title
import com.jehadalomour.flowvan.core.designsystem.resources.statement_opening_balance
import com.jehadalomour.flowvan.core.designsystem.resources.statement_page_of
import com.jehadalomour.flowvan.core.designsystem.resources.statement_payment
import com.jehadalomour.flowvan.core.designsystem.resources.statement_period
import com.jehadalomour.flowvan.core.designsystem.resources.statement_phone
import com.jehadalomour.flowvan.core.designsystem.resources.statement_printed_at
import com.jehadalomour.flowvan.core.designsystem.resources.statement_salesman
import com.jehadalomour.flowvan.core.designsystem.resources.statement_sign_customer
import com.jehadalomour.flowvan.core.designsystem.resources.statement_sign_salesman
import com.jehadalomour.flowvan.core.designsystem.resources.statement_title
import com.jehadalomour.flowvan.core.designsystem.resources.voucher_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// ── The sheet ─────────────────────────────────────────────────────────────────

/**
 * A4 at 96 dpi. Every page is EXACTLY this, never "as tall as the content" —
 * which is the difference between a document with pages and one long image the
 * PDF writer shrinks until the closing balance is too small to read.
 */
internal val A4_WIDTH = 794.dp
internal val A4_HEIGHT = 1123.dp

private val PageMargin = 36.dp

/**
 * One movement's line height, and the number the page arithmetic is built on —
 * see [planStatementPages]. Fixed rather than measured: every cell on this table
 * is a date, a short label or an amount, none of which wrap at A4 width.
 */
private val RowHeight = 26.dp

private val Ink = Color(0xFF111111)
private val Muted = Color(0xFF5F5F5F)
private val Faint = Color(0xFF949494)
private val Hair = Color(0xFFDDDDDD)
private val HeadBg = Color(0xFFEFEFEF)
/** Every other row, so an eye tracking one line across six columns does not slip. */
private val ZebraBg = Color(0xFFFAFAFA)
private val DebitInk = Color(0xFFB3261E)
private val CreditInk = Color(0xFF1B5E20)
private val WarnBg = Color(0xFFFFF4E5)
private val WarnInk = Color(0xFF8A5300)

/** Latin digits, left-to-right, inside an otherwise right-to-left page. */
private val LtrNum = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))

/**
 * The customer account statement as A4 paper.
 *
 * Same figures as the thermal roll and the same order — oldest first, carrying a
 * running balance — because the two must never disagree about what a shop owes.
 * What changes is the shape. A roll is 80mm of continuous paper read once and
 * thrown away; this is the sheet that gets emailed to an office, filed, and
 * produced three months later in a disagreement about a payment. So it is laid
 * out to be read cold by someone who was not there:
 *
 *  - **It has real pages.** A long account becomes several A4 sheets rather than
 *    one squeezed onto a single page, which is what makes a year of movement
 *    illegible. [planStatementPages] does the cutting.
 *  - **Every sheet identifies itself** — customer, period and "page n of m" — so
 *    a set that gets separated can be put back together, and a missing sheet is
 *    visible rather than silent.
 *  - **The closing balance never leaves the movements that produce it.** It sits
 *    on the last page under the totals, boxed, and if the rows fill that page it
 *    takes a sheet of its own rather than being crushed in.
 *
 * Rendered off-screen and captured page by page; see StatementPrintScreen.
 */
@Composable
internal fun StatementA4Page(
    state: StatementPrintState,
    plan: StatementPagePlan,
    pageNumber: Int,
    pageCount: Int,
) {
    // Arabic document: the page is right-to-left, and only the numeric cells are
    // pinned the other way so amounts read as amounts.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .requiredWidth(A4_WIDTH)
                .requiredHeight(A4_HEIGHT)
                .background(Color.White)
                .padding(PageMargin),
        ) {
            if (plan.showsHeader) FullMasthead(state) else ContinuationHeader(state)

            Spacer(Modifier.height(10.dp))
            ColumnHeadings()

            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(Res.string.statement_empty),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Muted,
                )
            } else {
                state.rows
                    .subList(plan.firstRow, plan.lastRowExclusive)
                    .forEachIndexed { i, row ->
                        // Striping follows the row's place in the WHOLE statement,
                        // so the banding does not restart at every page break.
                        MovementRow(row, striped = (plan.firstRow + i) % 2 == 1)
                    }
            }

            // Pushes the closing block to the foot of the sheet, and on a page that
            // has none, holds the footer down where a reader expects it.
            Spacer(Modifier.weight(1f))

            if (plan.showsTrailer) ClosingBlock(state)

            PageFooter(state, pageNumber, pageCount)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun FullMasthead(state: StatementPrintState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            val logo = remember(state.companyLogo) { decodeBase64Image(state.companyLogo) }
            if (logo != null) {
                // Never tinted: a company's uploaded mark is usually a JPEG, which
                // has no transparency, so tinting paints a solid block. Only the
                // bundled vector below is a monochrome shape drawn to be tinted.
                Image(bitmap = logo, contentDescription = null, modifier = Modifier.heightIn(max = 60.dp))
            } else {
                Image(
                    painter = painterResource(Res.drawable.voucher_logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    colorFilter = ColorFilter.tint(Ink),
                )
            }
            Spacer(Modifier.height(6.dp))
            if (state.companyNameAr.isNotBlank()) {
                Text(state.companyNameAr, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            if (state.companyNameEn.isNotBlank()) {
                Text(state.companyNameEn, fontSize = 11.sp, color = Muted)
            }
            if (state.companyTaxNumber.isNotBlank()) {
                Text(
                    "${stringResource(Res.string.print_customer_tax_number)} ${state.companyTaxNumber}",
                    fontSize = 10.sp,
                    color = Muted,
                    style = LtrNum,
                )
            }
        }
        Text(
            text = stringResource(Res.string.statement_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }

    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
    Spacer(Modifier.height(12.dp))

    // Who the statement is about, and what window it covers — side by side, because
    // both are read before anybody looks at a single row.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f)) {
            Field(stringResource(Res.string.statement_customer), state.customerNameAr)
            if (state.customerCode.isNotBlank()) {
                Field(stringResource(Res.string.statement_customer_code), state.customerCode, numeric = true)
            }
            if (state.customerPhone.isNotBlank()) {
                Field(stringResource(Res.string.statement_phone), state.customerPhone, numeric = true)
            }
        }
        Column(Modifier.weight(1f)) {
            Field(
                stringResource(Res.string.statement_period),
                "${state.fromMillis.fullDate()} — ${state.toMillis.fullDate()}",
                numeric = true,
            )
            if (state.salesmanNameAr.isNotBlank()) {
                Field(stringResource(Res.string.statement_salesman), state.salesmanNameAr)
            }
            Field(
                stringResource(Res.string.statement_printed_at),
                state.printedAt.fullDateTime(),
                numeric = true,
            )
        }
    }

    if (state.isLocalOnly) {
        // The paper outlives the moment it was printed in. A sheet that was built
        // from one handset's ledger can be short of another van's invoices, and
        // three months later nobody can tell that from looking at it — so it says
        // so on its face.
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(WarnBg)
                .border(0.8.dp, WarnInk.copy(alpha = 0.35f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                stringResource(Res.string.statement_local_only_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WarnInk,
            )
            Text(stringResource(Res.string.statement_local_only_body), fontSize = 10.sp, color = WarnInk)
        }
    }

    Spacer(Modifier.height(12.dp))
    OpeningBalanceStrip(state)
}

/**
 * A continuation sheet's header: enough to identify the paper, and no more.
 *
 * Repeating the logo and the customer block would cost a third of every sheet
 * after the first, for information the reader already has in their hand.
 */
@Composable
private fun ContinuationHeader(state: StatementPrintState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.statement_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        Text(state.customerNameAr, fontSize = 12.sp, color = Muted)
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink))
}

@Composable
private fun OpeningBalanceStrip(state: StatementPrintState) {
    // The figure the table starts from. Without it the closing balance is only the
    // period's net movement, which is not what a shopkeeper reads it as.
    Row(
        Modifier.fillMaxWidth()
            .background(HeadBg)
            .border(0.8.dp, Hair)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.statement_opening_balance),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
        )
        Text(
            state.openingBalance.jod(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            style = LtrNum,
        )
    }
}

// ── Table ─────────────────────────────────────────────────────────────────────

/** Column widths, shared by the heading and every row so the grid cannot drift. */
private const val W_DATE = 1.0f
private const val W_TYPE = 1.1f
private const val W_DOC = 1.8f
private const val W_DEBIT = 1.2f
private const val W_CREDIT = 1.2f
private const val W_BALANCE = 1.3f

@Composable
private fun ColumnHeadings() {
    Row(Modifier.fillMaxWidth().background(HeadBg).border(0.8.dp, Hair)) {
        Head(stringResource(Res.string.statement_col_date), W_DATE, TextAlign.Center)
        Head(stringResource(Res.string.statement_col_doc), W_TYPE, TextAlign.Start)
        // A4 has the width the roll did not, so the document number gets a proper
        // column instead of a second line under each row.
        Head(stringResource(Res.string.statement_col_doc_no), W_DOC, TextAlign.Start)
        Head(stringResource(Res.string.statement_col_debit), W_DEBIT, TextAlign.Center)
        Head(stringResource(Res.string.statement_col_credit), W_CREDIT, TextAlign.Center)
        Head(stringResource(Res.string.statement_col_balance), W_BALANCE, TextAlign.Center)
    }
}

@Composable
private fun MovementRow(row: StatementRow, striped: Boolean) {
    // No ORDER label: an order moves no receivable, so it is not on a statement.
    // See CustomerStatement — the same rule the screen and the roll both apply.
    val label = when (row.docType) {
        "SALE" -> stringResource(Res.string.print_voucher_type_sale)
        "RETURN" -> stringResource(Res.string.print_voucher_type_return)
        "PAYMENT" -> when (row.method) {
            "CASH" -> stringResource(Res.string.method_cash_label)
            "CHEQUE" -> stringResource(Res.string.method_cheque_label)
            "TRANSFER" -> stringResource(Res.string.method_transfer_label)
            else -> stringResource(Res.string.statement_payment)
        }
        else -> row.docType
    }
    Row(
        Modifier.fillMaxWidth()
            .requiredHeight(RowHeight)
            .background(if (striped) ZebraBg else Color.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell(row.createdAt.fullDate(), W_DATE, TextAlign.Center, numeric = true)
        Cell(label, W_TYPE, TextAlign.Start)
        Cell(row.number, W_DOC, TextAlign.Start, numeric = true)
        // A dash, not a zero: an empty side of the ledger is not a nil amount, and
        // a column of 0.000 is far harder to scan past than a column of dashes.
        Cell(
            if (row.debit > 0) row.debit.jod() else "—",
            W_DEBIT,
            TextAlign.Center,
            numeric = row.debit > 0,
            color = if (row.debit > 0) DebitInk else Faint,
        )
        Cell(
            if (row.credit > 0) row.credit.jod() else "—",
            W_CREDIT,
            TextAlign.Center,
            numeric = row.credit > 0,
            color = if (row.credit > 0) CreditInk else Faint,
        )
        Cell(row.balance.jod(), W_BALANCE, TextAlign.Center, numeric = true, bold = true)
    }
    Box(Modifier.fillMaxWidth().height(0.8.dp).background(Hair))
}

// ── Closing block ─────────────────────────────────────────────────────────────

@Composable
private fun ClosingBlock(state: StatementPrintState) {
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        Column(Modifier.weight(1.1f)) {
            SummaryLine(stringResource(Res.string.statement_debits), state.totalDebits.jod(), DebitInk)
            SummaryLine(stringResource(Res.string.statement_credits), state.totalCredits.jod(), CreditInk)
        }
    }

    Spacer(Modifier.height(8.dp))
    // The number the whole document exists to state, inverted so the eye lands on
    // it before anything else on the sheet.
    Row(
        Modifier.fillMaxWidth().background(Ink).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.statement_closing_balance),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            state.closingBalance.jod(),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            style = LtrNum,
        )
    }

    // A negative closing balance means the shop is in credit — rare, and precisely
    // the case a reader will otherwise take for a debt.
    if (state.closingBalance < 0) {
        Spacer(Modifier.height(5.dp))
        Text(
            stringResource(Res.string.statement_in_credit),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }

    Spacer(Modifier.height(30.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        SignatureLine(stringResource(Res.string.statement_sign_customer), Modifier.weight(1f))
        SignatureLine(stringResource(Res.string.statement_sign_salesman), Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    Text(
        stringResource(Res.string.print_footer_thanks),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        color = Muted,
    )
    Spacer(Modifier.height(10.dp))
}

/**
 * The identifying strip at the foot of EVERY sheet.
 *
 * Loose A4 gets separated, and a set with no page numbers cannot be checked for a
 * missing sheet — which on a statement is the difference between an account and
 * an argument.
 */
@Composable
private fun PageFooter(state: StatementPrintState, pageNumber: Int, pageCount: Int) {
    Box(Modifier.fillMaxWidth().height(0.8.dp).background(Hair))
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "${state.customerNameAr} · ${state.fromMillis.fullDate()} — ${state.toMillis.fullDate()}",
            fontSize = 9.sp,
            color = Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.statement_page_of, pageNumber, pageCount),
            fontSize = 9.sp,
            color = Faint,
            style = LtrNum,
        )
    }
}

// ── Small parts ───────────────────────────────────────────────────────────────

@Composable
private fun RowScope.Head(text: String, weight: Float, align: TextAlign) {
    Box(Modifier.weight(weight).padding(horizontal = 6.dp, vertical = 7.dp)) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = align,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RowScope.Cell(
    text: String,
    weight: Float,
    align: TextAlign,
    numeric: Boolean = false,
    bold: Boolean = false,
    color: Color = Ink,
) {
    Box(Modifier.weight(weight).padding(horizontal = 6.dp)) {
        Text(
            text,
            fontSize = 11.sp,
            color = color,
            textAlign = align,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (numeric) LtrNum else TextStyle.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Field(label: String, value: String, numeric: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = Muted, modifier = Modifier.weight(0.9f))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            style = if (numeric) LtrNum else TextStyle.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.6f),
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = Muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, style = LtrNum)
    }
}

@Composable
private fun SignatureLine(label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink))
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

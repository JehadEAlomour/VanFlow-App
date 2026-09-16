package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

// ── Paper kit for the transaction report ─────────────────────────────────────
// Same values as StatementPrintScreen on purpose. Kept as a separate set rather
// than shared with it because these two papers are allowed to diverge later —
// what must not happen is one drifting by accident while nobody is comparing.

// Three screens draw their paper through this kit — the transaction report, the
// sales report and the daily cash flow — so everything here that ends up on
// paper is written for a thermal head rather than for a screen: one bit per dot,
// black or white, and no grey to carry hierarchy with, which leaves size, weight
// and rules to carry it. See ThermalInk for where the numbers come from. The
// handful of coloured constants below are screen chrome and stay as they are.

/** Paper ink. Two colours, and they are the only two a head can print. */
internal val TxnPaperBg  = ThermalInk.Paper
internal val TxnInk      = ThermalInk.Ink

/**
 * Secondary text on the printer status line. Screen chrome, not paper — its
 * three call sites all sit in the action bar — so it keeps its slate grey.
 */
internal val TxnSubText  = Color(0xFF637181)

/**
 * The torn edge drawn above and below the paper PREVIEW. It is decoration on
 * screen and is deliberately not inside the capture, so it may stay grey — a
 * torn edge rendered in black would print as two solid bars if it ever were.
 */
internal val TxnTearGray = Color(0xFFD1D5DB)

/** Screen chrome only — none of these ever touch paper. */
internal val TxnScreenBg = Color(0xFFD1D5DB)
internal val TxnDarkBlue = Color(0xFF1A2A3A)
internal val TxnBlue     = Color(0xFF185FA5)
internal val TxnGreen    = Color(0xFF1D9E75)
internal val TxnAmber    = Color(0xFFC97B1A)

/** Latin digits, LTR — so figures never shape as ٠١٢ under the Arabic locale. */
internal val TxnLtr = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))

/**
 * The dp width this paper's layout is designed against, and the number
 * `ThermalCapture(paperDp = …)` has to be given: the capture pins its density so
 * that this many dp comes out as exactly one head's worth of dots, and handing
 * it a different width would rescale every column on the page.
 */
internal val TxnPaperWidth = 384.dp

/**
 * A customer's uploaded logo is a continuous-tone photograph, and it was drawn
 * 300dp wide on a 384dp roll — four fifths of the page handed to a head that can
 * only answer it by scattering dots. Capped so the photograph is a mark at the
 * top of the receipt instead of the receipt.
 */
internal val TxnLogoSize = 110.dp

/** The lightest weight allowed on paper: under Bold the stems fall below a dot. */
internal val TxnWeight = FontWeight.Bold

// Type sizes come from ThermalInk rather than from taste. They read large for a
// screen and are meant to: 80mm of roll is about 384dp, so the old 12sp sub-row
// landed at roughly 2.5mm of letter height — not enough dots to keep the marks
// that tell one Arabic letter from another. The names stay because all three
// screens type them.
internal const val TXN_FS_COMPANY = ThermalInk.FS_COMPANY
internal const val TXN_FS_SUB = ThermalInk.FS_SUB
internal const val TXN_FS_TITLE = ThermalInk.FS_TITLE

/**
 * A section heading inside the body. Kept separate from [TXN_FS_HEAD], which
 * sizes the column headings of a table: a heading has to be visibly larger than
 * the rows under it, while a column heading has to fit inside a column.
 */
internal const val TXN_FS_SECTION = ThermalInk.FS_SECTION
internal const val TXN_FS_INFO = ThermalInk.FS_ROW
internal const val TXN_FS_TOTAL_LABEL = ThermalInk.FS_ROW
internal const val TXN_FS_TOTAL_VALUE = ThermalInk.FS_TOTAL
internal const val TXN_FS_HEAD = ThermalInk.FS_HEAD
internal const val TXN_FS_ROW = ThermalInk.FS_ROW
internal const val TXN_FS_SUB_ROW = ThermalInk.FS_MIN
internal const val TXN_FS_BOX_LABEL = ThermalInk.FS_SECTION

/** Two points over a total row: it is the figure the paper is read for. */
internal const val TXN_FS_BOX_VALUE = ThermalInk.FS_TOTAL + 2
internal const val TXN_FS_FOOTER = ThermalInk.FS_SUB

/** Money: three decimals, Latin digits, sign dropped (the label carries meaning). */
internal fun Double.txnJod(): String {
    val v = abs(this)
    val whole = v.toLong()
    val frac = ((v - whole) * 1000).toLong().coerceIn(0, 999)
    return "$whole.${frac.toString().padStart(3, '0')}"
}

internal fun Long.txnDate(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    return "$d/$m/${dt.year}"
}

internal fun Long.txnDateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "${txnDate()} $h:$min"
}

@Composable
internal fun TxnCenter(text: String, size: Int, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = TxnInk,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.ExtraBold else TxnWeight,
        textAlign = TextAlign.Center,
    )
}

/**
 * A label and its figure. Both black — the label used to be told apart from the
 * value by being the lighter grey of the two, which is a distinction a head
 * cannot draw, so it is carried by weight now: Bold label, ExtraBold value.
 *
 * The label is weighted so that at receipt type sizes a long Arabic label wraps
 * onto a second line instead of squeezing the figure off the edge; `fill = false`
 * keeps a short label short, so the row still reads as label-then-figure.
 */
@Composable
internal fun TxnInfo(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            color = TxnInk,
            fontSize = TXN_FS_INFO.sp,
            fontWeight = TxnWeight,
        )
        Text(
            value,
            color = TxnInk,
            fontSize = TXN_FS_INFO.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TxnLtr,
        )
    }
}

@Composable
internal fun TxnTotal(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            color = TxnInk,
            fontSize = TXN_FS_TOTAL_LABEL.sp,
            fontWeight = TxnWeight,
        )
        Text(
            value,
            color = TxnInk,
            fontSize = TXN_FS_TOTAL_VALUE.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TxnLtr,
        )
    }
}

/**
 * A figure important enough to be read before anything else on the page.
 *
 * A solid black band with the text knocked out white is the one panel treatment
 * a head prints cleanly — both halves are 1-bit. Knocked-out type does need more
 * room than type on white, though: the letters are the gaps left in a fully
 * burned band, and a thin gap closes up. Hence the label at section size rather
 * than row size, and square corners — a rounded corner is a grey arc.
 */
@Composable
internal fun TxnBoxedTotal(label: String, value: String) {
    Box(modifier = Modifier.fillMaxWidth().background(TxnInk).padding(horizontal = 8.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                color = TxnPaperBg,
                fontSize = TXN_FS_BOX_LABEL.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                value,
                color = TxnPaperBg,
                fontSize = TXN_FS_BOX_VALUE.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TxnLtr,
            )
        }
    }
}

/**
 * Table cells. `TextAlign.Right`, absolutely — `End` resolves against the
 * paragraph's text direction, and a heading (RTL) would land on the opposite
 * edge from the figure below it (LTR, because of [TxnLtr]).
 */
@Composable
internal fun RowScope.TxnHead(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = TxnInk,
        fontSize = TXN_FS_HEAD.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Right,
        // Two lines, because a heading is now wider than its column may be and a
        // clipped heading loses a word outright. The header row grows taller; the
        // columns stay where they are.
        maxLines = 2,
    )
}

@Composable
internal fun RowScope.TxnCell(text: String, weight: Float, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = TxnInk,
        fontSize = TXN_FS_ROW.sp,
        fontWeight = if (bold) FontWeight.ExtraBold else TxnWeight,
        textAlign = TextAlign.Right,
        style = TxnLtr,
        // Same reason, and it matters more here: at this size a three-decimal
        // figure can outgrow the narrowest column, and a money cell that clips or
        // ellipsises has silently dropped digits. Wrapping keeps all of them.
        maxLines = 2,
    )
}

@Composable
internal fun TxnRule() {
    Box(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness).background(TxnInk))
}

/**
 * Was a half-dot hairline at 35% alpha, which is about the least printable mark
 * there is. A head has nothing thinner than a rule to offer, so this is now the
 * same rule as [TxnRule] — the name stays because the screens separate table rows
 * with it, and the hierarchy they want comes from the spacing they leave around
 * the major rules, not from a weight difference the paper cannot hold.
 */
@Composable
internal fun TxnThinRule() {
    Box(Modifier.fillMaxWidth().height(ThermalInk.RuleThickness).background(TxnInk))
}

/**
 * The torn edge drawn above and below the paper on screen.
 *
 * Decoration, and it belongs in the preview only — outside [ThermalCapture].
 * It used to fill its 6dp strip with #D1D5DB, which a head can answer only by
 * scattering dots, so it is drawn in ink now: if it ever does end up inside a
 * capture the result is a clean black sawtooth rather than a band of speckle.
 */
@Composable
internal fun TxnTear(flipped: Boolean = false) {
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val toothWidth = 10f
        val teeth = (size.width / toothWidth).toInt() + 1
        val path = Path()
        if (flipped) {
            path.moveTo(0f, 0f)
            for (i in 0 until teeth) {
                val x = i * toothWidth
                path.lineTo(x + toothWidth / 2, size.height)
                path.lineTo(x + toothWidth, 0f)
            }
            path.lineTo(size.width, 0f)
        } else {
            path.moveTo(0f, size.height)
            for (i in 0 until teeth) {
                val x = i * toothWidth
                path.lineTo(x + toothWidth / 2, 0f)
                path.lineTo(x + toothWidth, size.height)
            }
            path.lineTo(size.width, size.height)
        }
        path.close()
        drawRect(color = TxnTearGray, topLeft = Offset.Zero, size = Size(size.width, size.height))
        drawPath(path, color = TxnPaperBg)
    }
}

@Composable
internal fun TxnActionChip(label: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) TxnBlue else Color.White,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (filled) Color.White else TxnDarkBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun txnPrinterStatusLabel(state: PrinterState): String = when (state) {
    is PrinterState.Connected -> stringResource(Res.string.printer_status_connected, state.target.name)
    is PrinterState.Connecting -> stringResource(Res.string.printer_status_connecting)
    is PrinterState.Error -> stringResource(Res.string.printer_status_error)
    PrinterState.Disconnected -> stringResource(Res.string.printer_status_disconnected)
}

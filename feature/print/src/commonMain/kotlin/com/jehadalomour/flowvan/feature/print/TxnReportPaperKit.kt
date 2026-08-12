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

internal val TxnPaperBg  = Color.White
internal val TxnScreenBg = Color(0xFFD1D5DB)
internal val TxnDarkBlue = Color(0xFF1A2A3A)
internal val TxnBlue     = Color(0xFF185FA5)
internal val TxnGreen    = Color(0xFF1D9E75)
internal val TxnAmber    = Color(0xFFC97B1A)
internal val TxnSubText  = Color(0xFF637181)
internal val TxnTearGray = Color(0xFFD1D5DB)
internal val TxnInk      = Color.Black

/** Latin digits, LTR — so figures never shape as ٠١٢ under the Arabic locale. */
internal val TxnLtr = TextStyle(textDirection = TextDirection.Ltr, localeList = LocaleList("en-US"))

internal val TxnPaperWidth = 384.dp
internal val TxnLogoSize = 300.dp
internal val TxnWeight = FontWeight.Bold

internal const val TXN_FS_COMPANY = 26
internal const val TXN_FS_SUB = 15
internal const val TXN_FS_TITLE = 20
internal const val TXN_FS_INFO = 15
internal const val TXN_FS_TOTAL_LABEL = 16
internal const val TXN_FS_TOTAL_VALUE = 18
internal const val TXN_FS_HEAD = 14
internal const val TXN_FS_ROW = 14
internal const val TXN_FS_SUB_ROW = 12
internal const val TXN_FS_BOX_LABEL = 15
internal const val TXN_FS_BOX_VALUE = 22
internal const val TXN_FS_FOOTER = 14

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

@Composable
internal fun TxnInfo(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TxnInk, fontSize = TXN_FS_INFO.sp, fontWeight = TxnWeight)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TxnInk, fontSize = TXN_FS_TOTAL_LABEL.sp, fontWeight = TxnWeight)
        Text(
            value,
            color = TxnInk,
            fontSize = TXN_FS_TOTAL_VALUE.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TxnLtr,
        )
    }
}

/** A figure important enough to be read before anything else on the page. */
@Composable
internal fun TxnBoxedTotal(label: String, value: String) {
    Box(modifier = Modifier.fillMaxWidth().background(TxnInk).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = TxnPaperBg, fontSize = TXN_FS_BOX_LABEL.sp, fontWeight = FontWeight.Bold)
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
        maxLines = 1,
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
        maxLines = 1,
    )
}

@Composable
internal fun TxnRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(TxnInk))
}

@Composable
internal fun TxnThinRule() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(TxnInk.copy(alpha = 0.35f)))
}

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

package com.jehadalomour.flowvan.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_back
import com.jehadalomour.flowvan.core.designsystem.resources.ic_print
import com.jehadalomour.flowvan.core.designsystem.resources.report_empty
import com.jehadalomour.flowvan.core.designsystem.resources.report_error
import com.jehadalomour.flowvan.core.designsystem.resources.report_print
import com.jehadalomour.flowvan.core.designsystem.resources.report_retry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The parts every report screen is built from.
 *
 * Eleven report screens had each grown their own top bar, their own totals card
 * and their own idea of what "empty" looks like — which is why a rep could not
 * tell "no transactions this month" from "the request failed" on most of them.
 * One kit, so that distinction is made once and inherited everywhere.
 *
 * Flat throughout: solid fills, a visible 1px border, 8px radius. No gradients,
 * no elevation except where something genuinely floats.
 */

// ── Chrome ────────────────────────────────────────────────────────────────────

/** Report top bar: title, optional customer/subtitle, optional print action. */
@Composable
fun ReportTopBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    onPrint: (() -> Unit)? = null,
    printEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painterResource(Res.drawable.ic_back),
                contentDescription = null,
                tint = Fv.TextHigh,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = Fv.TextMid, fontSize = 11.sp, maxLines = 1)
            }
        }
        if (onPrint != null) {
            IconButton(onClick = onPrint, enabled = printEnabled) {
                Icon(
                    painterResource(Res.drawable.ic_print),
                    contentDescription = stringResource(Res.string.report_print),
                    tint = if (printEnabled) Fv.Blue else Fv.TextLow,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// ── Totals ────────────────────────────────────────────────────────────────────

/** One figure inside [ReportTotals]. */
data class ReportFigure(
    val label: String,
    val value: String,
    val accent: Color = Fv.TextHigh,
    /** Emphasised figures sit below the divider at a larger size. */
    val emphasis: Boolean = false,
)

/**
 * The bordered block of figures at the top of a report.
 *
 * A block of label/value rows rather than a grid of cards: on a report the
 * numbers are read in relation to each other, and cards put a gutter between
 * things that belong in one column.
 *
 * Figures marked [ReportFigure.emphasis] are pulled below a divider and set
 * larger — the one or two numbers the report exists to state.
 */
@Composable
fun ReportTotals(figures: List<ReportFigure>, modifier: Modifier = Modifier) {
    val (emphasised, plain) = figures.partition { it.emphasis }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Fv.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, Fv.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        plain.forEach { f ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(f.label, color = Fv.TextMid, fontSize = 12.sp)
                Text(f.value, color = f.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (emphasised.isNotEmpty() && plain.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = Fv.Border)
            Spacer(Modifier.height(8.dp))
        }
        emphasised.forEach { f ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(f.label, color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(f.value, color = f.accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ── Filters ───────────────────────────────────────────────────────────────────

@Composable
fun ReportChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Fv.Blue else Fv.Surface,
        border = if (selected) null else BorderStroke(1.dp, Fv.Border),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun ReportChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

// ── Rows ──────────────────────────────────────────────────────────────────────

/**
 * A report line. Flush to the screen edges with a divider beneath, and a 4px
 * semantic bar at the START edge — the right, in RTL, where the eye lands first,
 * so colour does the scanning down a long list.
 */
@Composable
fun ReportRow(
    title: String,
    subtitle: String?,
    value: String,
    valueCaption: String? = null,
    edgeColor: Color = Fv.Border,
    valueColor: Color = Fv.TextHigh,
    badge: String? = null,
    badgeColor: Color = Fv.TextMid,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Fv.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(edgeColor))
        Spacer(Modifier.width(12.dp))

        if (badge != null) {
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Fv.TextLow, fontSize = 11.sp, maxLines = 1)
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 14.dp)) {
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            // A bare figure in a list is ambiguous. One word says which it is.
            if (!valueCaption.isNullOrBlank()) {
                Text(valueCaption, color = Fv.TextLow, fontSize = 11.sp)
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = Fv.Border)
}

// ── The three states ──────────────────────────────────────────────────────────
// Named, so that "nothing happened" and "we could not ask" stop being the same
// grey sentence. A failure that reads as emptiness is how a rep concludes a
// month had no sales.

@Composable
fun ReportLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Fv.Blue)
    }
}

@Composable
fun ReportEmpty(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Fv.TextMid, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/** Amber, bordered, and carrying a retry — deliberately unlike [ReportEmpty]. */
@Composable
fun ReportError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFDF6EA), RoundedCornerShape(8.dp))
                .border(1.dp, Fv.Amber, RoundedCornerShape(8.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                message,
                color = Fv.Amber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Surface(onClick = onRetry, shape = RoundedCornerShape(6.dp), color = Fv.Blue) {
                Text(
                    stringResource(Res.string.report_retry),
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Convenience default for screens with no message of their own. */
@Composable
fun ReportEmptyDefault(modifier: Modifier = Modifier) =
    ReportEmpty(stringResource(Res.string.report_empty), modifier)

@Composable
fun ReportErrorDefault(modifier: Modifier = Modifier, onRetry: () -> Unit) =
    ReportError(stringResource(Res.string.report_error), modifier, onRetry)

// ── Notices ───────────────────────────────────────────────────────────────────

/**
 * What a notice is saying, which decides its colour. Meaning, not decoration —
 * a rep reads the colour before the sentence.
 */
enum class FvTone { Warning, Success, Danger }

private val FvTone.line: Color
    get() = when (this) {
        FvTone.Warning -> Fv.Amber
        FvTone.Success -> Fv.Green
        FvTone.Danger -> Fv.Red
    }

private val FvTone.fill: Color
    get() = when (this) {
        FvTone.Warning -> Color(0xFFFDF6EA)
        FvTone.Success -> Color(0xFFEAF6F0)
        FvTone.Danger -> Color(0xFFFDEDED)
    }

/**
 * A bordered block that says something the rep must not scroll past: an approval
 * is needed, a save was refused, the office has decided.
 *
 * Bordered and tinted rather than a bare coloured sentence, because a sentence
 * in red is what every field validation looks like, and these are not that.
 * [busy] replaces the icon with a spinner for the states that are still moving.
 */
@Composable
fun FvNotice(
    title: String,
    tone: FvTone,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: androidx.compose.ui.graphics.painter.Painter? = null,
    busy: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tone.fill, RoundedCornerShape(8.dp))
            .border(1.dp, tone.line, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            busy -> CircularProgressIndicator(
                color = tone.line,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            icon != null -> Icon(icon, contentDescription = null, tint = tone.line, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = tone.line, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!body.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(body, color = Fv.TextMid, fontSize = 12.sp)
            }
        }
    }
}

// ── Forms ─────────────────────────────────────────────────────────────────────

/**
 * A labelled section of a form. The label sits above the block rather than
 * floating inside a field, so it stays readable once the field is filled.
 */
@Composable
fun FvSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = Fv.TextMid,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** A flat full-width action. [primary] fills; otherwise it is outlined. */
@Composable
fun FvButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    busy: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        // A busy button keeps its live colours even though it is not clickable:
        // the disabled fill is near-white, and a white spinner on it is invisible
        // — the rep would read a save in flight as a button that did nothing.
        color = when {
            !enabled && !busy -> Fv.SurfaceTop
            primary -> Fv.Blue
            else -> Fv.Surface
        },
        border = if (primary) null else BorderStroke(1.dp, Fv.Border),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = if (primary) Color.White else Fv.Blue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    label,
                    color = when {
                        !enabled -> Fv.TextLow
                        primary -> Color.White
                        else -> Fv.Blue
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Grid ──────────────────────────────────────────────────────────────────────

/**
 * One tile in a 3-column function grid — the dashboard, the customer page and
 * the reports hub all use this.
 *
 * A flat white square with a line icon and a label. No count, no description,
 * no gradient: the label is the description, and anything more turns a launcher
 * into a feed.
 */
@Composable
fun FvGridTile(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(Fv.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, Fv.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = Fv.TextHigh,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

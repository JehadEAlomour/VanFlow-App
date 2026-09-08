package com.jehadalomour.flowvan.feature.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.designsystem.components.*
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import com.jehadalomour.flowvan.core.model.SalesTarget
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TargetsScreen(
    onBack: () -> Unit,
    viewModel: TargetsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = null,
                            tint = Fv.TextHigh,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.targets_title), color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Current month ──────────────────────────────────────────────
            item {
                Text(stringResource(Res.string.targets_current), color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            item {
                val cur = state.current
                if (cur != null && cur.isWorthShowing) {
                    TargetCard(cur, highlighted = true)
                } else {
                    Text(
                        stringResource(if (state.isLoading) Res.string.targets_loading else Res.string.targets_no_target),
                        color = Fv.TextMid, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }

            // ── History ────────────────────────────────────────────────────
            val past = state.history.drop(1).filter { it.isWorthShowing }
            if (past.isNotEmpty()) {
                item {
                    Text(stringResource(Res.string.targets_history), color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(past) { t -> TargetCard(t, highlighted = false) }
            }

            state.error?.let { err ->
                item {
                    Text(err, color = Fv.Red, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

/**
 * One month, as the salesman sees it.
 *
 * They are measured on two separate things — what they SOLD and what they
 * COLLECTED — and paid at a different rate for each. Both are shown, and both
 * only if a target was actually set: a bar for a target nobody set would read as
 * total failure rather than as "nothing was asked of you here".
 *
 * The commission is broken into its parts rather than stated as one figure. A
 * salesman who can watch the month adding up does not have to take the number on
 * trust at the end of it.
 */
@Composable
private fun TargetCard(t: SalesTarget, highlighted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${t.month}/${t.year}",
                    color = Fv.TextHigh,
                    fontSize = if (highlighted) 15.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (t.commissionTotal > 0.0) {
                    Text(
                        t.commissionTotal.formatJod(AppLanguage.AR),
                        color = Fv.Green,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Goal(
                title = stringResource(Res.string.targets_sales),
                achieved = t.salesAchieved,
                target = t.salesTarget,
                progressPct = t.salesProgressPct,
            )
            Spacer(Modifier.height(10.dp))
            Goal(
                title = stringResource(Res.string.targets_collections),
                achieved = t.collectedAchieved,
                target = t.collectionTarget,
                progressPct = t.collectionProgressPct,
            )

            if (t.commissionTotal > 0.0) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Figure(stringResource(Res.string.targets_on_cash), t.commissionOnCash.formatJod(AppLanguage.AR), Modifier.weight(1f))
                    Figure(stringResource(Res.string.targets_on_credit), t.commissionOnCredit.formatJod(AppLanguage.AR), Modifier.weight(1f))
                    Figure(stringResource(Res.string.targets_on_collection), t.commissionOnCollection.formatJod(AppLanguage.AR), Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * One goal: what was done, against what was asked, if anything was asked.
 *
 * With no target there is no bar and no percentage — only the figure achieved.
 * An empty bar would say the salesman failed at something nobody set them.
 */
@Composable
private fun Goal(title: String, achieved: Double, target: Double?, progressPct: Int?) {
    val pct = progressPct ?: 0
    val barColor = if (pct >= 100) Fv.Green else Fv.Amber

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            achieved.formatJod(AppLanguage.AR),
            color = Fv.TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (target != null) {
            Text(
                " / " + target.formatJod(AppLanguage.AR),
                color = Fv.TextMid,
                fontSize = 12.sp,
            )
        }
    }

    if (target != null && progressPct != null) {
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(Fv.BgDeepest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = pct.coerceIn(0, 100) / 100f)
                    .height(8.dp).clip(RoundedCornerShape(4.dp)).background(barColor),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("$pct%", color = barColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    } else {
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(Res.string.targets_no_target_set),
            color = Fv.TextMid,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Fv.TextMid, fontSize = 10.sp)
        Text(value, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun fmt(t: SalesTarget, v: Double): String =
    if (t.isAmount) v.formatJod(AppLanguage.AR) else "${v.toInt()} ${stringResource(Res.string.targets_qty_unit)}"

/**
 * Whether a month has anything to say.
 *
 * Not just "a target was set": a salesman with no target who nonetheless sold,
 * collected, or earned commission has a month worth showing — and hiding it
 * would tell them they did nothing.
 */
private val SalesTarget.isWorthShowing: Boolean
    get() = hasTarget ||
        salesTarget != null ||
        collectionTarget != null ||
        salesAchieved > 0.0 ||
        collectedAchieved > 0.0 ||
        commissionTotal > 0.0

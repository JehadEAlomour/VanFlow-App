package com.jehadalomour.flowvan.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.shared.domain.model.CustomerSegment
import com.jehadalomour.flowvan.shared.domain.model.CustomerTier
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TierBadge(tier: CustomerTier, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tier) {
        CustomerTier.A -> Fv.Green to Fv.BgDeepest
        CustomerTier.B -> Fv.Amber to Fv.BgDeepest
        CustomerTier.C -> Fv.SurfaceTop to Fv.TextHigh
    }
    Text(
        text = stringResource(Res.string.tier_badge_label, tier.name),
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SegmentChip(segment: CustomerSegment, churnRisk: Double, modifier: Modifier = Modifier) {
    val bg = when {
        churnRisk >= 0.65 -> Color(0x33F04F4F)
        churnRisk >= 0.30 -> Color(0x33F5A41A)
        else -> Color(0x331DC97A)
    }
    val fg = when {
        churnRisk >= 0.65 -> Fv.Red
        churnRisk >= 0.30 -> Fv.Amber
        else -> Fv.Green
    }
    Text(
        text = segmentLabelAr(segment),
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun segmentLabelAr(segment: CustomerSegment): String = when (segment) {
    CustomerSegment.CHAMPIONS -> stringResource(Res.string.segment_champions)
    CustomerSegment.LOYAL     -> stringResource(Res.string.segment_loyal)
    CustomerSegment.AT_RISK   -> stringResource(Res.string.segment_at_risk)
    CustomerSegment.PROMISING -> stringResource(Res.string.segment_promising)
    CustomerSegment.DORMANT   -> stringResource(Res.string.segment_dormant)
    CustomerSegment.REGULAR   -> stringResource(Res.string.segment_regular)
}

@Composable
fun OverdueChip(amountText: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.chip_overdue_amount, amountText),
        modifier = modifier
            .background(Color(0x33F04F4F), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = Fv.Red,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun ChurnChip(risk: Double, modifier: Modifier = Modifier) {
    val pct = (risk * 100).toInt()
    Text(
        text = stringResource(Res.string.chip_churn_risk, pct),
        modifier = modifier
            .background(Color(0x33F04F4F), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = Fv.Red,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun OffRoutePill(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.chip_off_route),
        modifier = modifier
            .background(Fv.SurfaceTop, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = Fv.TextMid,
        fontSize = 11.sp,
    )
}

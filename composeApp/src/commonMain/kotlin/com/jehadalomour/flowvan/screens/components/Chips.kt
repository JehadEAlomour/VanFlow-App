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

@Composable
fun TierBadge(tier: CustomerTier, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tier) {
        CustomerTier.A -> Fv.Green to Fv.BgDeepest
        CustomerTier.B -> Fv.Amber to Fv.BgDeepest
        CustomerTier.C -> Fv.SurfaceTop to Fv.TextHigh
    }
    Text(
        text = "فئة ${tier.name}",
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

fun segmentLabelAr(segment: CustomerSegment): String = when (segment) {
    CustomerSegment.CHAMPIONS -> "أبطال"
    CustomerSegment.LOYAL     -> "مخلصون"
    CustomerSegment.AT_RISK   -> "عرضة"
    CustomerSegment.PROMISING -> "واعدون"
    CustomerSegment.DORMANT   -> "خاملون"
    CustomerSegment.REGULAR   -> "عاديون"
}

@Composable
fun OverdueChip(amountText: String, modifier: Modifier = Modifier) {
    Text(
        text = "متأخر $amountText",
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
        text = "خطر التسرب $pct%",
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
        text = "خارج المسار",
        modifier = modifier
            .background(Fv.SurfaceTop, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = Fv.TextMid,
        fontSize = 11.sp,
    )
}

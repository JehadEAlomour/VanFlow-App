package com.jehadalomour.flowvan.screens.customer

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.jehadalomour.flowvan.shared.domain.model.CustomerSegment
import com.jehadalomour.flowvan.shared.domain.model.CustomerTier
import com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardState
import com.jehadalomour.flowvan.shared.presentation.feature.customerdashboard.CustomerDashboardViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
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
    viewModel: CustomerDashboardViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

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
                        .clickable(onClick = onBack),
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
                    "بطاقة العميل",
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
        }

        // ── Bottom Action Bar ─────────────────────────────────────────────────
        BottomActionBar(
            onSale = { onOpenSale(customerId) },
            onReturn = { onOpenReturn(customerId) },
            onRequest = { onOpenRequest(customerId) },
            onCollection = { onOpenCollection(customerId) },
        )
    }
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
                    label = "الرصيد",
                    value = c.balance.formatJod(AppLanguage.AR),
                    valueColor = if (c.balance < 0) Color(0xFFFF8080) else Color.White,
                    modifier = Modifier.weight(1f),
                )
                FrostedStatBox(
                    label = "متأخر",
                    value = c.overdueAmount.formatJod(AppLanguage.AR),
                    valueColor = if (c.overdueAmount > 0) Color(0xFFFFB570) else Color.White,
                    modifier = Modifier.weight(1f),
                )
                FrostedStatBox(
                    label = "السقف",
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
        CustomerTier.A -> "فئة A" to listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))
        CustomerTier.B -> "فئة B" to listOf(Color(0xFFC97B1A), Color(0xFF9A5C10))
        CustomerTier.C -> "فئة C" to listOf(Color(0xFF637181), Color(0xFF3E4D5C))
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
        CustomerSegment.CHAMPIONS -> Color(0xFFB5F5D5) to "عميل مميز"
        CustomerSegment.LOYAL -> Color(0xFFAACAFF) to "مخلصون"
        CustomerSegment.AT_RISK -> Color(0xFFFFAAAA) to "عرضة للفقدان"
        CustomerSegment.PROMISING -> Color(0xFFFFD9A0) to "واعدون"
        CustomerSegment.DORMANT -> Color(0xFFBBCCDD) to "نائم"
        CustomerSegment.REGULAR -> Color(0xFFBBCCDD) to "عادي"
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
                Text("ملخص الحساب", color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryIconRow(
                    iconRes = Res.drawable.ic_receipt,
                    label = "عدد الفواتير",
                    value = "${state.sales.size + state.returns.size + state.requests.size}",
                    accent = Fv.Blue,
                )
                SummaryIconRow(
                    iconRes = Res.drawable.ic_cart,
                    label = "إجمالي المبيعات",
                    value = state.salesTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Green,
                )
                SummaryIconRow(
                    iconRes = Res.drawable.ic_return_arrow,
                    label = "إجمالي المرتجعات",
                    value = state.returnsTotal.formatJod(AppLanguage.AR),
                    accent = Fv.Red,
                )
                SummaryIconRow(
                    iconRes = Res.drawable.ic_payment,
                    label = "إجمالي التحصيلات",
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
                        "الرصيد الحالي",
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
            label = "تقرير الفواتير",
            value = state.salesTotal.formatJod(AppLanguage.AR),
            accent = Fv.Blue,
            modifier = Modifier.weight(1f),
            onClick = onOpenVoucherReport,
        )
        ReportCard(
            iconRes = Res.drawable.ic_payment,
            iconGradient = Brush.linearGradient(listOf(Color(0xFF0FA968), Color(0xFF0A7A4B))),
            label = "تقرير المدفوعات",
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
            Text("عرض التقرير ←", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                Text("كشف الحساب", color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("جميع الحركات والمديونية", color = Fv.TextMid, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Fv.Purple.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text("تصفية", color = Fv.Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Bottom Action Bar ─────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(
    onSale: () -> Unit,
    onReturn: () -> Unit,
    onRequest: () -> Unit,
    onCollection: () -> Unit,
) {
    Surface(color = Fv.Surface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "إجراءات سريعة",
                color = Fv.TextMid,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(
                    iconRes = Res.drawable.ic_cart,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFF0FA968), Color(0xFF0A7A4B))),
                    label = "بيع",
                    labelColor = Fv.Green,
                    modifier = Modifier.weight(1f),
                    onClick = onSale,
                )
                ActionTile(
                    iconRes = Res.drawable.ic_return_arrow,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFFD63B3B), Color(0xFF992828))),
                    label = "مرتجع",
                    labelColor = Fv.Red,
                    modifier = Modifier.weight(1f),
                    onClick = onReturn,
                )
                ActionTile(
                    iconRes = Res.drawable.ic_receipt,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFF0E9E91), Color(0xFF0A6E66))),
                    label = "طلب",
                    labelColor = Fv.Teal,
                    modifier = Modifier.weight(1f),
                    onClick = onRequest,
                )
                ActionTile(
                    iconRes = Res.drawable.ic_payment,
                    iconGradient = Brush.linearGradient(listOf(Color(0xFFB36C00), Color(0xFF7A4A00))),
                    label = "تحصيل",
                    labelColor = Fv.Amber,
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
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
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

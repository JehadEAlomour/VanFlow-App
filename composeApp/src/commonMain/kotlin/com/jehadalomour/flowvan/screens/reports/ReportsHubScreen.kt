package com.jehadalomour.flowvan.screens.reports

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv

@Composable
fun ReportsHubScreen(
    onBack: () -> Unit,
    onOpenSalesReport: () -> Unit,
    onOpenPaymentsReport: () -> Unit,
    onOpenVisitReport: () -> Unit,
    onOpenCashFlow: () -> Unit,
    onOpenItemsSales: () -> Unit,
    onOpenReceivables: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                    Spacer(Modifier.width(4.dp))
                    Text("التقارير", color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                ReportCard(
                    emoji = "🧾",
                    title = "تقرير المبيعات",
                    subtitle = "فواتير البيع والمرتجعات حسب التاريخ",
                    accent = Fv.Blue,
                    onClick = onOpenSalesReport,
                )
            }
            item {
                ReportCard(
                    emoji = "💵",
                    title = "تقرير التحصيلات",
                    subtitle = "المدفوعات المستلمة حسب التاريخ والطريقة",
                    accent = Fv.Green,
                    onClick = onOpenPaymentsReport,
                )
            }
            item {
                ReportCard(
                    emoji = "🗺️",
                    title = "تقرير الزيارات",
                    subtitle = "العملاء المزارين والمتبقين في المسار",
                    accent = Fv.Teal,
                    onClick = onOpenVisitReport,
                )
            }
            item {
                ReportCard(
                    emoji = "📊",
                    title = "الكشف اليومي",
                    subtitle = "ملخص كامل للمبيعات والتحصيلات والمرتجعات",
                    accent = Fv.Amber,
                    onClick = onOpenCashFlow,
                )
            }
            item {
                ReportCard(
                    emoji = "📦",
                    title = "مبيعات الأصناف",
                    subtitle = "إجمالي مبيعات كل صنف من تاريخ إلى تاريخ",
                    accent = Fv.Purple,
                    onClick = onOpenItemsSales,
                )
            }
            item {
                ReportCard(
                    emoji = "⚠️",
                    title = "تقرير ذمم العملاء",
                    subtitle = "العملاء الذين لديهم أرصدة مستحقة مرتبة تنازلياً",
                    accent = Fv.Red,
                    onClick = onOpenReceivables,
                )
            }
        }
    }
}

@Composable
private fun ReportCard(emoji: String, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 20.sp) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Fv.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Fv.TextMid, fontSize = 12.sp)
            }
            Text("›", color = Fv.TextMid, fontSize = 20.sp)
        }
    }
}


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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.presentation.feature.voucherreport.VoucherKindFilter
import com.jehadalomour.flowvan.shared.presentation.feature.voucherreport.VoucherReportEvent
import com.jehadalomour.flowvan.shared.presentation.feature.voucherreport.VoucherReportViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.voucherreport.VoucherTypeFilter
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VoucherReportScreen(
    customerId: String,
    onBack: () -> Unit,
    onOpenVoucher: (invoiceId: String) -> Unit,
    viewModel: VoucherReportViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                Text("تقرير الفواتير", color = Fv.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DateRangeBar(
                        fromMillis = state.fromMillis,
                        toMillis = state.toMillis,
                        onRangeSelected = { from, to ->
                            viewModel.onEvent(VoucherReportEvent.DateRangeChanged(from, to))
                        },
                    )
                }

                item {
                    FilterChipRow(
                        filters = VoucherTypeFilter.entries,
                        selected = state.typeFilter,
                        label = { filter ->
                            when (filter) {
                                VoucherTypeFilter.ALL -> "الكل"
                                VoucherTypeFilter.SALE -> "مبيعات"
                                VoucherTypeFilter.RETURN -> "مرتجعات"
                                VoucherTypeFilter.REQUEST -> "طلبات"
                            }
                        },
                        onSelect = { viewModel.onEvent(VoucherReportEvent.TypeFilterChanged(it)) },
                    )
                }

                item {
                    FilterChipRow(
                        filters = VoucherKindFilter.entries,
                        selected = state.kindFilter,
                        label = { filter ->
                            when (filter) {
                                VoucherKindFilter.ALL -> "الكل"
                                VoucherKindFilter.CASH -> "نقداً"
                                VoucherKindFilter.CHEQUE -> "شيك"
                                VoucherKindFilter.TRANSFER -> "تحويل"
                                VoucherKindFilter.CREDIT -> "آجل"
                            }
                        },
                        onSelect = { viewModel.onEvent(VoucherReportEvent.KindFilterChanged(it)) },
                    )
                }

                item {
                    SummaryCard(count = state.invoices.size, total = state.total)
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = Fv.Blue) }
                    }
                } else if (state.invoices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("لا توجد فواتير في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp) }
                    }
                } else {
                    items(state.invoices, key = { it.id }) { invoice ->
                        VoucherRow(invoice = invoice, onClick = { onOpenVoucher(invoice.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(count: Int, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("عدد الفواتير", color = Fv.TextMid, fontSize = 10.sp)
                Text("$count فاتورة", color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(modifier = Modifier.width(1.dp), color = Fv.SurfaceHigh)
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("الإجمالي", color = Fv.TextMid, fontSize = 10.sp)
                Text(total.formatJod(AppLanguage.AR), color = Fv.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VoucherRow(invoice: InvoiceEntity, onClick: () -> Unit) {
    val (typeLabel, typeColor) = when (invoice.type) {
        "SALE" -> "بيع" to Fv.Green
        "RETURN" -> "مرتجع" to Fv.Red
        "REQUEST" -> "طلب" to Fv.Teal
        else -> invoice.type to Fv.TextMid
    }
    val kindLabel = when (invoice.paymentMethod) {
        "CASH" -> "نقداً"
        "CHEQUE" -> "شيك"
        "TRANSFER" -> "تحويل"
        "CREDIT" -> "آجل"
        else -> "آجل"
    }
    val kindColor = when (invoice.paymentMethod) {
        "CASH" -> Fv.Green
        "CHEQUE" -> Fv.Amber
        "TRANSFER" -> Fv.Blue
        else -> Fv.TextMid
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) { Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(kindColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) { Text(kindLabel, color = kindColor, fontSize = 10.sp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invoice.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(invoice.createdAt.toDateTimeString(), color = Fv.TextMid, fontSize = 10.sp)
            }
            Text(invoice.total.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

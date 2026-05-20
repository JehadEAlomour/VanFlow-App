package com.jehadalomour.flowvan.screens.reports

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.presentation.feature.transactionreport.TransactionReportEvent
import com.jehadalomour.flowvan.shared.presentation.feature.transactionreport.TransactionReportViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.transactionreport.TxnTypeFilter
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TransactionReportScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: TransactionReportViewModel = koinViewModel { parametersOf(customerId) },
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    "تقرير المعاملات",
                    color = Fv.TextHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DateRangeBar(
                        fromMillis = state.fromMillis,
                        toMillis = state.toMillis,
                        onRangeSelected = { from, to ->
                            viewModel.onEvent(TransactionReportEvent.DateRangeChanged(from, to))
                        },
                    )
                }

                item {
                    FilterChipRow(
                        filters = TxnTypeFilter.entries,
                        selected = state.typeFilter,
                        label = { filter ->
                            when (filter) {
                                TxnTypeFilter.ALL -> "الكل"
                                TxnTypeFilter.SALE -> "مبيعات"
                                TxnTypeFilter.RETURN -> "مرتجعات"
                                TxnTypeFilter.REQUEST -> "طلبات"
                            }
                        },
                        onSelect = { viewModel.onEvent(TransactionReportEvent.TypeFilterChanged(it)) },
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryPill(
                            label = "إجمالي المبيعات",
                            value = state.salesTotal.formatJod(AppLanguage.AR),
                            accent = Fv.Green,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryPill(
                            label = "إجمالي المرتجعات",
                            value = state.returnsTotal.formatJod(AppLanguage.AR),
                            accent = Fv.Red,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fv.Blue)
                        }
                    }
                } else if (state.invoices.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد معاملات في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(state.invoices, key = { it.id }) { invoice ->
                        TxnInvoiceRow(invoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun TxnInvoiceRow(invoice: InvoiceEntity) {
    val (typeLabel, typeColor) = when (invoice.type) {
        "SALE" -> "بيع" to Fv.Green
        "RETURN" -> "مرتجع" to Fv.Red
        "REQUEST" -> "طلب" to Fv.Teal
        else -> invoice.type to Fv.TextMid
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) { Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invoice.number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(invoice.createdAt.toDateTimeString(), color = Fv.TextMid, fontSize = 10.sp)
                invoice.paymentMethod?.let { Text(it, color = Fv.TextMid, fontSize = 10.sp) }
            }
            Text(invoice.total.formatJod(AppLanguage.AR), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

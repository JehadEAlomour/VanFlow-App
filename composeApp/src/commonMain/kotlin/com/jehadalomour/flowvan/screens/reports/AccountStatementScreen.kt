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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.jehadalomour.flowvan.shared.presentation.feature.accountstatement.AccountStatementEvent
import com.jehadalomour.flowvan.shared.presentation.feature.accountstatement.AccountStatementViewModel
import com.jehadalomour.flowvan.shared.presentation.feature.accountstatement.StatementEntry
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AccountStatementScreen(
    customerId: String,
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit = {},
    onOpenReceipt: (String) -> Unit = {},
    viewModel: AccountStatementViewModel = koinViewModel { parametersOf(customerId) },
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
                Column {
                    Text(
                        "كشف الحساب",
                        color = Fv.TextHigh,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.customer?.nameAr?.let {
                        Text(it, color = Fv.TextMid, fontSize = 11.sp)
                    }
                }
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
                            viewModel.onEvent(AccountStatementEvent.DateRangeChanged(from, to))
                        },
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ملخص الحساب", color = Fv.TextMid, fontSize = 11.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryPill(
                                    label = "إجمالي المديونية",
                                    value = state.totalDebits.formatJod(AppLanguage.AR),
                                    accent = Fv.Red,
                                    modifier = Modifier.weight(1f),
                                )
                                SummaryPill(
                                    label = "إجمالي السداد",
                                    value = state.totalCredits.formatJod(AppLanguage.AR),
                                    accent = Fv.Green,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Fv.SurfaceHigh)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("صافي الرصيد", color = Fv.TextMid, fontSize = 12.sp)
                                Text(
                                    state.net.formatJod(AppLanguage.AR),
                                    color = if (state.net > 0) Fv.Red else Fv.Green,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fv.Blue)
                        }
                    }
                } else if (state.entries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد حركات في هذه الفترة", color = Fv.TextMid, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(state.entries, key = {
                        when (it) {
                            is StatementEntry.Invoice -> "inv-${it.entity.id}"
                            is StatementEntry.Payment -> "pay-${it.entity.id}"
                        }
                    }) { entry ->
                        StatementEntryRow(
                            entry = entry,
                            onClick = {
                                when (entry) {
                                    is StatementEntry.Invoice -> onOpenInvoice(entry.entity.id)
                                    is StatementEntry.Payment -> onOpenReceipt(entry.entity.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementEntryRow(entry: StatementEntry, onClick: () -> Unit) {
    when (entry) {
        is StatementEntry.Invoice -> {
            val (typeLabel, typeColor) = when (entry.entity.type) {
                "SALE" -> "بيع" to Fv.Red
                "RETURN" -> "مرتجع" to Fv.Green
                "REQUEST" -> "طلب" to Fv.Teal
                else -> entry.entity.type to Fv.TextMid
            }
            StatementRow(
                badge = typeLabel,
                badgeColor = typeColor,
                number = entry.entity.number,
                date = entry.entity.createdAt.toDateTimeString(),
                amount = entry.entity.total.formatJod(AppLanguage.AR),
                amountColor = typeColor,
                side = if (entry.entity.type == "RETURN") "دائن" else "مدين",
                onClick = onClick,
            )
        }
        is StatementEntry.Payment -> {
            val methodLabel = when (entry.entity.method) {
                "CASH" -> "نقداً"
                "CHEQUE" -> "شيك"
                "TRANSFER" -> "تحويل"
                else -> entry.entity.method
            }
            StatementRow(
                badge = methodLabel,
                badgeColor = Fv.Blue,
                number = entry.entity.number,
                date = entry.entity.createdAt.toDateTimeString(),
                amount = entry.entity.amount.formatJod(AppLanguage.AR),
                amountColor = Fv.Green,
                side = "دائن",
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun StatementRow(
    badge: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    number: String,
    date: String,
    amount: String,
    amountColor: androidx.compose.ui.graphics.Color,
    side: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) { Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(number, color = Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(date, color = Fv.TextMid, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = amountColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(side, color = Fv.TextMid, fontSize = 10.sp)
            }
        }
    }
}

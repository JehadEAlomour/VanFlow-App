package com.jehadalomour.flowvan.screens.reports

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import flowvan.composeapp.generated.resources.Res
import flowvan.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.presentation.feature.reports.ReceivablesReportViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReceivablesReportScreen(
    onBack: () -> Unit,
    viewModel: ReceivablesReportViewModel = koinViewModel(),
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
                    Text(stringResource(Res.string.receivables_title), color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryPill(
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.receivables_total_balance),
                        value = state.totalBalance.formatJod(AppLanguage.AR),
                        accent = Fv.Red,
                    )
                    SummaryPill(
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.receivables_total_overdue),
                        value = state.totalOverdue.formatJod(AppLanguage.AR),
                        accent = Fv.Amber,
                    )
                }
            }

            item {
                Text(
                    stringResource(Res.string.receivables_customers_count, state.count),
                    color = Fv.TextMid,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            items(state.customers) { customer ->
                ReceivableRow(customer)
            }

            if (state.customers.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.receivables_empty),
                        color = Fv.Green,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivableRow(customer: Customer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.nameAr, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                if (customer.overdueAmount > 0.0) {
                    Text(
                        stringResource(Res.string.receivables_overdue_value, customer.overdueAmount.formatJod(AppLanguage.AR)),
                        color = Fv.Red,
                        fontSize = 12.sp,
                    )
                } else {
                    Text(customer.tier.name, color = Fv.TextMid, fontSize = 12.sp)
                }
            }
            Text(
                customer.balance.formatJod(AppLanguage.AR),
                color = if (customer.overdueAmount > 0.0) Fv.Red else Fv.Amber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

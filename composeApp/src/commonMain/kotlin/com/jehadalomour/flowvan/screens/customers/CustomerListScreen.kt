package com.jehadalomour.flowvan.screens.customers

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.screens.components.Fv
import com.jehadalomour.flowvan.screens.components.OffRoutePill
import com.jehadalomour.flowvan.screens.components.SegmentChip
import com.jehadalomour.flowvan.screens.components.TierBadge
import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.CustomerTier
import com.jehadalomour.flowvan.shared.presentation.feature.customers.CustomerListEvent
import com.jehadalomour.flowvan.shared.presentation.feature.customers.CustomerListViewModel
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    viewModel: CustomerListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                    Text(
                        "قائمة العملاء",
                        color = Fv.TextHigh,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${state.visible.size}", color = Fv.TextMid, fontSize = 13.sp)
                }
            }
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(CustomerListEvent.SearchChanged(it)) },
                    placeholder = { Text("ابحث بالاسم أو الكود أو المنطقة", color = Fv.TextMid) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors(),
                )
            }
            item {
                TierFilterRow(
                    selected = state.tierFilter,
                    onSelect = { viewModel.onEvent(CustomerListEvent.TierFilter(it)) },
                )
            }
            items(state.visible, key = { it.id }) { customer ->
                CustomerListCard(
                    customer,
                    onClick = { onOpenCustomer(customer.id) },
                    onNavigate = if (customer.lat != null && customer.lng != null) {
                        { onNavigateTo(customer.id) }
                    } else null,
                )
            }
            if (state.visible.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        "لا نتائج",
                        color = Fv.TextMid,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TierFilterRow(selected: CustomerTier?, onSelect: (CustomerTier?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterPill("الكل", selected == null) { onSelect(null) } }
        items(CustomerTier.values().toList()) { tier ->
            FilterPill("فئة ${tier.name}", selected == tier) { onSelect(if (selected == tier) null else tier) }
        }
    }
}

@Composable
private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (active) Fv.Blue else Fv.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active) Fv.TextHigh else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CustomerListCard(customer: Customer, onClick: () -> Unit, onNavigate: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Fv.SurfaceTop, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    customer.nameAr.firstOrNull()?.toString() ?: "?",
                    color = Fv.TextHigh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.nameAr, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${customer.code} · ${customer.area}", color = Fv.TextMid, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SegmentChip(customer.segment, customer.churnRisk)
                    if (!customer.isOnRoute) OffRoutePill()
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                TierBadge(customer.tier)
                if (customer.balance > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        customer.balance.formatJod(AppLanguage.AR),
                        color = if (customer.overdueAmount > 0) Fv.Red else Fv.TextHigh,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (onNavigate != null) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clickable { onNavigate() }
                            .background(Fv.Blue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("🚗", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun darkFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Fv.TextHigh,
    unfocusedTextColor = Fv.TextHigh,
    focusedContainerColor = Fv.Surface,
    unfocusedContainerColor = Fv.Surface,
    focusedIndicatorColor = Fv.Blue,
    unfocusedIndicatorColor = Fv.Border,
    cursorColor = Fv.Blue,
    focusedPlaceholderColor = Fv.TextMid,
    unfocusedPlaceholderColor = Fv.TextMid,
)

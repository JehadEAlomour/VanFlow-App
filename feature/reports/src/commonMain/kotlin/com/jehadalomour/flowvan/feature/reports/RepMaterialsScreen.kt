package com.jehadalomour.flowvan.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.FvNotice
import com.jehadalomour.flowvan.core.designsystem.components.FvTone
import com.jehadalomour.flowvan.core.designsystem.components.ReportTopBar
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_inventory
import com.jehadalomour.flowvan.core.designsystem.resources.ic_truck
import com.jehadalomour.flowvan.core.designsystem.resources.ic_warning
import com.jehadalomour.flowvan.core.network.dto.WarehouseMaterialsDto
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The rep's own materials, grouped by warehouse. One card per warehouse — the
 * rep's van first with a badge — each listing the materials and quantities.
 * Read-only.
 */
@Composable
fun RepMaterialsScreen(
    onBack: () -> Unit,
    viewModel: RepMaterialsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        ReportTopBar(title = "موادي حسب المستودع", onBack = onBack)

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Fv.Blue)
            }
            state.error != null -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                FvNotice(state.error!!, FvTone.Danger, icon = painterResource(Res.drawable.ic_warning))
            }
            state.warehouses.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مواد", color = Fv.TextMid, fontSize = 13.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.warehouses, key = { it.whNumber }) { wh -> WarehouseCard(wh) }
            }
        }
    }
}

@Composable
private fun WarehouseCard(wh: WarehouseMaterialsDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Fv.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, if (wh.isRepVan) Fv.Blue else Fv.Border, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painterResource(if (wh.isRepVan) Res.drawable.ic_truck else Res.drawable.ic_inventory),
                contentDescription = null,
                tint = if (wh.isRepVan) Fv.Blue else Fv.TextMid,
                modifier = Modifier.size(18.dp),
            )
            Text(wh.whName ?: wh.whNumber, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (wh.isRepVan) {
                Text(
                    "مركبتي",
                    color = Fv.Blue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Fv.Blue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Text("${wh.itemCount} مادة", color = Fv.TextMid, fontSize = 11.sp)
        }

        wh.items.forEach { it ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(it.itemName ?: "—", color = Fv.TextHigh, fontSize = 13.sp)
                    Text(it.itemNumber, color = Fv.TextMid, fontSize = 10.sp)
                }
                Text(formatQty(it.qty), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Whole numbers without a decimal tail; fractional kept to 3 places. */
private fun formatQty(q: Double): String {
    val rounded = (q * 1000).toLong()
    return if (rounded % 1000L == 0L) (rounded / 1000L).toString()
    else q.toString()
}

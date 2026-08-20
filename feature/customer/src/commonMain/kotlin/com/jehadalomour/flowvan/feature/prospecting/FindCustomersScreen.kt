package com.jehadalomour.flowvan.feature.prospecting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.FvButton
import com.jehadalomour.flowvan.core.designsystem.components.FvNotice
import com.jehadalomour.flowvan.core.designsystem.components.FvSectionLabel
import com.jehadalomour.flowvan.core.designsystem.components.FvTone
import com.jehadalomour.flowvan.core.designsystem.components.ReportTopBar
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_customers
import com.jehadalomour.flowvan.core.designsystem.resources.ic_map
import com.jehadalomour.flowvan.core.designsystem.resources.ic_warning
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Find customers near the rep. Reads GPS, searches 2 km around it, lists the
 * shops it finds. Each row can open the in-app map to that point, or start a
 * prefilled add-customer form.
 *
 * @param onOpenMap        lat, lng, label — the raw-point map route.
 * @param onAddCustomer    name, phone, lat, lng, prospectId — the create form.
 */
@Composable
fun FindCustomersScreen(
    onBack: () -> Unit,
    onOpenMap: (lat: Double, lng: Double, label: String) -> Unit,
    onAddCustomer: (name: String, phone: String?, lat: Double?, lng: Double?, prospectId: String) -> Unit,
    viewModel: FindCustomersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        ReportTopBar(title = "البحث عن عملاء", onBack = onBack)

        // ── The search controls: chips, keyword, button. Fixed above the list. ──
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLocating -> FvNotice("جارٍ تحديد موقعك…", FvTone.Success)
                state.locationError -> FvNotice(
                    "تعذّر تحديد الموقع",
                    FvTone.Warning,
                    body = "لا يمكن البحث دون موقع. فعّل خدمة الموقع وأعد المحاولة.",
                    icon = painterResource(Res.drawable.ic_warning),
                )
            }

            if (state.featuredCategories.isNotEmpty()) {
                FvSectionLabel("نوع المكان")
                CategoryChips(
                    categories = state.featuredCategories,
                    selected = state.selectedCategories,
                    onToggle = { viewModel.onEvent(FindCustomersEvent.ToggleCategory(it)) },
                )
            }

            KeywordField(
                keywords = state.keywords,
                onAdd = { viewModel.onEvent(FindCustomersEvent.AddKeyword(it)) },
                onRemove = { viewModel.onEvent(FindCustomersEvent.RemoveKeyword(it)) },
            )

            FvButton(
                label = if (state.isSearching) "جارٍ البحث…" else "ابحث حولي (٢ كم)",
                onClick = { viewModel.onEvent(FindCustomersEvent.Search) },
                enabled = state.canSearch,
                busy = state.isSearching,
            )

            state.errorAr?.let {
                FvNotice(it, FvTone.Danger, icon = painterResource(Res.drawable.ic_warning))
            }
        }

        // ── Results ─────────────────────────────────────────────────────────────
        if (state.hasSearched && state.results.isEmpty() && !state.isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد نتائج قريبة", color = Fv.TextMid, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.results, key = { it.prospect.id }) { row ->
                    ProspectRow(
                        row = row,
                        onOpenMap = {
                            val p = row.prospect
                            if (p.latitude != null && p.longitude != null) {
                                onOpenMap(p.latitude!!, p.longitude!!, p.name)
                            }
                        },
                        onAdd = {
                            val p = row.prospect
                            onAddCustomer(p.name, p.phone, p.latitude, p.longitude, p.id)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    // A simple wrap: chips are short and few (the featured subset), so a plain
    // flowing row reads better than a horizontal scroller that hides half of them.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { key ->
            val on = key in selected
            Text(
                text = categoryLabelAr(key),
                color = if (on) Fv.Blue else Fv.TextMid,
                fontSize = 12.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable { onToggle(key) }
                    .background(
                        if (on) Fv.Blue.copy(alpha = 0.15f) else Fv.Surface,
                        RoundedCornerShape(16.dp),
                    )
                    .border(
                        1.dp,
                        if (on) Fv.Blue else Fv.Border,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordField(
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("أضف كلمة بحث (مثال: بقالة)", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (text.isNotBlank()) { onAdd(text); text = "" }
            }),
        )
        if (keywords.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                keywords.forEach { term ->
                    Text(
                        "$term  ✕",
                        color = Fv.TextHigh,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { onRemove(term) }
                            .background(Fv.SurfaceTop, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProspectRow(
    row: NearbyProspect,
    onOpenMap: () -> Unit,
    onAdd: () -> Unit,
) {
    val p = row.prospect
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Fv.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Fv.Border, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                p.address?.let { Text(it, color = Fv.TextMid, fontSize = 11.sp, maxLines = 2) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.distanceM?.let {
                        Text(formatDistance(it), color = Fv.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    p.phone?.let { Text(it, color = Fv.TextMid, fontSize = 11.sp) }
                }
            }
            // Already a customer: say so instead of offering to add it again.
            if (row.isExistingCustomer) {
                Text(
                    "عميل حالي",
                    color = Fv.TextMid,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(Fv.SurfaceTop, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (p.hasLocation) {
                RowAction(
                    label = "الموقع",
                    icon = painterResource(Res.drawable.ic_map),
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!row.isExistingCustomer) {
                RowAction(
                    label = "إضافة عميل",
                    icon = painterResource(Res.drawable.ic_customers),
                    onClick = onAdd,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RowAction(
    label: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .background(
                if (primary) Fv.Blue.copy(alpha = 0.15f) else Fv.SurfaceTop,
                RoundedCornerShape(8.dp),
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (primary) Fv.Blue else Fv.TextMid, modifier = Modifier.size(16.dp))
        Text(label, color = if (primary) Fv.Blue else Fv.TextHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDistance(m: Int): String =
    if (m < 1_000) "$m م" else "${(m / 100) / 10.0} كم"

/**
 * Arabic labels for the featured categories. A short local map rather than a
 * network round trip — these are the same nine keys the dashboard features, and
 * an unmapped key falls back to itself so nothing renders blank.
 */
private fun categoryLabelAr(key: String): String = when (key) {
    "supermarket" -> "سوبر ماركت"
    "grocery_store" -> "بقالة"
    "convenience_store" -> "دكان"
    "shopping_mall" -> "مركز تجاري"
    "wholesaler" -> "جملة"
    "liquor_store" -> "مشروبات"
    "gas_station" -> "محطة وقود"
    "restaurant" -> "مطعم"
    "cafe" -> "مقهى"
    else -> key
}

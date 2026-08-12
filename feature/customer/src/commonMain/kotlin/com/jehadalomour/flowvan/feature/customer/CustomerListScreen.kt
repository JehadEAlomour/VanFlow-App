package com.jehadalomour.flowvan.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.common.format.formatJod
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import com.jehadalomour.flowvan.core.model.Customer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * العملاء — the round, as a list.
 *
 * Rows rather than cards: a card costs padding and a shadow per shop, and on a
 * round of fifty that is several screens of scrolling bought for decoration.
 * The search field never scrolls away, because a rep looking for one shop should
 * not have to scroll up before they can type.
 */
@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    onAddCustomer: () -> Unit = {},
    viewModel: CustomerListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(Res.drawable.ic_back),
                    contentDescription = null,
                    tint = Fv.TextHigh,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                stringResource(Res.string.customers_title),
                modifier = Modifier.weight(1f),
                color = Fv.TextHigh,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Search, pinned ────────────────────────────────────────────────────
        SearchField(
            query = state.searchQuery,
            onQueryChange = { viewModel.onEvent(CustomerListEvent.SearchChanged(it)) },
            onClear = { viewModel.onEvent(CustomerListEvent.ClearSearch) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        // ── Filters ───────────────────────────────────────────────────────────
        // Hidden while searching: a query is already a filter, and two competing
        // narrowings on one screen is how a rep ends up staring at an empty list
        // wondering which one hid the shop.
        if (!state.isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(stringResource(Res.string.customers_filter_all), state.filter == CustomerFilter.ALL) {
                    viewModel.onEvent(CustomerListEvent.FilterChanged(CustomerFilter.ALL))
                }
                FilterChip(stringResource(Res.string.customers_filter_route), state.filter == CustomerFilter.ON_ROUTE) {
                    viewModel.onEvent(CustomerListEvent.FilterChanged(CustomerFilter.ON_ROUTE))
                }
                FilterChip(stringResource(Res.string.customers_filter_owing), state.filter == CustomerFilter.OWING) {
                    viewModel.onEvent(CustomerListEvent.FilterChanged(CustomerFilter.OWING))
                }
                FilterChip(stringResource(Res.string.customers_filter_overlimit), state.filter == CustomerFilter.OVER_LIMIT) {
                    viewModel.onEvent(CustomerListEvent.FilterChanged(CustomerFilter.OVER_LIMIT))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Count ─────────────────────────────────────────────────────────────
        if (!state.isLoading) {
            Text(
                "${state.visible.size} ${stringResource(Res.string.customers_count_suffix)}",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = Fv.TextLow,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(6.dp))
        }

        // ── List ──────────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fv.Blue)
                }

                // A search that matched nothing and having no customers at all are
                // different facts, and a rep must be able to tell them apart.
                state.visible.isEmpty() && state.isSearching ->
                    EmptyBlock(
                        message = "${stringResource(Res.string.customers_no_results)} «${state.searchQuery}»",
                        actionLabel = stringResource(Res.string.customers_clear_search),
                        onAction = { viewModel.onEvent(CustomerListEvent.ClearSearch) },
                    )

                state.visible.isEmpty() ->
                    EmptyBlock(message = stringResource(Res.string.customers_empty))

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(state.visible, key = { it.id }) { customer ->
                        CustomerRow(
                            customer = customer,
                            onOpen = { onOpenCustomer(customer.id) },
                            onNavigate = { onNavigateTo(customer.id) },
                        )
                        HorizontalDivider(thickness = 1.dp, color = Fv.Border)
                    }
                }
            }
        }

        // ── New customer ──────────────────────────────────────────────────────
        // A full-width bar rather than a floating circle: it never covers a row,
        // and it is reachable with the thumb that is already holding the phone.
        if (state.canAddCustomer) {
            Surface(
                onClick = onAddCustomer,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Fv.Blue,
            ) {
                Text(
                    stringResource(Res.string.customers_add),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun CustomerRow(
    customer: Customer,
    onOpen: () -> Unit,
    onNavigate: () -> Unit,
) {
    val overLimit = customer.creditLimit > 0 && customer.balance >= customer.creditLimit
    val owing = customer.balance > 0

    // The status bar carries the row's state, at the START edge — which in RTL is
    // the right, where the eye lands first. Colour is doing the scanning here.
    val statusColor = when {
        overLimit -> Fv.Amber
        owing -> Fv.Red
        customer.isOnRoute -> Fv.Green
        else -> Fv.Border
    }
    // Over-limit tints the whole row, unlike every other state which colours one
    // element. The others are information; this one is a prohibition — no credit
    // sale here — and a prohibition has to survive being scrolled past quickly.
    val rowBg = if (overLimit) Color(0xFFFDF6EA) else Fv.Surface

    Row(
        modifier = Modifier.fillMaxWidth().background(rowBg).height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(statusColor))

        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 12.dp),
        ) {
            Text(
                customer.nameAr,
                color = Fv.TextHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${customer.code} · ${customer.area}",
                color = Fv.TextLow,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }

        Column(
            modifier = Modifier.clickable(onClick = onOpen).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                customer.balance.formatJod(AppLanguage.AR),
                color = if (overLimit) Fv.Amber else if (owing) Fv.Red else Fv.TextLow,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            // A bare number in a list is ambiguous — owed, bought, or their limit?
            // One word removes a question the rep would answer by tapping in.
            Text(
                when {
                    overLimit -> stringResource(Res.string.customers_over_limit)
                    owing -> stringResource(Res.string.customers_balance)
                    else -> stringResource(Res.string.customers_no_balance)
                },
                color = Fv.TextLow,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Navigate to the shop. Disabled rather than hidden when the customer has
        // no coordinates: a button that appears on some rows and not others reads
        // as a bug, while a greyed one reads as "this shop has no location yet".
        val hasLocation = customer.lat != null && customer.lng != null
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(44.dp)
                .border(1.dp, if (hasLocation) Fv.Border else Fv.SurfaceTop, RoundedCornerShape(8.dp))
                .background(Fv.Surface, RoundedCornerShape(8.dp))
                .then(if (hasLocation) Modifier.clickable(onClick = onNavigate) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_map),
                contentDescription = stringResource(Res.string.customers_navigate),
                tint = if (hasLocation) Fv.Blue else Fv.TextLow.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Parts ─────────────────────────────────────────────────────────────────────

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Fv.Surface, RoundedCornerShape(6.dp))
            .border(1.dp, Fv.Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(Res.drawable.ic_customers),
            contentDescription = null,
            tint = Fv.TextLow,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    stringResource(Res.string.customers_search_hint),
                    color = Fv.TextLow,
                    fontSize = 13.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Fv.TextHigh, fontSize = 13.sp),
                cursorBrush = SolidColor(Fv.Blue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                painterResource(Res.drawable.ic_cancel),
                contentDescription = stringResource(Res.string.customers_clear_search),
                tint = Fv.TextMid,
                modifier = Modifier.size(18.dp).clickable(onClick = onClear),
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Fv.Blue else Fv.Surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Fv.Border),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else Fv.TextMid,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EmptyBlock(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            message,
            color = Fv.TextMid,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                actionLabel,
                modifier = Modifier.clickable(onClick = onAction).padding(8.dp),
                color = Fv.Blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

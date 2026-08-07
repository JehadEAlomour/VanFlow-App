package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jehadalomour.flowvan.feature.voucher.AppBackHandler
import com.jehadalomour.flowvan.core.designsystem.components.CartItemCard
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.components.ProductAvatar
import com.jehadalomour.flowvan.core.designsystem.components.ProductThumb
import com.jehadalomour.flowvan.core.designsystem.components.ProductImageViewerDialog
import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.designsystem.components.fvFieldColors
import com.jehadalomour.flowvan.core.designsystem.components.standardUnits
import com.jehadalomour.flowvan.core.model.AppliedOffer
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.LineOffer
import com.jehadalomour.flowvan.core.model.OfferChoice
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.model.ServerLine
import com.jehadalomour.flowvan.feature.voucher.ReturnReason
import com.jehadalomour.flowvan.feature.voucher.DiscountType
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.feature.voucher.VoucherEvent
import com.jehadalomour.flowvan.feature.voucher.VoucherState
import com.jehadalomour.flowvan.feature.voucher.VoucherType
import com.jehadalomour.flowvan.feature.voucher.VoucherViewModel
import com.jehadalomour.flowvan.core.common.format.formatJod
import kotlin.math.floor
import com.jehadalomour.flowvan.core.common.i18n.AppLanguage
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// ── Save-button palette — only colors live in the UI layer ───────────────────
private val saleSaveGradient    = listOf(Color(0xFF1D9E75), Color(0xFF0F6E56))
private val returnSaveGradient  = listOf(Color(0xFFD63B3B), Color(0xFF992828))
private val orderSaveGradient   = listOf(Color(0xFF0E9E91), Color(0xFF0A6E66))

/** Picker stock scope: only in-stock (qty > 0) items, or the whole catalog. */
private enum class StockFilter { IN_STOCK, ALL }

@Composable
fun VoucherScreen(
    customerId: String,
    type: VoucherType,
    onBack: () -> Unit,
    onPrint: (invoiceId: String) -> Unit,
    viewModel: VoucherViewModel = koinViewModel { parametersOf(customerId, type) },
) {
    val state by viewModel.state.collectAsState()
    var dialogProduct by remember { mutableStateOf<Product?>(null) }
    // Which unit's line the sheet was opened on (null = opened from the picker, so the
    // sheet starts on the item's first unit). The cart is per (product, unit), so the
    // product alone no longer says which line the rep tapped.
    var dialogUnitId by remember { mutableStateOf<String?>(null) }
    var expandedImage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var stockFilter by remember { mutableStateOf(StockFilter.IN_STOCK) }

    // Picker browsing scope: the stock dropdown chooses in-stock-only (qty > 0) vs the whole
    // catalog, then the category dropdown narrows further. Applies to every voucher type.
    val displayProducts = remember(state.visibleProducts, selectedCategory, stockFilter) {
        val base = if (stockFilter == StockFilter.IN_STOCK) {
            state.visibleProducts.filter { it.vanStock > 0 }
        } else {
            state.visibleProducts
        }
        val cat = selectedCategory
        if (cat == null) base else base.filter { it.category == cat }
    }

    val saveGradient = when (state.type) {
        VoucherType.SALE -> saleSaveGradient
        VoucherType.RETURN -> returnSaveGradient
        VoucherType.ORDER -> orderSaveGradient
    }

    LaunchedEffect(state.savedId) {
        val id = state.savedId ?: return@LaunchedEffect
        onPrint(id)
    }

    AppBackHandler(enabled = state.view == VoucherView.CART) {
        viewModel.onEvent(VoucherEvent.ToggleView)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Fv.Surface,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (state.view == VoucherView.CART) viewModel.onEvent(VoucherEvent.ToggleView)
                        else onBack()
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = null,
                            tint = Fv.TextHigh,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(state.titleRes), color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        state.customer?.let { Text(it.nameAr, color = Fv.TextMid, fontSize = 11.sp) }
                    }
                    CartToggle(state.view, state.cart.size) { viewModel.onEvent(VoucherEvent.ToggleView) }
                }
            }

            // ── RETURN: reference to the original sale invoice ─────────────────
            if (state.type == VoucherType.RETURN) {
                ReturnReferenceBanner(
                    referenceNumber = state.referenceNumber,
                    onPick = { viewModel.onEvent(VoucherEvent.OpenSourcePicker) },
                )
            }

            // ── Views ─────────────────────────────────────────────────────────
            when (state.view) {
                VoucherView.PICKER -> {
                    ProductListPicker(
                        products = displayProducts,
                        allProducts = state.products,
                        productUnits = state.productUnits,
                        customerPrices = state.customerPrices,
                        searchQuery = state.searchQuery,
                        selectedCategory = selectedCategory,
                        stockFilter = stockFilter,
                        showStockBadge = state.showStockBadge,
                        onSearch = { viewModel.onEvent(VoucherEvent.SearchChanged(it)) },
                        onSelectCategory = { selectedCategory = it },
                        onSelectStockFilter = { stockFilter = it },
                        onTapProduct = { dialogProduct = it; dialogUnitId = null },
                        onExpandImage = { expandedImage = it },
                        modifier = Modifier.weight(1f),
                    )
                    if (state.cart.isNotEmpty()) {
                        PickerSummaryBar(itemCount = state.cart.size, total = state.total)
                    }
                }
                VoucherView.CART -> {
                    CartView(
                        state = state,
                        onTapLine = { productId, unitId ->
                            dialogProduct = state.products.firstOrNull { it.id == productId }
                            dialogUnitId = unitId
                        },
                        onNotesChange = { viewModel.onEvent(VoucherEvent.NotesChanged(it)) },
                        onReasonSelect = { viewModel.onEvent(VoucherEvent.ReasonSelected(it)) },
                        onPaymentMethod = { viewModel.onEvent(VoucherEvent.PaymentMethodSelected(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    CartSummaryCard(
                        state = state,
                        onDiscountInputChange = { viewModel.onEvent(VoucherEvent.VoucherDiscountInputChanged(it)) },
                        onDiscountTypeToggle = { viewModel.onEvent(VoucherEvent.VoucherDiscountTypeToggled) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 6.dp),
                    )
                    if (state.isAwaitingApproval) {
                        // Blocked: a return request is waiting on the manager. The
                        // salesman can only wait (auto-prints on approve) or cancel.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Fv.SurfaceTop),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "بانتظار موافقة المدير على المرتجع…",
                                    color = Fv.TextMid,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            TextButton(onClick = { viewModel.onEvent(VoucherEvent.CancelApproval) }) {
                                Text("إلغاء الطلب", color = Fv.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        if (state.canSave) Modifier.background(Brush.linearGradient(saveGradient))
                                        else Modifier.background(Fv.SurfaceTop),
                                    )
                                    .clickable(enabled = state.canSave) {
                                        viewModel.onEvent(VoucherEvent.Save)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(state.saveLabelRes),
                                    color = if (state.canSave) Color.White else Fv.TextMid,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Full-screen product image viewer (tap a list thumbnail) ───────────────
    expandedImage?.let { url ->
        ProductImageViewerDialog(imageUrl = url, onDismiss = { expandedImage = null })
    }

    // ── Item bottom sheet ─────────────────────────────────────────────────────
    dialogProduct?.let { product ->
        // ALL of this item's cart lines — one per unit. The sheet edits the one matching the
        // unit currently selected in its dropdown. Fall back to sku: when offers are active
        // the cart renders server lines keyed by sku (itemNumber), and a line's productId can
        // differ from productBySku[sku].id — without the sku fallback the tap opens a blank
        // "add" sheet (qty 1, base unit) instead of the real line.
        val productLines = state.cart.filter { it.productId == product.id }
            .ifEmpty { state.cart.filter { it.sku == product.sku } }
        // SALE + server offers: seed the line-discount field with the offer discount
        // applied to this line, so tapping a discounted line shows its % (not blank).
        val offerDiscountPct = if (state.useServerOffers) {
            state.serverLines.firstOrNull { it.itemNumber == product.sku }
                ?.takeIf { it.lineDiscountJod > 0 }
                ?.discountFraction
        } else null
        AddItemBottomSheet(
            product = product,
            cartLines = productLines,
            initialUnitId = dialogUnitId,
            offerDiscountPct = offerDiscountPct,
            dbUnits = state.productUnits[product.id] ?: emptyList(),
            enforceStock = state.showStockBadge,
            canDiscount = state.showDiscountInputs,
            canEditPrice = state.canEditPrice,
            customerBasePrice = state.customerPrices[product.sku],
            onConfirm = { qty, unit, unitPrice, discountPct ->
                viewModel.onEvent(
                    VoucherEvent.ConfirmItemDialog(product, qty, unit, unitPrice, discountPct),
                )
                dialogProduct = null
                dialogUnitId = null
            },
            onDelete = { unitId ->
                viewModel.onEvent(VoucherEvent.RemoveLine(product.id, unitId))
                dialogProduct = null
                dialogUnitId = null
            },
            onDismiss = { dialogProduct = null; dialogUnitId = null },
        )
    }

    // ── SALE: choose-free-item sheet (ITEM_QTY_REWARD gift picks) ───────────────
    // A gift is an entitlement, so the sheet is MANDATORY: it shows whenever any offer still
    // has an unfilled gift quota and can't be dismissed until every quota is picked, after
    // which it closes automatically.
    val giftQuotaUnmet = state.pendingChoices.any { choice ->
        val pool = choice.choices.toSet()
        state.chosenFreeItems.count { it in pool } < choice.qty
    }
    if (state.offersEnabled && giftQuotaUnmet) {
        ChooseFreeItemSheet(
            choices = state.pendingChoices,
            selectedItems = state.chosenFreeItems,
            productNameFor = { itemNumber ->
                state.products.firstOrNull { it.sku == itemNumber }?.nameAr ?: itemNumber
            },
            onAdd = { offerId, itemNumber ->
                viewModel.onEvent(VoucherEvent.ChooseFreeItem(offerId, itemNumber))
            },
            onRemove = { offerId, itemNumber ->
                viewModel.onEvent(VoucherEvent.RemoveFreeItem(offerId, itemNumber))
            },
        )
    }

    // ── RETURN: source sale-invoice picker ─────────────────────────────────────
    if (state.showSourcePicker) {
        SourceInvoicePickerDialog(
            invoices = state.sourceInvoices,
            lookupQuery = state.sourceLookupQuery,
            isLookingUp = state.isLookingUp,
            onLookupChange = { viewModel.onEvent(VoucherEvent.SourceLookupChanged(it)) },
            onLookup = { viewModel.onEvent(VoucherEvent.LookupSource) },
            onSelect = { viewModel.onEvent(VoucherEvent.SelectSourceInvoice(it)) },
            onDismiss = { viewModel.onEvent(VoucherEvent.DismissSourcePicker) },
        )
    }

    // ── Save sheet: PaymentMethodDialog for SALE, AlertDialog for RETURN/ORDER
    if (state.showSaveSheet) {
        if (state.showPaymentDialog) {
            PaymentMethodDialog(
                current = state.paymentMethod,
                onSelect = { viewModel.onEvent(VoucherEvent.PaymentMethodSelected(it)) },
                onConfirm = { viewModel.onEvent(VoucherEvent.ConfirmSave) },
                onDismiss = { viewModel.onEvent(VoucherEvent.DismissSaveSheet) },
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(VoucherEvent.DismissSaveSheet) },
                title = { Text(stringResource(Res.string.voucher_confirm_save_title), color = Fv.TextHigh) },
                text = { Text(state.confirmTextRes?.let { stringResource(it) } ?: "", color = Fv.TextHigh) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(VoucherEvent.ConfirmSave) }) {
                        Text(
                            stringResource(Res.string.confirm),
                            color = when (state.type) {
                                VoucherType.RETURN -> Fv.Red
                                VoucherType.ORDER -> Fv.Teal
                                else -> Fv.Green
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(VoucherEvent.DismissSaveSheet) }) {
                        Text(stringResource(Res.string.cancel), color = Fv.TextMid)
                    }
                },
                containerColor = Fv.Surface,
            )
        }
    }

    state.errorAr?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(VoucherEvent.DismissError) },
            title = { Text(stringResource(Res.string.dialog_error_title), color = Fv.TextHigh) },
            text = { Text(msg, color = Fv.TextHigh) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(VoucherEvent.DismissError) }) {
                    Text(stringResource(Res.string.dialog_ok), color = Fv.Blue)
                }
            },
            containerColor = Fv.Surface,
        )
    }
}

// ── Cart Toggle ───────────────────────────────────────────────────────────────

@Composable
private fun CartToggle(view: VoucherView, count: Int, onClick: () -> Unit) {
    val isPickerView = view == VoucherView.PICKER
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isPickerView) Fv.Blue else Fv.SurfaceTop)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPickerView) Res.drawable.ic_cart else Res.drawable.ic_inventory),
                contentDescription = null,
                tint = if (isPickerView) Color.White else Fv.TextMid,
                modifier = Modifier.size(21.dp),
            )
        }
        if (isPickerView && count > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(19.dp)
                    .background(Fv.Red, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.coerceAtMost(99).toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Product List Picker ───────────────────────────────────────────────────────

@Composable
private fun ProductListPicker(
    products: List<Product>,
    allProducts: List<Product>,
    productUnits: Map<String, List<ProductUnit>>,
    /** Customer's price list as sku→base-unit price (JOD); empty when unassigned. */
    customerPrices: Map<String, Double>,
    searchQuery: String,
    selectedCategory: String?,
    stockFilter: StockFilter,
    showStockBadge: Boolean,
    onSearch: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectStockFilter: (StockFilter) -> Unit,
    onTapProduct: (Product) -> Unit,
    onExpandImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Category options follow the stock scope: in-stock-only mode lists just the categories
    // that still have qty > 0 items (so no option leads to an empty list), while the "all
    // items" scope lists every category in the catalog.
    val categories = remember(allProducts, stockFilter) {
        val base = if (stockFilter == StockFilter.IN_STOCK) allProducts.filter { it.vanStock > 0 } else allProducts
        base.map { it.category }.distinct().filter { it.isNotBlank() }.sorted()
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text(stringResource(Res.string.sale_search_product), color = Fv.TextMid) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = fvFieldColors(),
        )

        // ── Filters under the search bar: category (searchable) + stock scope ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryFilterDropdown(
                categories = categories,
                selected = selectedCategory,
                onSelect = onSelectCategory,
                modifier = Modifier.weight(1f),
            )
            StockFilterDropdown(
                selected = stockFilter,
                onSelect = onSelectStockFilter,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(products, key = { it.id }) { product ->
                val baseUnit = remember(product.id, productUnits) {
                    productUnits[product.id]?.minByOrNull { it.conversionQty }
                }
                // Price shown on the list card: the customer's price-list price when the
                // item is on the list (base-unit JOD × the shown unit's conversion), else
                // the base unit's catalog price, else the item's main sale price. Mirrors
                // stepItem/the add-item sheet so the browse price matches the cart price.
                val listBase = customerPrices[product.sku]
                val resolvedPrice = if (listBase != null) {
                    listBase * (baseUnit?.conversionQty ?: 1.0)
                } else {
                    baseUnit?.price ?: product.salePrice
                }
                ProductListCard(
                    product = product,
                    unitName = baseUnit?.name ?: product.unit,
                    unitPrice = resolvedPrice,
                    showStockBadge = showStockBadge,
                    onTap = { onTapProduct(product) },
                    onExpandImage = onExpandImage,
                )
            }
            if (products.isEmpty()) {
                item {
                    Text(stringResource(Res.string.voucher_no_products), color = Fv.TextMid, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductListCard(
    product: Product,
    unitName: String,
    unitPrice: Double,
    showStockBadge: Boolean,
    onTap: () -> Unit,
    onExpandImage: (String) -> Unit,
) {
    // DIAGNOSTIC: log each product's image state so we can see, in logcat, whether the
    // item actually carries an imageUrl (data problem) or it's present but not rendering.
    LaunchedEffect(product.id, product.imageUrl) {
        Logger.withTag("ProductImage").d(
            "list item id=${product.id} sku=${product.sku} name=${product.nameAr} " +
                "imageUrl=${product.imageUrl ?: "<null>"} blank=${product.imageUrl.isNullOrBlank()}",
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Show the product image when present, falling back to the letter avatar.
            // Tapping the image (only when one exists) opens the full-screen viewer
            // instead of the add-item sheet.
            val hasImage = !product.imageUrl.isNullOrBlank()
            Box(
                modifier = Modifier.clickable(enabled = hasImage) {
                    product.imageUrl?.let(onExpandImage)
                },
            ) {
                ProductThumb(
                    imageUrl = product.imageUrl,
                    seed = product.category,
                    letter = product.nameAr.firstOrNull()?.toString() ?: "؟",
                    size = 54.dp,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.nameAr,
                    color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(product.sku, color = Fv.TextMid, fontSize = 11.sp)
                    if (showStockBadge) StockBadge(product)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(unitPrice.formatJod(AppLanguage.AR), color = Fv.Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("/ $unitName", color = Fv.TextMid, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.size(8.dp))
            Text("‹", color = Fv.TextMid, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun StockBadge(product: Product) {
    val outOfStock = product.vanStock <= 0
    val low = !outOfStock && product.vanStock < product.minStock
    val chipColor = if (outOfStock) Fv.Red else if (low) Fv.Amber else Fv.Green
    val chipLabel = when {
        outOfStock -> stringResource(Res.string.van_stock_out_of_stock)
        low -> stringResource(Res.string.voucher_stock_low_count, product.vanStock)
        else -> stringResource(Res.string.voucher_stock_available_count, product.vanStock)
    }
    Box(
        modifier = Modifier.background(chipColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(chipLabel, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Filter dropdowns (category + stock scope) ─────────────────────────────────

/** A boxed, pill-shaped anchor that opens a dropdown; used by both picker filters. */
@Composable
private fun FilterAnchor(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Fv.Blue.copy(alpha = 0.16f) else Fv.SurfaceTop)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (active) Fv.Blue else Fv.TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("▾", color = if (active) Fv.Blue else Fv.TextMid, fontSize = 14.sp)
    }
}

/** Searchable category picker: type in the in-menu field to narrow the category list. */
@Composable
private fun CategoryFilterDropdown(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(categories, query) {
        if (query.isBlank()) categories else categories.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    Box(modifier = modifier) {
        FilterAnchor(
            label = selected ?: stringResource(Res.string.voucher_filter_all_categories),
            active = selected != null,
            onClick = { query = ""; expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Fv.Surface),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(Res.string.voucher_filter_category), color = Fv.TextMid) },
                singleLine = true,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).width(200.dp),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.voucher_filter_all_categories), fontWeight = FontWeight.SemiBold, color = Fv.TextHigh) },
                onClick = { onSelect(null); expanded = false },
            )
            filtered.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat, color = Fv.TextHigh, fontWeight = if (cat == selected) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onSelect(cat); expanded = false },
                )
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.voucher_no_products), color = Fv.TextMid, fontSize = 12.sp) },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

/** Stock-scope picker: in-stock-only (qty > 0) vs the whole catalog. */
@Composable
private fun StockFilterDropdown(
    selected: StockFilter,
    onSelect: (StockFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        StockFilter.IN_STOCK to stringResource(Res.string.voucher_filter_stock_available),
        StockFilter.ALL to stringResource(Res.string.voucher_filter_stock_all),
    )
    Box(modifier = modifier) {
        FilterAnchor(
            label = options.first { it.first == selected }.second,
            active = selected != StockFilter.IN_STOCK,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Fv.Surface),
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = Fv.TextHigh, fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun PickerSummaryBar(itemCount: Int, total: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A2A3A)).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(total.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.voucher_item_count, itemCount), color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
    }
}

// ── Cart View ─────────────────────────────────────────────────────────────────

@Composable
private fun CartView(
    state: VoucherState,
    onTapLine: (productId: String, unitId: String) -> Unit,
    onNotesChange: (String) -> Unit,
    onReasonSelect: (ReturnReason) -> Unit,
    onPaymentMethod: (PaymentMethod) -> Unit,
    modifier: Modifier,
) {
    // Look up product name/avatar seed by itemNumber (= sku) for server-fed lines (SALE).
    val productBySku = remember(state.products) { state.products.associateBy { it.sku } }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
    ) {
        // ── SALE: editable Cash/Credit toggle ─────────────────────────────────
        if (state.offersEnabled) {
            item(key = "payment-toggle") {
                PaymentToggle(current = state.paymentMethod, onSelect = onPaymentMethod)
            }
        }
        // ── SALE: offline banner ──────────────────────────────────────────────
        if (state.isOffline) {
            item(key = "offline-banner") { OfflineBanner(text = state.offlineBannerTextAr) }
        }
        // ── Cart lines (per-line offer discount shown on each line) ────────────
        // SALE online: render the server-fed result (per-line server discount + net).
        // Otherwise: render the on-device cart lines (RETURN/ORDER unchanged).
        //
        // The offer result is keyed by SKU, so an item the rep entered as two variant lines
        // (3 red + 2 blue) comes back as ONE row — rendering it would silently swallow a line
        // they typed. Such a cart falls back to the on-device lines, which already carry the
        // same offer discount through [VoucherState.displayCart].
        val serverLinesFaithful = state.cart.distinctBy { it.sku }.size == state.cart.size
        if (state.useServerOffers && state.serverLines.isNotEmpty() && serverLinesFaithful) {
            items(state.serverLines, key = { "srv-${it.itemNumber}" }) { srv ->
                val product = productBySku[srv.itemNumber]
                val cartLine = state.cart.firstOrNull { it.sku == srv.itemNumber }
                ServerCartLineCard(
                    line = srv,
                    nameAr = product?.nameAr ?: srv.itemNumber,
                    unit = cartLine?.unit ?: "",
                    onTap = { product?.let { onTapLine(it.id, cartLine?.unitId.orEmpty()) } },
                )
            }
        } else {
            // Composite key: two lines of one item under a single productId key is a
            // LazyColumn crash ("Key was already used").
            items(state.displayCart, key = { it.key }) { line ->
                CartItemCard(line = line, onTap = { onTapLine(line.productId, line.unitId) })
            }
        }
        // ── SALE: FREE lines — a free item is a NORMAL cart line at its real price with a
        // 100% discount (net 0). The engine emits one qty-1 line per pick, so N picks of the
        // same gift merge by ITEM into one "× N" line (e.g. 3 water + 2 pepsi → two lines).
        if (state.offersEnabled) {
            val mergedFree = state.freeLines
                .groupBy { it.itemNumber }
                .map { (_, lines) -> lines.first().copy(qty = lines.sumOf { it.qty }) }
            items(mergedFree, key = { "free-${it.itemNumber}" }) { free ->
                val gross = free.unitPriceJod * free.qty
                ServerCartLineCard(
                    line = ServerLine(
                        itemNumber = free.itemNumber,
                        qty = free.qty,
                        unitPriceJod = free.unitPriceJod,
                        lineDiscountJod = gross,   // 100% off → net 0
                        lineNetJod = 0.0,
                        offers = listOf(LineOffer(offerId = free.offerId, name = "هدية", pct = 100.0, discountJod = gross)),
                    ),
                    nameAr = state.products.firstOrNull { it.sku == free.itemNumber }?.nameAr ?: free.itemNumber,
                    unit = state.cart.firstOrNull { it.sku == free.itemNumber }?.unit ?: "",
                    onTap = {},   // offer-driven, read-only
                )
            }
        }
        if (state.cart.isEmpty() && state.freeLines.isEmpty()) {
            item {
                Text(stringResource(Res.string.voucher_cart_empty_hint), color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.padding(24.dp))
            }
        }
        if (state.showReasonRow) {
            item { ReasonRow(state.reason, onReasonSelect) }
        }

        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(Res.string.voucher_notes_optional), color = Fv.TextMid, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fvFieldColors(),
            )
        }
    }
}

// ── SALE: editable Cash/Credit toggle (in-cart) ───────────────────────────────

@Composable
private fun PaymentToggle(current: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Fv.SurfaceTop, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            PaymentMethod.CASH to stringResource(Res.string.payment_method_cash),
            PaymentMethod.CREDIT to stringResource(Res.string.payment_method_credit),
        ).forEach { (method, label) ->
            val active = method == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Fv.Blue else Color.Transparent)
                    .clickable { onSelect(method) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) Color.White else Fv.TextMid,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── SALE: offline banner ──────────────────────────────────────────────────────

@Composable
private fun OfflineBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Fv.Amber.copy(alpha = 0.14f))
            .border(0.5.dp, Fv.Amber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = Fv.Amber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── SALE: server-fed cart line (per-line server discount + net) ───────────────

@Composable
private fun ServerCartLineCard(
    line: ServerLine,
    nameAr: String,
    unit: String,
    onTap: () -> Unit,
) {
    val hasDiscount = line.lineDiscountJod > 0
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            0.5.dp,
            if (hasDiscount) Fv.Green.copy(alpha = 0.45f) else Fv.Border,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductAvatar(
                    seed = nameAr,
                    letter = nameAr.firstOrNull()?.toString() ?: "؟",
                    size = 50.dp,
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        nameAr,
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        buildString {
                            append("${line.unitPriceJod.formatJod(AppLanguage.AR)} × ${line.qty.toInt()}")
                            if (unit.isNotBlank()) append(" · $unit")
                        },
                        color = Fv.TextMid,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            line.lineNetJod.formatJod(AppLanguage.AR),
                            color = Fv.Blue,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        // Total WITHOUT discount, struck through, so the saving is obvious.
                        if (hasDiscount) {
                            Text(
                                line.grossJod.formatJod(AppLanguage.AR),
                                color = Fv.TextMid,
                                fontSize = 12.sp,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                            )
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .background(Fv.SurfaceTop, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        "× ${line.qty.toInt()}",
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // ── Per-line offer rows: offer name + % + amount saved ───────────────
            if (hasDiscount) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
                Spacer(Modifier.height(8.dp))
                if (line.offers.isNotEmpty()) {
                    line.offers.forEach { offer ->
                        // Amount rewards carry pct = 0 (they're a fils amount, not a %),
                        // so derive the EFFECTIVE % from the amount saved vs the line gross
                        // — otherwise an amount offer shows a misleading "-0%".
                        val effPct =
                            if (offer.pct > 0.0) offer.pct
                            else if (line.grossJod > 0.0) offer.discountJod / line.grossJod * 100.0
                            else 0.0
                        OfferLineRow(offer.copy(pct = effPct))
                    }
                } else {
                    // Fallback (older server without per-line attribution): combined % only.
                    OfferLineRow(
                        LineOffer(
                            offerId = "",
                            name = "خصم العرض",
                            pct = line.discountFraction * 100.0,
                            discountJod = line.lineDiscountJod,
                        ),
                    )
                }
            }
        }
    }
}

/** A single applied-offer row on a cart line: 🎟 name · −X% · −amount. */
@Composable
private fun OfferLineRow(offer: LineOffer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("🎟", fontSize = 12.sp)
        Text(
            offer.name,
            color = Fv.Green,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .background(Fv.Green.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                "- ${formatPct(offer.pct)}",
                color = Fv.Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "- ${offer.discountJod.formatJod(AppLanguage.AR)}",
            color = Fv.Green,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Format an offer percentage: drop the decimal when whole (10.0 → "10%", 12.5 → "12.5%"). */
private fun formatPct(pct: Double): String {
    val rounded = (pct * 10).toLong() / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    return "$text%"
}

// ── SALE: choose-free-item bottom sheet ───────────────────────────────────────

@Composable
private fun ChooseFreeItemSheet(
    choices: List<OfferChoice>,
    selectedItems: List<String>,
    productNameFor: (String) -> String,
    onAdd: (offerId: String, itemNumber: String) -> Unit,
    onRemove: (offerId: String, itemNumber: String) -> Unit,
) {
    if (choices.isEmpty()) return
    fun picksFor(choice: OfferChoice): Int {
        val pool = choice.choices.toSet()
        return selectedItems.count { it in pool }
    }
    val totalPicked = choices.sumOf { picksFor(it) }
    val totalQuota = choices.sumOf { it.qty }
    Dialog(
        // Mandatory: not dismissible. The caller hides the sheet automatically once every
        // gift quota is filled, so the offer is always applied.
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp).height(4.dp)
                                .background(Color(0xFFDDE8F5), RoundedCornerShape(2.dp)),
                        )
                    }
                    Text("اختر هديتك", color = Fv.TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    choices.forEach { choice ->
                        val picks = picksFor(choice)
                        val atQuota = picks >= choice.qty
                        Text(
                            "اختر ${choice.qty} ($picks/${choice.qty})",
                            color = if (atQuota) Fv.Green else Fv.TextMid,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp, top = 2.dp),
                        )
                        choice.choices.forEach { itemNumber ->
                            val count = selectedItems.count { it == itemNumber }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (count > 0) Fv.Green.copy(alpha = 0.18f)
                                        else Fv.Green.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        if (count > 0) 1.5.dp else 0.5.dp,
                                        Fv.Green.copy(alpha = if (count > 0) 0.7f else 0.30f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text("🎁", fontSize = 14.sp)
                                        Text(
                                            productNameFor(itemNumber),
                                            color = Fv.TextHigh,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    // Quantity stepper: − count + (so a single-item pool like
                                    // "3 free water" can be picked as water ×3).
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        StepperButton(symbol = "−", enabled = count > 0) {
                                            onRemove(choice.offerId, itemNumber)
                                        }
                                        Text(
                                            "$count",
                                            color = Fv.TextHigh,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.widthIn(min = 16.dp),
                                            textAlign = TextAlign.Center,
                                        )
                                        StepperButton(symbol = "+", enabled = !atQuota) {
                                            onAdd(choice.offerId, itemNumber)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Progress hint. The sheet closes on its own once this reaches the quota.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEDF2F8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "اختر كل الهدايا للمتابعة ($totalPicked/$totalQuota)",
                            color = Fv.TextMid,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/** Small round +/− control for the gift-quantity stepper. Greys out when [enabled] is false. */
@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Fv.Green.copy(alpha = 0.16f) else Color(0xFFF0F3F7))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = if (enabled) Fv.Green else Color(0xFFB6C2D2),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Reason Row (shown only for RETURN via state.showReasonRow) ────────────────

@Composable
private fun ReasonRow(selected: ReturnReason?, onSelect: (ReturnReason) -> Unit) {
    Column {
        Text("${stringResource(Res.string.return_reason)} *", color = Fv.TextMid, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReturnReason.entries.forEach { reason ->
                val active = reason == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) Fv.Red else Fv.SurfaceTop, RoundedCornerShape(20.dp))
                        .clickable { onSelect(reason) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(reason.labelRes), color = if (active) Color.White else Fv.TextMid, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
    }
}

// ── Delivery Date Field (shown only for ORDER via state.showDeliveryDate) ─────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryDateField(dateMillis: Long?, onChange: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val label = dateMillis?.let { millis ->
        val dt = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${dt.dayOfMonth.toString().padStart(2, '0')}/${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
    } ?: stringResource(Res.string.voucher_delivery_date_placeholder)

    Column {
        Text(
            stringResource(Res.string.voucher_delivery_date_label),
            color = Fv.TextMid,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Fv.Surface)
                .border(1.dp, Fv.Border, RoundedCornerShape(10.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                label,
                color = if (dateMillis != null) Fv.TextHigh else Fv.TextMid,
                fontSize = 13.sp,
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(pickerState.selectedDateMillis)
                    showPicker = false
                }) { Text(stringResource(Res.string.confirm), color = Fv.Blue) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

// ── Cart Summary Card ─────────────────────────────────────────────────────────

@Composable
private fun CartSummaryCard(
    state: VoucherState,
    onDiscountInputChange: (String) -> Unit,
    onDiscountTypeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, Fv.Border),
    ) {
        Column {
            // Tax-exempt banner, at the very top of the totals card — the rep sees WHY
            // there is no tax line before taking the money, not after the receipt
            // prints. Same rule the server applies (flag AND validity window), so the
            // total shown here is the total that gets posted.
            if (state.isTaxExemptSale) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFFFF6E5))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.voucher_tax_exempt_banner),
                        color = Color(0xFFC97B1A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    state.customer?.taxExemptionNumber?.takeIf { it.isNotBlank() }?.let { n ->
                        Spacer(Modifier.width(8.dp))
                        Text(n, color = Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)) {
                    SummaryDetailRow(stringResource(Res.string.voucher_detail_subtotal), state.subtotal.formatJod(AppLanguage.AR), Fv.TextMid)
                    if (state.lineDiscountTotal > 0)
                        SummaryDetailRow(stringResource(Res.string.voucher_line_discount_total), "- ${state.lineDiscountTotal.formatJod(AppLanguage.AR)}", Fv.Red)
                    if (state.showDiscountSection && state.showDiscountInputs) {
                        Spacer(Modifier.height(8.dp))
                        VoucherDiscountSection(
                            type = state.voucherDiscountType,
                            input = state.voucherDiscountInput,
                            computedAmount = state.voucherDiscountAmount,
                            onInputChange = onDiscountInputChange,
                            onTypeToggle = onDiscountTypeToggle,
                        )
                    }
                    if (state.taxAmount > 0) {
                        Spacer(Modifier.height(6.dp))
                        SummaryDetailRow(stringResource(state.taxLabelRes), state.taxAmount.formatJod(AppLanguage.AR), Fv.TextMid)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.voucher_final_total), color = Fv.TextMid, fontSize = 12.sp)
                    Text(state.total.formatJod(AppLanguage.AR), color = Fv.Blue, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(if (expanded) "▲" else "▼", color = Fv.TextMid, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SummaryDetailRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Fv.TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VoucherDiscountSection(
    type: DiscountType,
    input: String,
    computedAmount: Double,
    onInputChange: (String) -> Unit,
    onTypeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.background(Fv.SurfaceTop, RoundedCornerShape(20.dp)).padding(2.dp)) {
            listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to stringResource(Res.string.voucher_discount_type_value)).forEach { (t, label) ->
                val active = t == type
                Box(
                    modifier = Modifier
                        .clickable { if (!active) onTypeToggle() }
                        .background(if (active) Fv.Blue else Color.Transparent, RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (active) Color.White else Fv.TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { v -> onInputChange(v.filter { it.isDigit() || it == '.' }.take(8)) },
            placeholder = { Text(if (type == DiscountType.PERCENT) stringResource(Res.string.voucher_discount_hint_percent) else stringResource(Res.string.voucher_discount_hint_value), color = Fv.TextMid, fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(if (type == DiscountType.PERCENT) "%" else "د.أ", color = Fv.TextMid, fontSize = 11.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = fvFieldColors(),
        )
        if (computedAmount > 0) {
            Text("- ${computedAmount.formatJod(AppLanguage.AR)}", color = Fv.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Add Item Bottom Sheet — exact same as SaleVoucherScreen ──────────────────

@Composable
private fun AddItemBottomSheet(
    product: Product,
    /** Every cart line of this item — one per unit. The sheet edits the selected unit's. */
    cartLines: List<CartLine>,
    /** The unit whose line was tapped; null when opened fresh from the picker. */
    initialUnitId: String?,
    offerDiscountPct: Double? = null,
    dbUnits: List<ProductUnit>,
    enforceStock: Boolean,
    canDiscount: Boolean,
    canEditPrice: Boolean,
    /** The customer's price-list base-unit price for this product (JOD), or null. */
    customerBasePrice: Double? = null,
    onConfirm: (qty: Double, unit: ProductUnit, unitPrice: Double, discountPct: Double) -> Unit,
    onDelete: (unitId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Use the item's OWN units (synced from the dashboard/ERP). No hardcoded list —
    // if an item somehow has none, fall back to just its base unit. That fallback keeps a
    // BLANK id on purpose: it isn't a real item_units row, and posting a made-up id would
    // be rejected by the server, so the line stays on the item's base pool.
    val effectiveUnits: List<ProductUnit> = remember(product.id, dbUnits, customerBasePrice) {
        val raw = if (dbUnits.isNotEmpty()) dbUnits
        else listOf(
            ProductUnit(
                id = "",
                productId = product.id,
                name = product.unit,
                price = product.salePrice,
                conversionQty = 1.0,
            ),
        )
        // Any unit the ERP didn't price gets one derived from the base (piece) price:
        //   unit price = conversionQty × price-per-base-piece.
        // When the customer has a price-list price, it is the base-unit price and
        // OVERRIDES every unit (each pack derives from it).
        val base = raw.minByOrNull { it.conversionQty }
        val baseConv = base?.conversionQty?.takeIf { it > 0.0 } ?: 1.0
        val perBasePrice =
            (customerBasePrice ?: base?.price?.takeIf { it > 0.0 } ?: product.salePrice) / baseConv
        raw.map { u ->
            if (customerBasePrice != null || u.price <= 0.0) {
                u.copy(price = perBasePrice * u.conversionQty)
            } else {
                u
            }
        }
    }
    val initialUnit: ProductUnit = remember(product.id, initialUnitId, effectiveUnits) {
        // Resolve the tapped line's unit BY ID. Name matching is only the fallback for a line
        // that predates per-unit ids — two colours of one item can share a display name, so a
        // name match would open the wrong line.
        val tappedName = cartLines.firstOrNull { it.unitId == initialUnitId }?.unit
            ?: cartLines.firstOrNull()?.unit
        effectiveUnits.firstOrNull { initialUnitId != null && it.id == initialUnitId }
            ?: effectiveUnits.firstOrNull { it.name == tappedName?.takeIf { n -> n.isNotBlank() } }
            ?: effectiveUnits.firstOrNull()
            ?: ProductUnit(id = "", productId = product.id, name = product.unit, price = product.salePrice, conversionQty = 1.0)
    }
    var selectedUnit by remember(product.id, initialUnitId) { mutableStateOf(initialUnit) }
    // The line under edit is the SELECTED unit's — switching unit in the dropdown moves to
    // that unit's line, or to a fresh "add" when the rep hasn't entered that unit yet.
    val currentLine = cartLines.firstOrNull { it.unitId == selectedUnit.id }

    var qty by remember(product.id, selectedUnit.id) { mutableStateOf(currentLine?.qty ?: 1.0) }
    // Editable text mirror of [qty] so the rep can TYPE a quantity, not just tap +/-.
    // The +/- handlers keep it in sync; typing digits updates qty (0 when cleared).
    var qtyText by remember(product.id, selectedUnit.id) { mutableStateOf(qty.toInt().toString()) }
    var lineDiscountType by remember(product.id, selectedUnit.id) { mutableStateOf(DiscountType.PERCENT) }
    var discountText by remember(product.id, selectedUnit.id) {
        // Prefer the offer discount applied to this line (SALE), else the manual line discount.
        val initial = offerDiscountPct ?: currentLine?.discountPct ?: 0.0
        mutableStateOf(if (initial > 0) (initial * 100).toInt().toString() else "")
    }
    // The text this field was seeded with when it came from the OFFER rather than a manual
    // discount. The offer % is shown here for information only: the engine re-applies the offer
    // on top of the line's manual discount, so persisting this value as the manual discount
    // would count the offer TWICE (tap a discounted line, confirm, and the discount doubles).
    // Confirming while the field still holds this exact value therefore keeps the line's own
    // manual discount instead. Editing it makes it a real manual discount, as typed.
    val offerSeedText = remember(product.id, selectedUnit.id) {
        offerDiscountPct?.takeIf { it > 0 }?.let { (it * 100).toInt().toString() }
    }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    // Editable price (only when the salesman has canEditPrice). Resets per unit.
    var priceText by remember(product.id, selectedUnit.id) {
        mutableStateOf((currentLine?.unitPrice?.takeIf { it > 0 } ?: selectedUnit.price).toString())
    }
    val effectivePrice = if (canEditPrice) (priceText.toDoubleOrNull() ?: selectedUnit.price) else selectedUnit.price

    // DIAGNOSTIC: dump the unit list (with derived prices) once when the sheet opens.
    LaunchedEffect(product.id, dbUnits) {
        val dump = effectiveUnits.joinToString { "${it.name}(conv=${it.conversionQty}, price=${it.price})" }
        Logger.withTag("UnitPrice").d(
            "sheet open id=${product.id} name=${product.nameAr} salePrice=${product.salePrice} units=[$dump]",
        )
    }

    val gross = effectivePrice * qty
    val discountPct = when (lineDiscountType) {
        DiscountType.PERCENT -> discountText.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0) ?: 0.0
        DiscountType.VALUE -> if (gross > 0) (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, gross) / gross else 0.0
    }
    val lineTotal = gross * (1.0 - discountPct)

    // Stock check (SALE only): the requested quantity is converted to base units via the
    // selected unit's pack size, then validated against the pool it will actually draw from
    // — a variant (أحمر) has its own stock, a packaging unit (كرتونة ×12) shares the item's.
    val availableBase =
        if (selectedUnit.isStockUnit) selectedUnit.vanStock.toDouble() else product.vanStock.toDouble()
    val requestedBase = qty * selectedUnit.conversionQty
    val maxQtyForUnit = if (selectedUnit.conversionQty > 0.0)
        floor(availableBase / selectedUnit.conversionQty).toInt() else 0
    val exceedsStock = enforceStock && requestedBase > availableBase
    val canIncrement = !enforceStock || qty < maxQtyForUnit
    val canConfirm = qty > 0 && !exceedsStock

    val blueGradient = Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C)))
    val greenGradient = Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF0F6E56)))
    val heroGradient = Brush.linearGradient(listOf(Color(0xFFEEF4FF), Color(0xFFE6F1FB), Color(0xFFE1F5EE)))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // BoxWithConstraints so the sheet can be capped against the ACTUAL screen
        // height: with the discount fields shown the content is tall enough to
        // push the confirm button off-screen on a short device.
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val sheetMaxHeight = maxHeight * 0.92f
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = sheetMaxHeight),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column {
                    // Drag handle
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFDDE8F5), RoundedCornerShape(2.dp)))
                    }

                    // Hero
                    Box(modifier = Modifier.fillMaxWidth().background(heroGradient)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp).background(Color.White, RoundedCornerShape(18.dp)).border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(blueGradient),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(product.nameAr.firstOrNull()?.toString() ?: "؟", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                product.nameAr,
                                color = Color(0xFF1A2A3A), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(blueGradient).padding(horizontal = 14.dp, vertical = 4.dp),
                            ) {
                                Text("${selectedUnit.price.formatJod(AppLanguage.AR)} / ${selectedUnit.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                                .clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.88f))
                                .border(0.5.dp, Color(0xFFB5D4F4), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(product.sku, color = Color(0xFF185FA5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Body — scrolls on its own. weight(fill = false) keeps the sheet
                    // compact for short content instead of always filling the cap.
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Qty stepper
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp)).border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(Res.string.voucher_qty_required), color = Color(0xFF5A7399), fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(38.dp).background(Color.White, CircleShape).border(0.5.dp, Color(0xFFC8D8EC), CircleShape).clickable(enabled = qty > 1) { qty -= 1; qtyText = qty.toInt().toString() },
                                    contentAlignment = Alignment.Center,
                                ) { Text("−", color = Color(0xFF185FA5), fontSize = 22.sp, fontWeight = FontWeight.Medium) }
                                // Type-in quantity — digits only; empty → 0 (confirm stays disabled).
                                // Drawn as a bordered input box so it clearly reads as a text field.
                                BasicTextField(
                                    value = qtyText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(6)
                                        qtyText = digits
                                        qty = digits.toIntOrNull()?.toDouble() ?: 0.0
                                    },
                                    modifier = Modifier.width(88.dp),
                                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF185FA5), textAlign = TextAlign.Center),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(Color(0xFF185FA5)),
                                    decorationBox = { inner ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White, RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF9EC3EA), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { inner() }
                                    },
                                )
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                        .background(if (canIncrement) blueGradient else Brush.linearGradient(listOf(Color(0xFFC8D8EC), Color(0xFFC8D8EC))))
                                        .clickable(enabled = canIncrement) { qty += 1; qtyText = qty.toInt().toString() },
                                    contentAlignment = Alignment.Center,
                                ) { Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                            }
                        }

                        // Unit dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(14.dp)).clickable { unitDropdownExpanded = true }.padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("▾", color = Color(0xFF185FA5), fontSize = 18.sp)
                                Text(unitLabel(selectedUnit), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A2A3A))
                            }
                            DropdownMenu(expanded = unitDropdownExpanded, onDismissRequest = { unitDropdownExpanded = false }) {
                                effectiveUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unitLabel(unit), fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            Logger.withTag("UnitPrice").d(
                                                "unit changed ${selectedUnit.name}(price=${selectedUnit.price}) -> " +
                                                    "${unit.name}(conv=${unit.conversionQty}, price=${unit.price})",
                                            )
                                            selectedUnit = unit; unitDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDE8F5)))

                        // Stock check (SALE) — available van stock vs. requested base units
                        if (enforceStock) {
                            val warn = exceedsStock
                            val accent = if (warn) Color(0xFFE24B4A) else Color(0xFF1D9E75)
                            val bg = if (warn) Color(0xFFFFF0F0) else Color(0xFFEAF7F1)
                            val border = if (warn) Color(0xFFF7C1C1) else Color(0xFFBFE6D5)
                            val requestedLabel = if (requestedBase % 1.0 == 0.0)
                                requestedBase.toInt().toString() else requestedBase.toString()
                            Column(
                                modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(14.dp))
                                    .border(0.5.dp, border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(Res.string.voucher_stock_available_count, availableBase.toInt()),
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent,
                                    )
                                    Text(
                                        stringResource(Res.string.voucher_stock_required_units, requestedLabel),
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5A7399),
                                    )
                                }
                                if (warn) {
                                    Text(
                                        stringResource(Res.string.voucher_stock_exceeds_warning),
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE24B4A),
                                    )
                                }
                            }
                        }

                        // Unit price — editable only when the salesman has canEditPrice.
                        if (canEditPrice) {
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { v ->
                                    priceText = v.filter { it.isDigit() || it == '.' }.take(10)
                                    Logger.withTag("UnitPrice").d(
                                        "price edited unit=${selectedUnit.name} text='$priceText' " +
                                            "parsed=${priceText.toDoubleOrNull()} unitDefault=${selectedUnit.price}",
                                    )
                                },
                                label = { Text(stringResource(Res.string.voucher_unit_price)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text("د.أ", color = Color(0xFF185FA5), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fvFieldColors(),
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F8FC), RoundedCornerShape(14.dp)).border(0.5.dp, Color(0xFFDDE8F5), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(selectedUnit.price.formatJod(AppLanguage.AR), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF185FA5))
                                Text("· ${stringResource(Res.string.voucher_unit_price)}", fontSize = 13.sp, color = Color(0xFF5A7399))
                            }
                        }

                        // Line discount — hidden when the salesman lacks the discount permission.
                        if (canDiscount) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(10.dp))) {
                                    listOf(DiscountType.PERCENT to "%", DiscountType.VALUE to stringResource(Res.string.voucher_discount_type_value)).forEach { (t, label) ->
                                        val active = t == lineDiscountType
                                        Box(
                                            modifier = Modifier.clickable { if (!active) { lineDiscountType = t; discountText = "" } }.then(if (active) Modifier.background(blueGradient) else Modifier).padding(horizontal = 14.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(label, color = if (active) Color.White else Color(0xFF8A9AB0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                Text(stringResource(Res.string.voucher_line_discount), color = Color(0xFF8A9AB0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { v -> discountText = v.filter { it.isDigit() || it == '.' }.take(8) },
                                placeholder = { Text("0", color = Color(0xFFB0BEC5)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text(if (lineDiscountType == DiscountType.PERCENT) "%" else "د.أ", color = Color(0xFF185FA5), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fvFieldColors(),
                            )
                        }
                        }

                        // Total card
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(blueGradient).padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(lineTotal.formatJod(AppLanguage.AR), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(stringResource(Res.string.voucher_line_total), fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }

                    // Actions
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Delete removes THIS unit's line only — the item's other units stay.
                        if (currentLine != null) {
                            Box(
                                modifier = Modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF0F0)).border(0.5.dp, Color(0xFFF7C1C1), RoundedCornerShape(16.dp)).clickable { onDelete(selectedUnit.id) }.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(stringResource(Res.string.delete), color = Color(0xFFE24B4A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).border(0.5.dp, Color(0xFFC8D8EC), RoundedCornerShape(16.dp)).clickable { onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(Res.string.cancel), color = Color(0xFF5A7399), fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                        Box(
                            modifier = Modifier.weight(if (currentLine != null) 1.8f else 2f).height(52.dp).clip(RoundedCornerShape(16.dp))
                                .then(if (canConfirm) Modifier.background(greenGradient) else Modifier.background(Fv.SurfaceTop))
                                .clickable(enabled = canConfirm) {
                                    // Untouched offer-seeded field → keep the line's own manual
                                    // discount, so the offer isn't persisted as a manual one and
                                    // then added again by the engine (see [offerSeedText]).
                                    val confirmedDiscountPct =
                                        if (offerSeedText != null &&
                                            lineDiscountType == DiscountType.PERCENT &&
                                            discountText == offerSeedText
                                        ) currentLine?.discountPct ?: 0.0
                                        else discountPct
                                    Logger.withTag("UnitPrice").d(
                                        "confirm qty=$qty unit=${selectedUnit.name}/${selectedUnit.id} " +
                                            "conv=${selectedUnit.conversionQty} effectivePrice=$effectivePrice " +
                                            "discountPct=$confirmedDiscountPct lineTotal=$lineTotal",
                                    )
                                    onConfirm(qty, selectedUnit, effectivePrice, confirmedDiscountPct)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_cart),
                                    contentDescription = null,
                                    tint = if (canConfirm) Color.White else Fv.TextMid,
                                    modifier = Modifier.size(19.dp),
                                )
                                Text(
                                    if (currentLine == null) stringResource(Res.string.voucher_add_to_cart) else stringResource(Res.string.voucher_update_line),
                                    color = if (canConfirm) Color.White else Fv.TextMid,
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun unitLabel(unit: ProductUnit): String {
    val qty = if (unit.conversionQty % 1.0 == 0.0) unit.conversionQty.toInt().toString() else unit.conversionQty.toString()
    return if (unit.conversionQty == 1.0) unit.name else "${unit.name}  ×$qty"
}

// ── Payment Method Dialog (SALE only) ────────────────────────────────────────

@Composable
private fun PaymentMethodDialog(
    current: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sale_payment_method), color = Fv.TextHigh) },
        text = {
            Column {
                listOf(PaymentMethod.CASH to stringResource(Res.string.payment_method_cash), PaymentMethod.CREDIT to stringResource(Res.string.payment_method_credit)).forEach { (method, label) ->
                    val active = method == current
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(method) }
                            .background(if (active) Fv.Blue else Fv.SurfaceHigh, RoundedCornerShape(10.dp)).padding(12.dp),
                    ) {
                        Text(label, color = if (active) Color.White else Fv.TextMid, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.confirm), color = Fv.Green) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) } },
        containerColor = Fv.Surface,
    )
}

// ── RETURN: reference to original sale invoice ────────────────────────────────

@Composable
private fun ReturnReferenceBanner(referenceNumber: String?, onPick: () -> Unit) {
    val hasRef = referenceNumber != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hasRef) Color(0xFFEFF6FF) else Color(0xFFFFF4E5))
            .clickable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasRef) {
            Text(
                stringResource(Res.string.return_reference_label, referenceNumber!!),
                color = Fv.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(Res.string.return_change), color = Fv.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(stringResource(Res.string.return_source_pick), color = Fv.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("+", color = Fv.Amber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SourceInvoicePickerDialog(
    invoices: List<InvoiceEntity>,
    lookupQuery: String,
    isLookingUp: Boolean,
    onLookupChange: (String) -> Unit,
    onLookup: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.return_source_title), color = Fv.TextHigh) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Manual lookup: find a sale by number on the server when it isn't local.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = lookupQuery,
                        onValueChange = onLookupChange,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("ابحث برقم فاتورة البيع", color = Fv.TextMid, fontSize = 13.sp) },
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = onLookup,
                        enabled = !isLookingUp && lookupQuery.isNotBlank(),
                    ) {
                        Text(if (isLookingUp) "..." else "بحث", color = Fv.Blue, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (invoices.isEmpty()) {
                    Text(stringResource(Res.string.return_source_empty), color = Fv.TextMid)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(invoices) { inv ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Fv.SurfaceTop)
                                .clickable { onSelect(inv.id) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("#${inv.number}", color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(inv.total.formatJod(AppLanguage.AR), color = Fv.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(formatInvoiceDate(inv.createdAt), color = Fv.TextMid, fontSize = 11.sp)
                        }
                    }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = Fv.TextMid) } },
        containerColor = Fv.Surface,
    )
}

private fun formatInvoiceDate(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
        "${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
}

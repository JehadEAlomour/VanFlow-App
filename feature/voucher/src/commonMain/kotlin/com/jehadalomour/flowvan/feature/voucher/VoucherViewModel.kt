package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.LineTaxType
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.TaxType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.jehadalomour.flowvan.core.domain.usecase.CreateRequestVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateReturnVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.core.domain.usecase.StockShortageException
import com.jehadalomour.flowvan.feature.voucher.DiscountType
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoucherViewModel(
    private val customerId: String,
    private val type: VoucherType,
    customers: CustomerRepository,
    products: ProductRepository,
    private val productUnits: ProductUnitRepository,
    private val session: SessionStore,
    private val createSale: CreateSaleVoucherUseCase,
    private val createReturn: CreateReturnVoucherUseCase,
    private val createRequest: CreateRequestVoucherUseCase,
    private val appSettings: AppSettingsRepository,
    private val invoiceDao: InvoiceDao,
    private val json: Json,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoucherState(type = type, showSourcePicker = type == VoucherType.RETURN),
    )
    val state: StateFlow<VoucherState> = _state.asStateFlow()

    init {
        customers.observeById(customerId)
            .onEach { c -> _state.update { it.copy(customer = c) } }
            .launchIn(viewModelScope)

        products.observeAll()
            .onEach { list -> _state.update { it.copy(products = list) }; applySearch() }
            .launchIn(viewModelScope)

        productUnits.observeAll()
            .onEach { units ->
                _state.update { it.copy(productUnits = units.groupBy { u -> u.productId }) }
            }
            .launchIn(viewModelScope)

        appSettings.observe()
            .onEach { settings ->
                val lineTaxType = settings.taxType.toLineTaxType()
                _state.update { s ->
                    // Also re-stamp existing cart lines so live totals stay in sync
                    s.copy(
                        taxType = lineTaxType,
                        cart = s.cart.map { it.copy(lineTaxType = lineTaxType) },
                    )
                }
            }
            .launchIn(viewModelScope)

        // RETURN: offer the customer's confirmed sale invoices as return sources.
        if (type == VoucherType.RETURN) {
            invoiceDao.observeByCustomerAndType(customerId, "SALE")
                .onEach { sales ->
                    _state.update { it.copy(sourceInvoices = sales.filter { inv -> inv.status != "CANCELLED" }) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onEvent(event: VoucherEvent) {
        when (event) {
            is VoucherEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.q) }
                applySearch()
            }
            is VoucherEvent.StepItem -> stepItem(event.product, event.delta)
            is VoucherEvent.ConfirmItemDialog -> confirmDialog(event)
            is VoucherEvent.ChangeQty -> changeQty(event.productId, event.qty)
            is VoucherEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.productId == event.productId })
            }
            is VoucherEvent.PaymentMethodSelected -> _state.update { it.copy(paymentMethod = event.method) }
            is VoucherEvent.NotesChanged -> _state.update { it.copy(notes = event.notes) }
            is VoucherEvent.VoucherDiscountInputChanged -> _state.update { it.copy(voucherDiscountInput = event.input) }
            VoucherEvent.VoucherDiscountTypeToggled -> _state.update {
                it.copy(
                    voucherDiscountType = if (it.voucherDiscountType == DiscountType.PERCENT) DiscountType.VALUE else DiscountType.PERCENT,
                    voucherDiscountInput = "",
                )
            }
            is VoucherEvent.ReasonSelected -> _state.update { it.copy(reason = event.reason) }
            VoucherEvent.ToggleView -> _state.update {
                it.copy(view = if (it.view == VoucherView.PICKER) VoucherView.CART else VoucherView.PICKER)
            }
            VoucherEvent.Save -> {
                val s = _state.value
                when {
                    type == VoucherType.RETURN && s.referenceInvoiceId == null ->
                        _state.update { it.copy(errorAr = "اختر فاتورة البيع المرجعية أولاً", showSourcePicker = true) }
                    s.cart.isEmpty() -> _state.update { it.copy(errorAr = "السلة فارغة") }
                    type == VoucherType.RETURN && s.reason == null ->
                        _state.update { it.copy(errorAr = "اختر سبب الإرجاع") }
                    s.canSave -> _state.update { it.copy(showSaveSheet = true) }
                }
            }
            VoucherEvent.ConfirmSave -> save()
            VoucherEvent.DismissSaveSheet -> _state.update { it.copy(showSaveSheet = false) }
            VoucherEvent.DismissError -> _state.update { it.copy(errorAr = null) }

            VoucherEvent.OpenSourcePicker -> _state.update { it.copy(showSourcePicker = true) }
            VoucherEvent.DismissSourcePicker -> _state.update { it.copy(showSourcePicker = false) }
            is VoucherEvent.SelectSourceInvoice -> selectSourceInvoice(event.invoiceId)
        }
    }

    /** Pre-fill the return cart from a chosen sale invoice — same items and quantities. */
    private fun selectSourceInvoice(invoiceId: String) {
        _state.update { s ->
            val invoice = s.sourceInvoices.firstOrNull { it.id == invoiceId } ?: return@update s
            val lines = runCatching {
                json.decodeFromString<List<InvoiceLine>>(invoice.linesJson)
            }.getOrDefault(emptyList())

            fun conversionFor(line: InvoiceLine): Double =
                s.productUnits[line.productId]?.firstOrNull { it.name == line.unit }?.conversionQty ?: 1.0

            val cart = lines.map { line ->
                CartLine(
                    productId = line.productId,
                    sku = line.sku,
                    nameAr = line.nameAr,
                    unitPrice = line.unitPrice,
                    qty = line.qty,
                    discountPct = line.discountPct,
                    unit = line.unit,
                    unitConversionQty = conversionFor(line),
                    taxRate = line.taxRate,
                    lineTaxType = runCatching { LineTaxType.valueOf(line.taxType) }.getOrDefault(s.taxType),
                )
            }
            val sold = lines.associate { it.productId to it.qty * conversionFor(it) }

            s.copy(
                cart = cart,
                referenceInvoiceId = invoice.id,
                referenceNumber = invoice.number,
                soldQtyByProduct = sold,
                showSourcePicker = false,
                view = VoucherView.CART,
            )
        }
    }

    /** RETURN can't exceed what was sold on the source invoice (compared in base units). */
    private fun capReturnQty(state: VoucherState, productId: String, unitConversionQty: Double, qty: Double): Double {
        if (!state.requiresSourceInvoice) return qty
        val soldBase = state.soldQtyByProduct[productId] ?: return qty
        if (unitConversionQty <= 0.0) return qty
        return qty.coerceAtMost(soldBase / unitConversionQty)
    }

    private fun applySearch() {
        val q = _state.value.searchQuery.trim().lowercase()
        val filtered = if (q.isEmpty()) _state.value.products else _state.value.products.filter {
            it.nameAr.lowercase().contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
        _state.update { it.copy(visibleProducts = filtered) }
    }

    private fun stepItem(product: Product, delta: Int) {
        _state.update { s ->
            val existing = s.cart.firstOrNull { it.productId == product.id }
            val newCart = when {
                existing == null && delta > 0 -> {
                    val defaultUnit = s.productUnits[product.id]?.minByOrNull { it.conversionQty }
                    s.cart + CartLine(
                        productId = product.id,
                        sku = product.sku,
                        nameAr = product.nameAr,
                        unitPrice = defaultUnit?.price ?: product.salePrice,
                        qty = 1.0,
                        unit = defaultUnit?.name ?: product.unit,
                        unitConversionQty = defaultUnit?.conversionQty ?: 1.0,
                        taxRate = product.taxRate,
                        lineTaxType = s.taxType,
                    )
                }
                existing == null -> s.cart
                (existing.qty + delta) <= 0 -> s.cart.filterNot { it.productId == product.id }
                else -> s.cart.map { if (it.productId == product.id) it.copy(qty = it.qty + delta) else it }
            }
            s.copy(cart = newCart)
        }
    }

    private fun confirmDialog(event: VoucherEvent.ConfirmItemDialog) {
        _state.update { s ->
            val qty = capReturnQty(s, event.product.id, event.unitConversionQty, event.qty)
            val existing = s.cart.firstOrNull { it.productId == event.product.id }
            val newCart = when {
                qty <= 0 -> s.cart.filterNot { it.productId == event.product.id }
                existing == null -> s.cart + CartLine(
                    productId = event.product.id,
                    sku = event.product.sku,
                    nameAr = event.product.nameAr,
                    unitPrice = event.unitPrice,
                    qty = qty,
                    unit = event.unit,
                    unitConversionQty = event.unitConversionQty,
                    discountPct = event.discountPct,
                    taxRate = event.product.taxRate,
                    lineTaxType = s.taxType,
                )
                else -> s.cart.map {
                    if (it.productId == event.product.id)
                        it.copy(
                            qty = qty,
                            unit = event.unit,
                            unitPrice = event.unitPrice,
                            unitConversionQty = event.unitConversionQty,
                            discountPct = event.discountPct,
                        )
                    else it
                }
            }
            s.copy(cart = newCart)
        }
    }

    private fun changeQty(productId: String, qty: Double) {
        _state.update { s ->
            val newCart = if (qty <= 0.0) s.cart.filterNot { it.productId == productId }
            else s.cart.map { if (it.productId == productId) it.copy(qty = qty) else it }
            s.copy(cart = newCart)
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(isSaving = true, showSaveSheet = false) }
        viewModelScope.launch {
            val salesmanId = session.currentUserId.orEmpty()
            val result = when (type) {
                VoucherType.SALE -> createSale(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    discountAmount = s.voucherDiscountAmount,
                    paymentMethod = s.paymentMethod,
                    notes = s.notes.takeIf { it.isNotBlank() },
                )
                VoucherType.RETURN -> createReturn(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    reason = s.reason!!.labelAr,
                    extraNotes = s.notes.takeIf { it.isNotBlank() },
                    referenceInvoiceId = s.referenceInvoiceId,
                    referenceNumber = s.referenceNumber,
                )
                VoucherType.ORDER -> createRequest(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    expectedDeliveryAt = s.deliveryDate,
                    notes = s.notes.takeIf { it.isNotBlank() },
                )
            }
            result.fold(
                onSuccess = { entity ->
                    _state.update { it.copy(isSaving = false, savedNumber = entity.number, savedId = entity.id) }
                },
                onFailure = { ex ->
                    val msg = when (ex) {
                        is StockShortageException ->
                            "الكمية غير متوفرة في الفان (${ex.available} متاح من ${ex.requested})"
                        is EmptyCartException -> "السلة فارغة"
                        else -> "حدث خطأ غير متوقع"
                    }
                    _state.update { it.copy(isSaving = false, errorAr = msg) }
                },
            )
        }
    }
}

/** Maps the global settings tax treatment to the per-line calculator type. */
private fun TaxType.toLineTaxType(): LineTaxType = when (this) {
    TaxType.EXCLUDED_TAX -> LineTaxType.TAXABLE
    TaxType.INCLUDED_TAX -> LineTaxType.INCLUSIVE
}

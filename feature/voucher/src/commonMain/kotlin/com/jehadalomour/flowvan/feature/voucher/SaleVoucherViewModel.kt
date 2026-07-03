package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.core.domain.usecase.StockShortageException
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SaleVoucherViewModel(
    private val customerId: String,
    customers: CustomerRepository,
    products: ProductRepository,
    private val productUnits: ProductUnitRepository,
    private val session: SessionStore,
    private val createSale: CreateSaleVoucherUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SaleVoucherState())
    val state: StateFlow<SaleVoucherState> = _state.asStateFlow()

    init {
        customers.observeById(customerId)
            .onEach { c -> _state.update { it.copy(customer = c) } }
            .launchIn(viewModelScope)

        products.observeAll()
            .onEach { list ->
                _state.update { it.copy(products = list) }
                applySearch()
            }
            .launchIn(viewModelScope)

        productUnits.observeAll()
            .onEach { units ->
                _state.update { it.copy(productUnits = units.groupBy { u -> u.productId }) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SaleVoucherEvent) {
        when (event) {
            is SaleVoucherEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.q) }
                applySearch()
            }
            is SaleVoucherEvent.StepItem -> stepItem(event.product, event.delta)
            is SaleVoucherEvent.ConfirmItemDialog -> confirmDialog(event)
            is SaleVoucherEvent.ChangeQty -> changeQty(event.productId, event.qty)
            is SaleVoucherEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.productId == event.productId })
            }
            is SaleVoucherEvent.PaymentMethodSelected -> _state.update { it.copy(paymentMethod = event.method) }
            is SaleVoucherEvent.NotesChanged -> _state.update { it.copy(notes = event.notes) }
            is SaleVoucherEvent.VoucherDiscountInputChanged -> _state.update { it.copy(voucherDiscountInput = event.input) }
            SaleVoucherEvent.VoucherDiscountTypeToggled -> _state.update {
                it.copy(
                    voucherDiscountType = if (it.voucherDiscountType == DiscountType.PERCENT) DiscountType.VALUE else DiscountType.PERCENT,
                    voucherDiscountInput = "",
                )
            }
            SaleVoucherEvent.ToggleView -> _state.update {
                it.copy(view = if (it.view == VoucherView.PICKER) VoucherView.CART else VoucherView.PICKER)
            }
            SaleVoucherEvent.OpenSaveSheet -> _state.update { it.copy(showSaveSheet = true) }
            SaleVoucherEvent.DismissSaveSheet -> _state.update { it.copy(showSaveSheet = false) }
            SaleVoucherEvent.ConfirmSave -> save()
            SaleVoucherEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
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
                    )
                }
                existing == null -> s.cart
                (existing.qty + delta) <= 0 -> s.cart.filterNot { it.productId == product.id }
                else -> s.cart.map { if (it.productId == product.id) it.copy(qty = it.qty + delta) else it }
            }
            s.copy(cart = newCart)
        }
    }

    private fun confirmDialog(event: SaleVoucherEvent.ConfirmItemDialog) {
        _state.update { s ->
            val existing = s.cart.firstOrNull { it.productId == event.product.id }
            val newCart = when {
                event.qty <= 0 -> s.cart.filterNot { it.productId == event.product.id }
                existing == null -> s.cart + CartLine(
                    productId = event.product.id,
                    sku = event.product.sku,
                    nameAr = event.product.nameAr,
                    unitPrice = event.unitPrice,
                    qty = event.qty,
                    unit = event.unit,
                    unitConversionQty = event.unitConversionQty,
                    discountPct = event.discountPct,
                )
                else -> s.cart.map {
                    if (it.productId == event.product.id)
                        it.copy(
                            qty = event.qty,
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
        if (s.cart.isEmpty()) {
            viewModelScope.launch {
                val msg = getString(Res.string.err_cart_empty)
                _state.update { it.copy(errorAr = msg, showSaveSheet = false) }
            }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = createSale(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                cart = s.cart,
                discountAmount = s.voucherDiscountAmount,
                paymentMethod = s.paymentMethod,
                notes = s.notes.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { entity ->
                    _state.update {
                        it.copy(isSaving = false, showSaveSheet = false, savedNumber = entity.number)
                    }
                },
                onFailure = { ex ->
                    val msg = when (ex) {
                        is StockShortageException ->
                            getString(Res.string.err_stock_unavailable, ex.available, ex.requested)
                        is EmptyCartException -> getString(Res.string.err_cart_empty)
                        else -> getString(Res.string.err_unexpected)
                    }
                    _state.update { it.copy(isSaving = false, showSaveSheet = false, errorAr = msg) }
                },
            )
        }
    }
}

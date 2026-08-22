package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.common.search.matchesTokenSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.domain.usecase.CreateRequestVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RequestVoucherViewModel(
    private val customerId: String,
    customers: CustomerRepository,
    products: ProductRepository,
    private val session: SessionStore,
    private val createRequest: CreateRequestVoucherUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RequestVoucherState())
    val state: StateFlow<RequestVoucherState> = _state.asStateFlow()

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
    }

    fun onEvent(event: RequestVoucherEvent) {
        when (event) {
            is RequestVoucherEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.q) }; applySearch()
            }
            is RequestVoucherEvent.AddToCart -> addOrIncrement(event.product)
            is RequestVoucherEvent.ChangeQty -> changeQty(event.productId, event.unitId, event.qty)
            is RequestVoucherEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.isLine(event.productId, event.unitId) })
            }
            is RequestVoucherEvent.ExpectedDateChanged ->
                _state.update { it.copy(expectedDeliveryAt = event.epochMillis) }
            is RequestVoucherEvent.NotesChanged ->
                _state.update { it.copy(notes = event.notes) }
            RequestVoucherEvent.ToggleView -> _state.update {
                it.copy(view = if (it.view == VoucherView.PICKER) VoucherView.CART else VoucherView.PICKER)
            }
            RequestVoucherEvent.Save -> save()
            RequestVoucherEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
    }

    private fun applySearch() {
        val filtered = _state.value.products.filter {
            matchesTokenSearch(_state.value.searchQuery, it.nameAr, it.nameEn, it.sku)
        }
        _state.update { it.copy(visibleProducts = filtered) }
    }

    // No unit picker on this screen — every line it makes is the item's base pool
    // (unitId ""), but it still matches on (productId, unitId) so it can never collide
    // with a variant line built elsewhere from the same item.
    private fun addOrIncrement(product: Product) {
        _state.update { s ->
            val existing = s.cart.firstOrNull { it.isLine(product.id, "") }
            val newCart = if (existing == null) {
                s.cart + CartLine(product.id, product.sku, product.nameAr, product.salePrice, qty = 1.0)
            } else {
                s.cart.map { if (it.isLine(product.id, "")) it.copy(qty = it.qty + 1) else it }
            }
            s.copy(cart = newCart)
        }
    }

    private fun changeQty(productId: String, unitId: String, qty: Double) {
        _state.update { s ->
            val newCart = if (qty <= 0.0) s.cart.filterNot { it.isLine(productId, unitId) }
            else s.cart.map { if (it.isLine(productId, unitId)) it.copy(qty = qty) else it }
            s.copy(cart = newCart)
        }
    }

    private fun save() {
        val s = _state.value
        if (s.cart.isEmpty()) {
            _state.update { it.copy(errorAr = "السلة فارغة") }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = createRequest(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                cart = s.cart,
                expectedDeliveryAt = s.expectedDeliveryAt,
                notes = s.notes.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { entity ->
                    _state.update { it.copy(isSaving = false, savedNumber = entity.number) }
                },
                onFailure = { ex ->
                    val msg = if (ex is EmptyCartException) "السلة فارغة" else "حدث خطأ غير متوقع"
                    _state.update { it.copy(isSaving = false, errorAr = msg) }
                },
            )
        }
    }
}

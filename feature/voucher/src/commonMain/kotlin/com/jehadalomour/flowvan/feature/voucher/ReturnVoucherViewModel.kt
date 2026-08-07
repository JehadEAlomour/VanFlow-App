package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.domain.usecase.CreateReturnVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.feature.voucher.VoucherView
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

class ReturnVoucherViewModel(
    private val customerId: String,
    customers: CustomerRepository,
    products: ProductRepository,
    private val session: SessionStore,
    private val createReturn: CreateReturnVoucherUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnVoucherState())
    val state: StateFlow<ReturnVoucherState> = _state.asStateFlow()

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

    fun onEvent(event: ReturnVoucherEvent) {
        when (event) {
            is ReturnVoucherEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.q) }; applySearch()
            }
            is ReturnVoucherEvent.AddToCart -> addOrIncrement(event.product)
            is ReturnVoucherEvent.ChangeQty -> changeQty(event.productId, event.unitId, event.qty)
            is ReturnVoucherEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.isLine(event.productId, event.unitId) })
            }
            is ReturnVoucherEvent.ReasonSelected -> _state.update { it.copy(reason = event.reason) }
            is ReturnVoucherEvent.NotesChanged -> _state.update { it.copy(notes = event.notes) }
            ReturnVoucherEvent.ToggleView -> _state.update {
                it.copy(view = if (it.view == VoucherView.PICKER) VoucherView.CART else VoucherView.PICKER)
            }
            ReturnVoucherEvent.OpenSaveSheet -> {
                if (_state.value.reason == null) {
                    viewModelScope.launch {
                        val msg = getString(Res.string.err_select_return_reason)
                        _state.update { it.copy(errorAr = msg) }
                    }
                } else {
                    _state.update { it.copy(showSaveSheet = true) }
                }
            }
            ReturnVoucherEvent.DismissSaveSheet -> _state.update { it.copy(showSaveSheet = false) }
            ReturnVoucherEvent.ConfirmSave -> save()
            ReturnVoucherEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
    }

    private fun applySearch() {
        val q = _state.value.searchQuery.trim().lowercase()
        val filtered = if (q.isEmpty()) _state.value.products else _state.value.products.filter {
            it.nameAr.lowercase().contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.sku.lowercase().contains(q)
        }
        _state.update { it.copy(visibleProducts = filtered) }
    }

    // This screen has no unit picker, so every line it makes is the item's base pool
    // (unitId ""). It still matches on (productId, unitId) so it can never collide with
    // a variant line built elsewhere from the same item.
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
        val reason = s.reason ?: return
        if (s.cart.isEmpty()) {
            viewModelScope.launch {
                val msg = getString(Res.string.err_cart_empty)
                _state.update { it.copy(errorAr = msg, showSaveSheet = false) }
            }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = createReturn(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                cart = s.cart,
                reason = reason.labelAr,
                // Legacy path (not the active return flow) — keep the prior credit-note behaviour.
                paymentMethod = com.jehadalomour.flowvan.core.model.PaymentMethod.CREDIT,
                extraNotes = s.notes.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { entity ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            showSaveSheet = false,
                            savedNumber = entity.number,
                        )
                    }
                },
                onFailure = { ex ->
                    val msg = if (ex is EmptyCartException) getString(Res.string.err_cart_empty) else getString(Res.string.err_unexpected)
                    _state.update { it.copy(isSaving = false, showSaveSheet = false, errorAr = msg) }
                },
            )
        }
    }
}

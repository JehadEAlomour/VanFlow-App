package com.jehadalomour.flowvan.shared.presentation.feature.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.Product
import com.jehadalomour.flowvan.shared.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.shared.domain.usecase.StockShortageException
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
    }

    fun onEvent(event: SaleVoucherEvent) {
        when (event) {
            is SaleVoucherEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.q) }
                applySearch()
            }
            is SaleVoucherEvent.AddToCart -> addOrIncrement(event.product)
            is SaleVoucherEvent.ChangeQty -> changeQty(event.productId, event.qty)
            is SaleVoucherEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.productId == event.productId })
            }
            is SaleVoucherEvent.DiscountChanged -> _state.update { it.copy(discountAmount = event.amount.coerceAtLeast(0.0)) }
            is SaleVoucherEvent.PaymentMethodSelected -> _state.update { it.copy(paymentMethod = event.method) }
            is SaleVoucherEvent.NotesChanged -> _state.update { it.copy(notes = event.notes) }
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

    private fun addOrIncrement(product: Product) {
        _state.update { s ->
            val existing = s.cart.firstOrNull { it.productId == product.id }
            val newCart = if (existing == null) {
                s.cart + CartLine(
                    productId = product.id,
                    sku = product.sku,
                    nameAr = product.nameAr,
                    unitPrice = product.salePrice,
                    qty = 1.0,
                )
            } else {
                s.cart.map { if (it.productId == product.id) it.copy(qty = it.qty + 1) else it }
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
            _state.update { it.copy(errorAr = "السلة فارغة", showSaveSheet = false) }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = createSale(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                cart = s.cart,
                discountAmount = s.discountAmount,
                paymentMethod = s.paymentMethod,
                notes = s.notes.takeIf { it.isNotBlank() },
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
                    val msg = when (ex) {
                        is StockShortageException ->
                            "الكمية غير متوفرة في الفان (${ex.available} متاح من ${ex.requested})"
                        is EmptyCartException -> "السلة فارغة"
                        else -> "حدث خطأ غير متوقع"
                    }
                    _state.update { it.copy(isSaving = false, showSaveSheet = false, errorAr = msg) }
                },
            )
        }
    }
}

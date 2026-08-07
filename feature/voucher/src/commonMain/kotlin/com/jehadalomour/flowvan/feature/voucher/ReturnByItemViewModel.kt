package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.network.api.ReturnByItemApi
import com.jehadalomour.flowvan.core.network.dto.ConfirmReturnRequest
import com.jehadalomour.flowvan.core.network.dto.ReturnPreviewRequest
import com.jehadalomour.flowvan.core.network.dto.ReturnRequestLineDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Return by item on the van. See ReturnByItemContract for why there is no
 * strategy picker.
 */
class ReturnByItemViewModel(
    private val api: ReturnByItemApi,
    private val products: ProductRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnByItemState())
    val state: StateFlow<ReturnByItemState> = _state.asStateFlow()

    init {
        // Observed, not fetched once: the van's catalogue changes under a sync
        // while the rep is mid-return.
        viewModelScope.launch {
            products.observeAll().collect { list ->
                _state.update { it.copy(products = list) }
            }
        }
    }

    fun setCustomer(number: String?, name: String?) {
        _state.update { it.copy(customerNumber = number, customerName = name) }
    }

    fun onEvent(event: ReturnByItemEvent) {
        when (event) {
            ReturnByItemEvent.OpenPicker -> _state.update { it.copy(isPickerOpen = true) }
            ReturnByItemEvent.ClosePicker ->
                _state.update { it.copy(isPickerOpen = false, searchQuery = "") }
            is ReturnByItemEvent.SearchChanged ->
                _state.update { it.copy(searchQuery = event.v) }

            is ReturnByItemEvent.AddProduct -> addProduct(event.product)

            is ReturnByItemEvent.QuantityChanged -> _state.update { s ->
                // Any edit invalidates the shown match: the plan on screen was
                // computed for the OLD quantities, and confirming against it
                // would create documents the rep never saw.
                s.copy(
                    lines = s.lines.mapIndexed { i, l ->
                        if (i == event.index) l.copy(quantity = event.v.filter { c ->
                            c.isDigit() || c == '.'
                        }) else l
                    },
                    plan = null,
                )
            }

            is ReturnByItemEvent.UnitChanged -> _state.update { s ->
                s.copy(
                    lines = s.lines.mapIndexed { i, l ->
                        if (i == event.index) {
                            l.copy(itemUnitId = event.itemUnitId, unitLabel = event.label)
                        } else {
                            l
                        }
                    },
                    plan = null,
                )
            }

            is ReturnByItemEvent.RemoveLine -> _state.update { s ->
                s.copy(lines = s.lines.filterIndexed { i, _ -> i != event.index }, plan = null)
            }

            ReturnByItemEvent.Preview -> preview()
            ReturnByItemEvent.Confirm -> confirm()
            ReturnByItemEvent.DismissError -> _state.update { it.copy(errorAr = null) }
        }
    }

    private fun addProduct(product: Product) {
        _state.update { s ->
            // Same item twice would be two request lines competing for the same
            // units; the allocator handles it, but the rep sees two rows for one
            // thing. Bump the existing row instead.
            val existing = s.lines.indexOfFirst { it.product.sku == product.sku }
            val lines = if (existing >= 0) {
                s.lines.mapIndexed { i, l ->
                    if (i == existing) {
                        l.copy(quantity = ((l.qtyOrZero) + 1).toString().removeSuffix(".0"))
                    } else {
                        l
                    }
                }
            } else {
                s.lines + ReturnByItemLine(product = product, quantity = "1")
            }
            s.copy(lines = lines, isPickerOpen = false, searchQuery = "", plan = null)
        }
    }

    private fun requestLines(): List<ReturnRequestLineDto> =
        _state.value.lines
            .filter { it.qtyOrZero > 0 }
            .map {
                ReturnRequestLineDto(
                    itemNumber = it.product.sku,
                    itemUnitId = it.itemUnitId,
                    quantity = it.qtyOrZero,
                )
            }

    private fun preview() {
        val lines = requestLines()
        if (lines.isEmpty()) return
        _state.update { it.copy(isPreviewing = true, errorAr = null) }
        viewModelScope.launch {
            runCatching {
                api.preview(
                    ReturnPreviewRequest(
                        lines = lines,
                        customerNumber = _state.value.customerNumber,
                        userCode = session.currentUserCode,
                    ),
                )
            }.fold(
                onSuccess = { res ->
                    _state.update { it.copy(isPreviewing = false, plan = res.plan) }
                },
                onFailure = {
                    _state.update { it.copy(isPreviewing = false, errorAr = ERR_PREVIEW) }
                },
            )
        }
    }

    private fun confirm() {
        val lines = requestLines()
        val userCode = session.currentUserCode
        if (lines.isEmpty() || userCode.isNullOrBlank()) {
            _state.update { it.copy(errorAr = ERR_NO_USER) }
            return
        }
        _state.update { it.copy(isConfirming = true, errorAr = null) }
        viewModelScope.launch {
            runCatching {
                // The REQUEST again, not the plan on screen — the server
                // re-allocates and re-checks under lock, so units returned by
                // someone else while the rep talked cannot be credited twice.
                api.confirm(
                    ConfirmReturnRequest(
                        lines = lines,
                        customerNumber = _state.value.customerNumber,
                        userCode = userCode,
                        confirmUserCode = userCode,
                    ),
                )
            }.fold(
                onSuccess = { res ->
                    _state.update {
                        it.copy(isConfirming = false, createdVouchers = res.vouchers)
                    }
                },
                onFailure = {
                    _state.update { it.copy(isConfirming = false, errorAr = ERR_CONFIRM) }
                },
            )
        }
    }

    private companion object {
        const val ERR_PREVIEW = "تعذّرت مطابقة الإرجاع. تحقق من الاتصال وحاول مرة أخرى."
        const val ERR_CONFIRM =
            "تعذّر إنشاء سندات الإرجاع. قد تكون الكميات أُرجعت بالفعل — أعد المطابقة."
        const val ERR_NO_USER = "لا يوجد كود مندوب في الجلسة."
    }
}

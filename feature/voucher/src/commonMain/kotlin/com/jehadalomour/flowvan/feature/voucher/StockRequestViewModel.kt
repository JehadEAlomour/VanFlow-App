package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.network.api.StockRequestApi
import com.jehadalomour.flowvan.core.network.dto.CreateStockRequestBody
import com.jehadalomour.flowvan.core.network.dto.StockRequestLineRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Requesting stock for the van, on the voucher cart pattern.
 *
 * See StockRequestContract for why this reuses [CartLine] and why an approved
 * request stays on the screen until the rep confirms receipt.
 */
class StockRequestViewModel(
    private val api: StockRequestApi,
    private val products: ProductRepository,
    private val units: ProductUnitRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockRequestState())
    val state: StateFlow<StockRequestState> = _state.asStateFlow()

    init {
        // Observed rather than fetched once: a sync can land mid-request, and the
        // van stock shown per row is the number the rep is deciding against.
        viewModelScope.launch {
            products.observeAll().collect { list ->
                _state.update { it.copy(products = list, visibleProducts = filter(list, it.searchQuery)) }
            }
        }
        viewModelScope.launch {
            units.observeAll().collect { list ->
                _state.update { it.copy(productUnits = list.groupBy { u -> u.productId }) }
            }
        }
        refreshMine()
    }

    fun onEvent(event: StockRequestEvent) {
        when (event) {
            StockRequestEvent.ToggleView -> _state.update {
                it.copy(
                    view = if (it.view == StockRequestView.PICKER) {
                        StockRequestView.CART
                    } else {
                        StockRequestView.PICKER
                    },
                )
            }

            is StockRequestEvent.SearchChanged -> _state.update {
                it.copy(searchQuery = event.v, visibleProducts = filter(it.products, event.v))
            }

            is StockRequestEvent.ConfirmItem -> confirmItem(event.product, event.qty, event.unit)

            is StockRequestEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.isLine(event.productId, event.unitId) })
            }

            StockRequestEvent.ClearCart -> _state.update { it.copy(cart = emptyList()) }

            is StockRequestEvent.NoteChanged -> _state.update { it.copy(note = event.v) }

            StockRequestEvent.Submit -> submit()
            is StockRequestEvent.Cancel -> act(event.id) { api.cancel(it) }
            is StockRequestEvent.Receive -> act(event.id, RECEIVED) { api.receive(it) }
            StockRequestEvent.Refresh -> refreshMine()
            StockRequestEvent.DismissError ->
                _state.update { it.copy(errorAr = null, noticeAr = null) }
        }
    }

    private fun filter(all: List<Product>, q: String): List<Product> =
        if (q.isBlank()) {
            all
        } else {
            all.filter {
                it.nameAr.contains(q, true) ||
                    it.nameEn.contains(q, true) ||
                    it.sku.contains(q, true)
            }
        }

    /**
     * Add the line, or replace the one already there for this (product, unit).
     *
     * Keyed on the pair, not the product: 3 cartons plus 10 loose pieces of one
     * item are two lines drawing on two different pools, and collapsing them by
     * product would silently drop whichever the rep entered first.
     */
    private fun confirmItem(product: Product, qty: Double, unit: ProductUnit) {
        if (qty <= 0) {
            // A zero on an existing line means "take it off", which is what the
            // rep expects from typing 0 rather than hunting for a delete.
            _state.update { s -> s.copy(cart = s.cart.filterNot { it.isLine(product.id, unit.id) }) }
            return
        }
        _state.update { s ->
            val line = CartLine(
                productId = product.id,
                sku = product.sku,
                nameAr = product.nameAr,
                // No money on a stock request. Left at zero rather than carrying
                // the sale price, so nothing downstream can read a value here and
                // present a request as if it were worth something.
                unitPrice = 0.0,
                qty = qty,
                unit = unit.name,
                unitId = unit.id,
                unitConversionQty = unit.conversionQty.takeIf { it > 0.0 } ?: 1.0,
                imageUrl = product.imageUrl,
            )
            val existing = s.cart.indexOfFirst { it.isLine(product.id, unit.id) }
            s.copy(
                cart = if (existing >= 0) {
                    s.cart.toMutableList().also { it[existing] = line }
                } else {
                    s.cart + line
                },
            )
        }
    }

    private fun submit() {
        val s = _state.value
        if (s.cart.isEmpty()) return
        _state.update { it.copy(isSubmitting = true, errorAr = null, noticeAr = null) }
        viewModelScope.launch {
            runCatching {
                api.create(
                    CreateStockRequestBody(
                        items = s.cart.map { line -> lineRequest(line) },
                        note = s.note.trim().takeIf { it.isNotBlank() },
                    ),
                )
            }.fold(
                onSuccess = { created ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            // Clear the cart, keep the screen and drop back to the
                            // picker: the answer arrives in the list below, and the
                            // rep is usually about to add a second request anyway.
                            cart = emptyList(),
                            note = "",
                            view = StockRequestView.PICKER,
                            mine = listOf(created) + it.mine,
                            noticeAr = "أُرسل الطلب ${created.requestNumber} إلى الإدارة.",
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isSubmitting = false, errorAr = ERR_SUBMIT) }
                },
            )
        }
    }

    /**
     * One cart line as the server wants it.
     *
     * `stockUnitCode` is the pool and must be "" for anything that is not a
     * variant: a packaging unit (كرتونة ×12) converts INTO the item's base pool,
     * and naming it as its own pool would receive the goods somewhere the sale
     * path never looks. The flag lives on ProductUnit, not on CartLine, so it is
     * resolved back through the units map here rather than carried on the line.
     */
    private fun lineRequest(line: CartLine): StockRequestLineRequest {
        val unit = _state.value.productUnits[line.productId]?.firstOrNull { it.id == line.unitId }
        return StockRequestLineRequest(
            itemNumber = line.sku,
            stockUnitCode = if (unit?.isStockUnit == true) unit.code else "",
            qtyOfUnit = line.qty,
            unitBaseQty = line.unitConversionQty.toInt().coerceAtLeast(1),
            // Blank is the synthesized fallback unit, not a real item_units row —
            // posting a made-up id would be rejected, so it travels as null.
            itemUnitId = line.unitId.takeIf { it.isNotBlank() },
            unitName = line.unit.takeIf { it.isNotBlank() },
        )
    }

    private fun refreshMine() {
        _state.update { it.copy(isLoadingMine = true) }
        viewModelScope.launch {
            runCatching { api.mine() }.fold(
                onSuccess = { list -> _state.update { it.copy(isLoadingMine = false, mine = list) } },
                onFailure = {
                    // Silent: a rep with no signal must still be able to BUILD a
                    // request. Only the send needs the network, and that failure is
                    // reported where it happens.
                    _state.update { it.copy(isLoadingMine = false) }
                },
            )
        }
    }

    /** One request-scoped call, with its own busy flag so each row spins alone. */
    private fun act(id: String, notice: String? = null, call: suspend (String) -> Any) {
        if (id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + id, errorAr = null) }
        viewModelScope.launch {
            runCatching { call(id) }.fold(
                onSuccess = {
                    _state.update { s -> s.copy(busyIds = s.busyIds - id, noticeAr = notice) }
                    // Re-read rather than patching the row: receiving posts a
                    // transfer, and only the server knows the voucher number.
                    refreshMine()
                },
                onFailure = {
                    _state.update { s -> s.copy(busyIds = s.busyIds - id, errorAr = ERR_ACTION) }
                },
            )
        }
    }

    private companion object {
        const val ERR_SUBMIT = "تعذّر إرسال الطلب. تحقق من الاتصال وحاول مرة أخرى."
        const val ERR_ACTION = "تعذّر تنفيذ العملية. حاول مرة أخرى."
        const val RECEIVED = "تم استلام البضاعة وتحديث مخزون المركبة."
    }
}

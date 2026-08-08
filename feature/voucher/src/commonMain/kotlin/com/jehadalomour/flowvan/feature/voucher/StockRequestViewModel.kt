package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
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
 * Requesting stock for the van. See StockRequestContract for why an approved
 * request stays on this screen until the rep confirms receipt.
 */
class StockRequestViewModel(
    private val api: StockRequestApi,
    private val products: ProductRepository,
    private val units: ProductUnitRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockRequestState())
    val state: StateFlow<StockRequestState> = _state.asStateFlow()

    init {
        // Observed rather than fetched once: a sync can land while the rep is
        // still building the request, and the van stock shown per line is the
        // number they are deciding against.
        viewModelScope.launch {
            products.observeAll().collect { list ->
                _state.update { it.copy(products = list) }
            }
        }
        viewModelScope.launch {
            units.observeAll().collect { list ->
                _state.update { it.copy(units = list) }
            }
        }
        refreshMine()
    }

    fun onEvent(event: StockRequestEvent) {
        when (event) {
            StockRequestEvent.OpenPicker -> _state.update { it.copy(isPickerOpen = true) }
            StockRequestEvent.ClosePicker ->
                _state.update { it.copy(isPickerOpen = false, searchQuery = "") }
            is StockRequestEvent.SearchChanged ->
                _state.update { it.copy(searchQuery = event.v) }

            is StockRequestEvent.AddProduct -> addProduct(event.product)

            is StockRequestEvent.QuantityChanged -> _state.update { s ->
                s.copy(
                    lines = s.lines.mapIndexed { i, l ->
                        if (i == event.index) {
                            l.copy(quantity = event.v.filter { c -> c.isDigit() || c == '.' })
                        } else {
                            l
                        }
                    },
                )
            }

            is StockRequestEvent.RemoveLine -> _state.update { s ->
                s.copy(lines = s.lines.filterIndexed { i, _ -> i != event.index })
            }

            is StockRequestEvent.NoteChanged -> _state.update { it.copy(note = event.v) }

            is StockRequestEvent.OpenUnitPicker ->
                _state.update { it.copy(unitPickerFor = event.index) }
            StockRequestEvent.CloseUnitPicker ->
                _state.update { it.copy(unitPickerFor = null) }

            is StockRequestEvent.UnitChanged -> _state.update { s ->
                s.copy(
                    lines = s.lines.mapIndexed { i, l ->
                        if (i == event.index) l.copy(unit = event.unit) else l
                    },
                    unitPickerFor = null,
                )
            }

            StockRequestEvent.Submit -> submit()
            is StockRequestEvent.Cancel -> act(event.id) { api.cancel(it) }
            is StockRequestEvent.Receive -> act(event.id, RECEIVED) { api.receive(it) }
            StockRequestEvent.Refresh -> refreshMine()
            StockRequestEvent.DismissError ->
                _state.update { it.copy(errorAr = null, noticeAr = null) }
        }
    }

    private fun addProduct(product: Product) {
        _state.update { s ->
            // Already on the list: focus the rep on the row they have rather
            // than adding a second one for the same item, which the server would
            // merge anyway and which reads as a mistake in between.
            if (s.lines.any { it.product.id == product.id }) {
                return@update s.copy(isPickerOpen = false, searchQuery = "")
            }
            // Default to the item's base unit when it has one. A rep asking for
            // "10" means 10 of whatever they normally count in.
            val base = s.unitsFor(product).firstOrNull { it.isBase }
            s.copy(
                lines = s.lines + StockRequestLine(product = product, unit = base),
                isPickerOpen = false,
                searchQuery = "",
            )
        }
    }

    private fun submit() {
        val lines = _state.value.lines.filter { it.qtyOrZero > 0 }
        if (lines.isEmpty()) return
        _state.update { it.copy(isSubmitting = true, errorAr = null, noticeAr = null) }
        viewModelScope.launch {
            runCatching {
                api.create(
                    CreateStockRequestBody(
                        items = lines.map { l ->
                            StockRequestLineRequest(
                                itemNumber = l.product.sku,
                                // Only a variant unit owns a pool; packaging over
                                // the base pool must stay "" or the goods are
                                // received into a pool that does not exist.
                                stockUnitCode =
                                    if (l.unit?.isStockUnit == true) l.unit.code else "",
                                qtyOfUnit = l.qtyOrZero,
                                unitBaseQty = l.factor,
                                itemUnitId = l.unit?.id,
                                unitName = l.unit?.name,
                            )
                        },
                        note = _state.value.note.trim().takeIf { it.isNotBlank() },
                    ),
                )
            }.fold(
                onSuccess = { created ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            // Clear the draft, keep the screen: the answer lands
                            // in the list below and the rep is waiting for it.
                            lines = emptyList(),
                            note = "",
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

    private fun refreshMine() {
        _state.update { it.copy(isLoadingMine = true) }
        viewModelScope.launch {
            runCatching { api.mine() }.fold(
                onSuccess = { list ->
                    _state.update { it.copy(isLoadingMine = false, mine = list) }
                },
                onFailure = {
                    // Silent: a rep with no signal should still be able to BUILD
                    // a request. Only the send needs the network, and that error
                    // is reported where it happens.
                    _state.update { it.copy(isLoadingMine = false) }
                },
            )
        }
    }

    /** One request-scoped call, with its own busy flag so each row spins alone. */
    private fun act(
        id: String,
        notice: String? = null,
        call: suspend (String) -> Any,
    ) {
        if (id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + id, errorAr = null) }
        viewModelScope.launch {
            runCatching { call(id) }.fold(
                onSuccess = {
                    _state.update { s -> s.copy(busyIds = s.busyIds - id, noticeAr = notice) }
                    // Re-read rather than patching the row: receiving posts a
                    // transfer, and the server is the only thing that knows the
                    // voucher number it produced.
                    refreshMine()
                },
                onFailure = {
                    _state.update { s ->
                        s.copy(busyIds = s.busyIds - id, errorAr = ERR_ACTION)
                    }
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

package com.jehadalomour.flowvan.shared.presentation.feature.vanstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class VanStockViewModel(private val products: ProductRepository) : ViewModel() {

    private val _state = MutableStateFlow(VanStockState())
    val state: StateFlow<VanStockState> = _state.asStateFlow()

    init {
        observeProducts()
    }

    fun onEvent(event: VanStockEvent) {
        when (event) {
            is VanStockEvent.SearchChanged -> _state.update { it.copy(searchQuery = event.query) }
            is VanStockEvent.CategorySelected -> _state.update { it.copy(selectedCategory = event.category) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun observeProducts() {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        products.observeAll()
            .onEach { list ->
                _state.update { s ->
                    s.copy(
                        allProducts = list,
                        totalInventoryValue = list.sumOf { it.vanStock * it.salePrice },
                        nowMs = nowMs,
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}

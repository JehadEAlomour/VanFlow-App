package com.jehadalomour.flowvan.shared.presentation.feature.vanstock

sealed class VanStockEvent {
    data class SearchChanged(val query: String) : VanStockEvent()
    data class CategorySelected(val category: String?) : VanStockEvent()
}

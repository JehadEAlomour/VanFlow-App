package com.jehadalomour.flowvan.feature.voucher

sealed class VanStockEvent {
    data class SearchChanged(val query: String) : VanStockEvent()
    data class CategorySelected(val category: String?) : VanStockEvent()
}

package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.Product

enum class StockStatus { GOOD, LOW, OUT, EXPIRING }

fun Product.stockStatus(nowMs: Long): StockStatus {
    // expiryDate is a public-API val from another module, so it can't be smart-cast after the
    // null check — capture it locally first.
    val expiry = expiryDate
    return when {
        vanStock == 0 -> StockStatus.OUT
        expiry != null && expiry <= nowMs + 30L * 24 * 3600 * 1000 -> StockStatus.EXPIRING
        vanStock < minStock -> StockStatus.LOW
        else -> StockStatus.GOOD
    }
}

data class VanStockState(
    val allProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val totalInventoryValue: Double = 0.0,
    val nowMs: Long = 0L,
    val isLoading: Boolean = true,
) {
    val categories: List<String> get() =
        allProducts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    val visibleProducts: List<Product> get() {
        var list = allProducts
        if (selectedCategory != null) list = list.filter { it.category == selectedCategory }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.nameAr.contains(q) || it.nameEn.lowercase().contains(q) ||
                    it.sku.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
        return list.sortedWith(compareBy({ it.stockStatus(nowMs).ordinal }, { it.nameAr }))
    }
}

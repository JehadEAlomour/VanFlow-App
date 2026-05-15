package com.jehadalomour.flowvan.shared.presentation.feature.collection

import com.jehadalomour.flowvan.shared.domain.model.Customer
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod

data class CollectionState(
    val customer: Customer? = null,
    val amountText: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val chequeNumber: String = "",
    val chequeBank: String = "",
    val chequeDate: Long? = null,
    val transferRef: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
) {
    val amount: Double? get() = amountText.toDoubleOrNull()
    val advanceWarning: Boolean get() {
        val a = amount ?: return false
        val balance = customer?.balance ?: return false
        return a > balance
    }
}

sealed interface CollectionEvent {
    data class AmountChanged(val text: String) : CollectionEvent
    data class MethodSelected(val method: PaymentMethod) : CollectionEvent
    data class ChequeNumberChanged(val v: String) : CollectionEvent
    data class ChequeBankChanged(val v: String) : CollectionEvent
    data class ChequeDateChanged(val epochMillis: Long?) : CollectionEvent
    data class TransferRefChanged(val v: String) : CollectionEvent
    data class NotesChanged(val v: String) : CollectionEvent
    data object Save : CollectionEvent
    data object DismissError : CollectionEvent
}

val JordanBanks = listOf(
    "البنك العربي", "بنك الأردن", "البنك الإسلامي الأردني",
    "بنك القاهرة عمان", "بنك الإسكان", "بنك ABC", "أخرى",
)

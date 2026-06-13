package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.PaymentMethod

data class JordanBank(val nameAr: String, val colorHex: Long, val initial: String)

val JordanBanks = listOf(
    JordanBank("بنك الأردن", 0xFF185FA5L, "أر"),
    JordanBank("البنك الإسلامي الأردني", 0xFF1D9E75L, "إس"),
    JordanBank("بنك عمان العربي", 0xFFD94040L, "عر"),
    JordanBank("البنك العربي", 0xFFC97B1AL, "عب"),
    JordanBank("بنك القاهرة عمان", 0xFF7F5FD4L, "قو"),
    JordanBank("البنك الأهلي الأردني", 0xFF0C447CL, "أه"),
    JordanBank("بنك الإسكان للتجارة", 0xFFC98B1AL, "إك"),
    JordanBank("بنك الاتحاد", 0xFF1D9E75L, "ات"),
    JordanBank("البنك الأردني الكويتي", 0xFF0F6E56L, "أك"),
    JordanBank("البنك التجاري الأردني", 0xFF637181L, "تج"),
    JordanBank("بنك الشرق الأوسط", 0xFFD85A30L, "شر"),
    JordanBank("بنك صفوة الإسلامي", 0xFF185FA5L, "صف"),
)

data class ChequeEntry(
    val amountText: String = "",
    val number: String = "",
    val bank: JordanBank? = null,
    val dateMillis: Long? = null,
) {
    val amount: Double? get() = amountText.toDoubleOrNull()
}

data class CollectionState(
    val customer: Customer? = null,
    val amountText: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val cheques: List<ChequeEntry> = listOf(ChequeEntry()),
    val bankSheetOpenForIndex: Int? = null,
    val bankSearchQuery: String = "",
    val transferRef: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val savedNumber: String? = null,
    val errorAr: String? = null,
) {
    val amount: Double? get() = when (method) {
        PaymentMethod.CHEQUE -> {
            val amounts = cheques.mapNotNull { it.amount }
            if (amounts.size == cheques.size && amounts.isNotEmpty()) amounts.sum() else null
        }
        else -> amountText.toDoubleOrNull()
    }

    val advanceWarning: Boolean get() {
        val a = amount ?: return false
        val balance = customer?.balance ?: return false
        return a > balance
    }

    val filteredBanks: List<JordanBank> get() =
        if (bankSearchQuery.isBlank()) JordanBanks
        else JordanBanks.filter { it.nameAr.contains(bankSearchQuery, ignoreCase = true) }
}

sealed interface CollectionEvent {
    data class AmountChanged(val text: String) : CollectionEvent
    data class MethodSelected(val method: PaymentMethod) : CollectionEvent
    data class ChequeAmountChanged(val index: Int, val text: String) : CollectionEvent
    data class ChequeNumberChanged(val index: Int, val v: String) : CollectionEvent
    data class ChequeDateChanged(val index: Int, val epochMillis: Long?) : CollectionEvent
    data class OpenBankSheet(val index: Int) : CollectionEvent
    data object CloseBankSheet : CollectionEvent
    data class BankSearchQueryChanged(val query: String) : CollectionEvent
    data class BankSelected(val bank: JordanBank) : CollectionEvent
    data object AddCheque : CollectionEvent
    data class RemoveCheque(val index: Int) : CollectionEvent
    data class TransferRefChanged(val v: String) : CollectionEvent
    data class QuickFillAmount(val amount: Double) : CollectionEvent
    data class NotesChanged(val v: String) : CollectionEvent
    data object Save : CollectionEvent
    data object DismissError : CollectionEvent
}

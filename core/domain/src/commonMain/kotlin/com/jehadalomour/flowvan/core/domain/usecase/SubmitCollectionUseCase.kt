package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.network.api.CollectionApi
import com.jehadalomour.flowvan.core.network.dto.ChequeInput
import com.jehadalomour.flowvan.core.network.dto.CreateCollectionRequest
import com.jehadalomour.flowvan.core.network.mapper.toPayment
import com.jehadalomour.flowvan.core.network.http.NetworkException
import com.jehadalomour.flowvan.core.network.http.jodToFils
import com.jehadalomour.flowvan.core.model.Payment
import com.jehadalomour.flowvan.core.model.PaymentMethod
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Records a cash/cheque collection on the backend. */
class SubmitCollectionUseCase(
    private val collectionApi: CollectionApi,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        repId: String,
        customerId: String,
        amount: Double,
        method: PaymentMethod,
        invoiceId: String? = null,
        chequeNumber: String? = null,
        chequeBank: String? = null,
        note: String? = null,
    ): Result<Payment> {
        if (amount <= 0.0) return Result.failure(IllegalArgumentException("amount must be > 0"))
        val apiMethod = if (method == PaymentMethod.CHEQUE) "cheque" else "cash"
        return try {
            val dto = collectionApi.create(
                CreateCollectionRequest(
                    repId = repId,
                    customerId = customerId,
                    invoiceId = invoiceId,
                    amount = amount.jodToFils().toLong(),
                    method = apiMethod,
                    note = note,
                    // ONE CHEQUE, CARRYING THE AMOUNT.
                    //
                    // The server takes a LIST and totals it, and refuses a body
                    // with any property it does not declare. This sent a single
                    // `cheque` object with no amount, so every cheque collection
                    // a rep took was rejected before it reached the service and
                    // turned up in neither VanFlow nor the ERP.
                    cheques = if (method == PaymentMethod.CHEQUE) {
                        listOf(
                            ChequeInput(
                                amount = amount.jodToFils().toLong(),
                                bankName = chequeBank,
                                chequeNumber = chequeNumber,
                            ),
                        )
                    } else {
                        null
                    },
                ),
            )
            val now = Clock.System.now().toEpochMilliseconds()
            Result.success(dto.toPayment(now, dueDateMs = null))
        } catch (e: NetworkException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

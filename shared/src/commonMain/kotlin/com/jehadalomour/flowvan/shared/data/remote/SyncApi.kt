package com.jehadalomour.flowvan.shared.data.remote

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.local.entity.LocationPointEntity
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SyncApi(private val httpClient: HttpClient, private val json: Json) {

    suspend fun postInvoices(baseUrl: String, invoices: List<InvoiceEntity>) {
        httpClient.post("$baseUrl/api/invoices/batch") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(InvoiceBatch.serializer(), InvoiceBatch(invoices.map { it.toDto() })))
        }
    }

    suspend fun postPayments(baseUrl: String, payments: List<PaymentEntity>) {
        httpClient.post("$baseUrl/api/payments/batch") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PaymentBatch.serializer(), PaymentBatch(payments.map { it.toDto() })))
        }
    }

    suspend fun postTracking(baseUrl: String, points: List<LocationPointEntity>) {
        httpClient.post("$baseUrl/api/tracking/batch") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TrackingBatch.serializer(), TrackingBatch(points.map { it.toDto() })))
        }
    }

    @Serializable data class InvoiceBatch(val invoices: List<InvoiceDto>)
    @Serializable data class PaymentBatch(val payments: List<PaymentDto>)
    @Serializable data class TrackingBatch(val points: List<TrackingPointDto>)

    @Serializable
    data class InvoiceDto(
        val id: String, val number: String, val type: String, val status: String,
        val customerId: String, val salesmanId: String, val createdAt: Long,
        val linesJson: String, val subtotal: Double, val discountAmount: Double,
        val taxAmount: Double, val total: Double, val paymentMethod: String?, val notes: String?,
    )

    @Serializable
    data class PaymentDto(
        val id: String, val number: String, val customerId: String, val salesmanId: String,
        val method: String, val amount: Double, val status: String,
        val createdAt: Long, val chequeNumber: String?, val chequeBank: String?,
        val chequeDate: Long?, val transferRef: String?, val notes: String?,
    )

    @Serializable
    data class TrackingPointDto(
        val id: Long, val shiftId: String, val userId: String,
        val lat: Double, val lng: Double, val accuracy: Float?, val recordedAt: Long,
    )

    private fun InvoiceEntity.toDto() = InvoiceDto(
        id, number, type, status, customerId, salesmanId, createdAt,
        linesJson, subtotal, discountAmount, taxAmount, total, paymentMethod, notes,
    )

    private fun PaymentEntity.toDto() = PaymentDto(
        id = id, number = number, customerId = customerId, salesmanId = salesmanId,
        method = method, amount = amount, status = status, createdAt = createdAt,
        chequeNumber = chequeNumber, chequeBank = chequeBank, chequeDate = chequeDate,
        transferRef = transferRef, notes = notes,
    )

    private fun LocationPointEntity.toDto() = TrackingPointDto(id, shiftId, userId, lat, lng, accuracy, recordedAt)
}

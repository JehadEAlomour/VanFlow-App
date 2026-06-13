package com.jehadalomour.flowvan.core.network

import com.jehadalomour.flowvan.core.network.dto.CustomerDto
import com.jehadalomour.flowvan.core.network.dto.InvoiceDto
import com.jehadalomour.flowvan.core.network.dto.LoginResponseDto
import com.jehadalomour.flowvan.core.network.dto.ProductDto
import com.jehadalomour.flowvan.core.network.mapper.toDomain
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import com.jehadalomour.flowvan.core.network.mapper.toUser
import com.jehadalomour.flowvan.core.network.http.ApiEnvelope
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.model.UserRole
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the DTOs + mappers against payloads captured live from the VanFlow backend
 * (see .claude/FLOW-API.md). Uses the same Json config as the response path.
 */
class RemoteDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun decodesLoginAndMapsRepRole() {
        val body = """
        {"success":true,"data":{"accessToken":"jwt.abc","user":{
        "id":"e51f4eeb","userNumber":"admin","name":"Default Admin","userType":"ADMIN",
        "role":"admin","repId":"6e48e184","permissions":{"canMakeVoucher":true}}},
        "timestamp":"2026-06-01T07:39:23Z"}
        """.trimIndent()
        val env = json.decodeFromString<ApiEnvelope<LoginResponseDto>>(body)
        assertEquals("jwt.abc", env.data.accessToken)
        assertEquals("6e48e184", env.data.user.repId)
        val user = env.data.user.toUser(env.data.accessToken)
        assertEquals(UserRole.ADMIN, user.role)
        assertEquals("jwt.abc", user.token)
    }

    @Test
    fun decodesCustomersPageAndMapsMoney() {
        val body = """
        {"success":true,"data":{"items":[{
        "id":"5ce4c79a","customerNumber":"CF-X","customerName":"Wed Shop","nameAr":"Wed Shop",
        "longitude":"35.910000","latitude":"31.950000","repId":"19028aa5",
        "creditLimit":"12.500","customerType":"CASH","totalDebt":"29.000","totalCredit":"0.00",
        "isActive":true}],"total":21}}
        """.trimIndent()
        val env = json.decodeFromString<ApiEnvelope<OffsetPage<CustomerDto>>>(body)
        assertEquals(21, env.data.total)
        val entity = env.data.items.first().toEntity()
        assertEquals("CF-X", entity.code)
        assertEquals(12.5, entity.creditLimit)
        assertEquals(29.0, entity.balance)          // totalDebt - totalCredit
        assertEquals(31.95, entity.lat)
    }

    @Test
    fun decodesProductAndConvertsFilsToJod() {
        val body = """
        {"success":true,"data":{"items":[{
        "id":"e6d20b2b","itemNumber":"P001","sku":"P001","name":"Cola 330ml","nameAr":"Cola 330ml",
        "unit":"carton","price":12500,"cost":null,"reorderQty":3,"taxRate":"0.1600","isActive":true
        }],"total":6}}
        """.trimIndent()
        val env = json.decodeFromString<ApiEnvelope<OffsetPage<ProductDto>>>(body)
        val entity = env.data.items.first().toEntity()
        assertEquals(12.5, entity.salePrice)        // 12500 fils -> 12.5 JOD
        assertEquals(3, entity.minStock)
        assertEquals(0.16, entity.taxRate)
    }

    @Test
    fun decodesInvoiceWithStringQuantity() {
        // Backend returns line quantity as a quoted number "2.000" — lenient Json must coerce it.
        val body = """
        {"success":true,"data":{"id":"153d6083","invoiceNumber":"INV-2026-000005","status":"confirmed",
        "customerId":"5ce4c79a","repId":"6e48e184","subtotal":25000,"totalLineDiscounts":0,
        "invoiceDiscountAmount":0,"totalTax":4000,"grandTotal":29000,
        "lines":[{"id":"6","productId":"e6d20b2b","quantity":"2.000","unitPrice":12500,
        "taxAmount":4000,"lineTotal":29000}]}}
        """.trimIndent()
        val env = json.decodeFromString<ApiEnvelope<InvoiceDto>>(body)
        val invoice = env.data.toDomain(createdAtMs = 0L)
        assertEquals(29.0, invoice.total)
        assertEquals(4.0, invoice.taxAmount)
        assertEquals(1, invoice.lines.size)
        assertEquals(2.0, invoice.lines.first().qty)
        assertEquals(12.5, invoice.lines.first().unitPrice)
        assertTrue(invoice.number == "INV-2026-000005")
    }
}

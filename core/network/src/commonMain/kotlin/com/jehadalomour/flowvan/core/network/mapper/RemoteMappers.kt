package com.jehadalomour.flowvan.core.network.mapper

import com.jehadalomour.flowvan.core.database.entity.CustomerEntity
import com.jehadalomour.flowvan.core.database.entity.ProductEntity
import com.jehadalomour.flowvan.core.network.dto.ApiUserDto
import com.jehadalomour.flowvan.core.network.dto.CollectionDto
import com.jehadalomour.flowvan.core.network.dto.CustomerDto
import com.jehadalomour.flowvan.core.network.dto.InvoiceDto
import com.jehadalomour.flowvan.core.network.dto.InvoiceLineDto
import com.jehadalomour.flowvan.core.network.dto.ProductDto
import com.jehadalomour.flowvan.core.network.dto.RepKpiDto
import com.jehadalomour.flowvan.core.network.http.filsToJod
import com.jehadalomour.flowvan.core.network.http.numericStringToDouble
import com.jehadalomour.flowvan.core.model.CustomerSegment
import com.jehadalomour.flowvan.core.model.CustomerTier
import com.jehadalomour.flowvan.core.model.DailyKpi
import com.jehadalomour.flowvan.core.model.Invoice
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.InvoiceStatus
import com.jehadalomour.flowvan.core.model.InvoiceType
import com.jehadalomour.flowvan.core.model.Payment
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.PaymentStatus
import com.jehadalomour.flowvan.core.model.User
import com.jehadalomour.flowvan.core.model.UserRole

// ---- Auth ----

fun ApiUserDto.toUser(token: String?): User = User(
    id = id,
    nameAr = name.ifBlank { userNumber },
    nameEn = name.ifBlank { null },
    phone = userNumber,
    role = mapRole(userType, role),
    token = token,
)

private fun mapRole(userType: String, role: String?): UserRole = when (userType.uppercase()) {
    "ADMIN" -> UserRole.ADMIN
    "MANAGER" -> UserRole.MANAGER
    "DRIVER", "SALES" -> UserRole.SALESMAN
    else -> when (role?.lowercase()) {
        "admin" -> UserRole.ADMIN
        "manager" -> UserRole.MANAGER
        "supervisor" -> UserRole.SUPERVISOR
        else -> UserRole.SALESMAN
    }
}

// ---- Customers ----

fun CustomerDto.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    code = customerNumber,
    nameAr = nameAr.ifBlank { customerName },
    nameEn = nameEn,
    phone = phone,
    area = city ?: cityCode ?: "",
    addressAr = addressAr,
    tier = CustomerTier.C.name,
    segment = CustomerSegment.REGULAR.name,
    churnRisk = 0.0,
    balance = totalDebt.numericStringToDouble() - totalCredit.numericStringToDouble(),
    overdueAmount = 0.0,
    creditLimit = creditLimit.numericStringToDouble(),
    taxNumber = tin,
    isOnRoute = false,
    visitOrder = 0,
    lat = latitude?.toDoubleOrNull(),
    lng = longitude?.toDoubleOrNull(),
    category = category,
    regionId = regionId,
    repId = repId,
    priceListId = priceListId,
)

// ---- Products ----

fun ProductDto.toEntity(): ProductEntity = ProductEntity(
    id = id,
    sku = sku.ifBlank { itemNumber },
    nameAr = nameAr.ifBlank { name },
    nameEn = nameEn ?: name,
    // Prefer the human category name; fall back to the id so the category filter
    // still works before the backend (categoryName) is deployed. Blank only when
    // the product is genuinely uncategorised.
    category = categoryName ?: categoryId ?: "",
    unit = unit,
    salePrice = price.filsToJod(),
    costPrice = (cost ?: 0L).filsToJod(),
    vanStock = 0,
    minStock = reorderQty,
    expiryDate = null,
    brand = null,
    taxRate = taxRate.toDoubleOrNull() ?: 0.16,
    imageUrl = imageUrl,
    isTobacco = isTobaccoProduct,
    tobaccoProfileId = tobaccoTaxProfileId,
    consumerPriceFils = consumerPriceFils ?: 0,
)

// ---- Invoices ----

fun InvoiceLineDto.toDomain(): InvoiceLine = InvoiceLine(
    productId = productId,
    sku = sku,
    nameAr = nameAr,
    qty = quantity,
    unitPrice = unitPrice.filsToJod(),
    lineTotal = lineTotal.filsToJod(),
    taxAmount = taxAmount.filsToJod(),
)

fun InvoiceDto.toDomain(createdAtMs: Long): Invoice = Invoice(
    id = id,
    number = invoiceNumber,
    type = InvoiceType.SALE,
    status = mapInvoiceStatus(status),
    customerId = customerId,
    salesmanId = repId,
    createdAt = createdAtMs,
    lines = lines.map { it.toDomain() },
    subtotal = subtotal.filsToJod(),
    discountAmount = (totalLineDiscounts + invoiceDiscountAmount).filsToJod(),
    taxAmount = totalTax.filsToJod(),
    total = grandTotal.filsToJod(),
    paymentMethod = null,
    notes = null,
)

private fun mapInvoiceStatus(status: String): InvoiceStatus = when (status.lowercase()) {
    "confirmed" -> InvoiceStatus.CONFIRMED
    "cancelled", "rejected" -> InvoiceStatus.CANCELLED
    else -> InvoiceStatus.DRAFT
}

// ---- Collections ----

fun CollectionDto.toPayment(createdAtMs: Long, dueDateMs: Long?): Payment = Payment(
    id = id,
    number = id,
    customerId = customerId,
    salesmanId = repId,
    amount = amount.filsToJod(),
    method = if (method.equals("cheque", ignoreCase = true)) PaymentMethod.CHEQUE else PaymentMethod.CASH,
    status = mapCollectionStatus(status),
    createdAt = createdAtMs,
    chequeNumber = cheque?.chequeNumber,
    chequeBank = cheque?.bankName,
    chequeDate = dueDateMs,
    transferRef = null,
    notes = note,
)

private fun mapCollectionStatus(status: String): PaymentStatus = when (status.lowercase()) {
    "confirmed", "deposited" -> PaymentStatus.CONFIRMED
    "bounced" -> PaymentStatus.BOUNCED
    else -> PaymentStatus.PENDING
}

// ---- Reps ----

fun RepKpiDto.toDailyKpi(customersPlanned: Int, customersVisited: Int): DailyKpi = DailyKpi(
    salesTotal = todayRevenueFils.filsToJod(),
    // The remote KPI doesn't split cash vs credit; treat the aggregate as cash and
    // leave credit at 0 (the local GetDailyKpiUseCase computes the real split).
    cashSalesTotal = todayRevenueFils.filsToJod(),
    creditSalesTotal = 0.0,
    returnsTotal = 0.0,
    collectionsTotal = 0.0,
    customersVisited = customersVisited,
    customersPlanned = customersPlanned,
)

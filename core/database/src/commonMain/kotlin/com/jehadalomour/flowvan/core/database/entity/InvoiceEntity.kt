package com.jehadalomour.flowvan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val number: String,
    val type: String,
    val status: String,
    val customerId: String,
    val salesmanId: String,
    val createdAt: Long,
    val linesJson: String,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: String?,
    val notes: String?,
    val syncedAt: Long?,
    /** For RETURN vouchers: the original SALE invoice this return is issued against. */
    val referenceInvoiceId: String? = null,
    val referenceNumber: String? = null,
    /**
     * GIFT picks for ITEM_QTY_REWARD offers, as a comma-joined list of item numbers
     * (e.g. "ITM-1,ITM-3"). Null/blank when the sale carries no gift picks. Sent to the
     * server on sync so it adds the free lines and records the redemption.
     */
    val chosenFreeItemsCsv: String? = null,
    /**
     * Rep's GPS at the moment the voucher was created (location-locked reps).
     * Persisted so an offline sale that syncs later is validated by the backend
     * against where the rep actually was, not where they are at sync time. Null
     * for unrestricted reps / when no fix was available.
     */
    val repLat: Double? = null,
    val repLng: Double? = null,
    /**
     * Offer-aware upload snapshot. The primary fields ([linesJson], [subtotal],
     * [discountAmount], [taxAmount], [total]) hold the OFFER-APPLIED result so the
     * saved/printed invoice matches the cart (correct even offline). But the server
     * re-applies offers on POST /vouchers, so the UPLOAD must carry the RAW cart
     * (manual discounts only) or offers would be counted twice. When an offer was
     * applied at sale time these hold that raw representation; the sync mapper uses
     * them instead of the primary fields. Null → no offer, upload uses the primary
     * fields (unchanged behaviour).
     */
    val uploadLinesJson: String? = null,
    val uploadDiscountAmount: Double? = null,
    /**
     * Offers applied at sale time, frozen for the printed receipt as a JSON list of
     * [com.jehadalomour.flowvan.core.model.InvoiceAppliedOffer] ({name, discountAmount} in JOD).
     * The live evaluation that knows each offer's amount isn't available when printing later from
     * a report, so it's captured here. Null/blank → no offers (footer shows the generic discount).
     */
    val appliedOffersJson: String? = null,
    /**
     * Tax-exemption SNAPSHOT (v18) — how THIS document was issued, not what the
     * customer's record says today. Frozen at save time and mirrored from the
     * server's own snapshot on sync, so a receipt reprinted months later still
     * shows the exemption it was issued under even if the certificate has lapsed.
     */
    @ColumnInfo(defaultValue = "0")
    val isTaxExempt: Boolean = false,
    val taxExemptionNumber: String? = null,
    /**
     * JoFotara e-invoice QR (v21) — the government QR content, mirrored from the
     * server once the ERP submits this sale to JoFotara. Null until it lands
     * (async); the receipt shows the QR when present, a "pending" note otherwise.
     */
    val jofotaraQrCode: String? = null,
)
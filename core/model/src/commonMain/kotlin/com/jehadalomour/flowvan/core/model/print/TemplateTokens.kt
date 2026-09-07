package com.jehadalomour.flowvan.core.model.print

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The voucher kinds a template can be designed for (spec §1), with the Arabic name
 * `{{invoice.kindName}}` prints. The app itself only creates SALE / RETURN / REQUEST
 * (an ORDER), but a template may name any kind, so the whole table is here.
 */
object VoucherKinds {
    private val arabic = mapOf(
        "SALE" to "سند بيع",
        "RETURN" to "سند مرتجع",
        "ORDER" to "طلبية",
        "REQUEST" to "طلبية",
        "TRANSFER" to "سند تحويل",
        "TRANSFER_IN" to "تحميل المركبة",
        "TRANSFER_OUT" to "تنزيل المركبة",
        "IN" to "إدخال للمخزن",
        "OUT" to "إخراج من المخزن",
        "PURCHASE" to "سند شراء",
        "ADJUSTMENT" to "تسوية",
        "PAYMENT_IN" to "سند قبض",
        "PAYMENT_OUT" to "سند صرف",
    )

    /** Arabic name of a kind; the code itself when the kind is unknown. */
    fun arabicName(kind: String): String = arabic[kind] ?: kind

    /**
     * The template key for an app invoice type. The app stores an order voucher as
     * REQUEST; the designer calls that kind ORDER.
     */
    fun documentTypeFor(invoiceType: String): String =
        if (invoiceType == "REQUEST") "ORDER" else invoiceType
}

/** One line of the items table, already reduced to the ten column keys of spec §4. */
data class TemplateLine(
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val qty: Double = 0.0,
    val unit: String = "",
    val price: Double = 0.0,
    /** Tax rate as a fraction (0.16 = 16 %). */
    val taxRate: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    /** qty × price, before discount and tax. */
    val gross: Double = 0.0,
    /** Net of discount and including tax — what this line costs the customer. */
    val total: Double = 0.0,
    /** A 100 %-discounted line; printed with the " (هدية)" suffix. */
    val isGift: Boolean = false,
)

/**
 * Everything a template can print, gathered from the voucher, the company profile and
 * the session. Pure data so the resolver — and its tests — need no UI or platform.
 * Money values are raw doubles; [TemplateTokens] formats them with [decimals] + [currency].
 */
data class TemplateContext(
    // company.*
    val companyNameAr: String = "",
    val companyNameEn: String = "",
    val companyTaxNumber: String = "",
    val companyAddressAr: String = "",
    val companyAddressEn: String = "",
    val companyPhone1: String = "",
    val companyPhone2: String = "",
    val companyEmail: String = "",
    val companyWebsite: String = "",
    val companyFooterNote: String = "",
    val companyFooterNoteAr: String = "",
    /** `data:<mime>;base64,...` URI of the logo; blank = none. Not a token — drawn by LOGO. */
    val companyLogo: String = "",
    // branch.*
    val branchName: String = "",
    val branchAddress: String = "",
    val branchPhone: String = "",
    // invoice.*
    val invoiceNumber: String = "",
    /** transKind code: SALE, RETURN, ORDER, … */
    val invoiceKind: String = "",
    /** `YYYY-MM-DD` */
    val invoiceDate: String = "",
    /** `HH:mm` */
    val invoiceTime: String = "",
    val cashier: String = "",
    val customerName: String = "",
    val customerNumber: String = "",
    val customerPhone: String = "",
    val customerTaxNumber: String = "",
    val customerAddress: String = "",
    val store: String = "",
    val fromStore: String = "",
    val toStore: String = "",
    val reference: String = "",
    val note: String = "",
    val lines: List<TemplateLine> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxTotal: Double = 0.0,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val change: Double = 0.0,
    /** "CASH: 6.500 JOD  CARD: …" — already composed by the caller. */
    val payments: String = "",
    val paymentType: String = "",
    val isTaxExempt: Boolean = false,
    /** The stamp text printed for `{{invoice.taxExempt}}` when [isTaxExempt]. */
    val taxExemptStamp: String = "",
    val taxExemptionNumber: String = "",
    val qrData: String = "",
    // formatting
    val decimals: Int = 3,
    val currency: String = "",
)

/**
 * Replaces every `{{token}}` in a template string (spec §4). Unknown tokens render as an
 * empty string, so a template written for a richer voucher still prints — with blanks —
 * rather than leaking its placeholders onto the customer's copy.
 */
object TemplateTokens {

    private val tokenPattern = Regex("""\{\{\s*([A-Za-z0-9_.]+)\s*}}""")

    const val GIFT_SUFFIX = " (هدية)"

    fun resolve(text: String?, ctx: TemplateContext): String {
        if (text.isNullOrEmpty()) return ""
        return tokenPattern.replace(text) { m -> value(m.groupValues[1], ctx) }
    }

    /** The value of one bare token name (without braces); "" when unknown. */
    fun value(token: String, ctx: TemplateContext): String = when (token) {
        "company.nameAr" -> ctx.companyNameAr
        "company.nameEn" -> ctx.companyNameEn
        "company.taxNumber" -> ctx.companyTaxNumber
        "company.addressAr" -> ctx.companyAddressAr
        "company.addressEn" -> ctx.companyAddressEn
        "company.phone1" -> ctx.companyPhone1
        "company.phone2" -> ctx.companyPhone2
        "company.email" -> ctx.companyEmail
        "company.website" -> ctx.companyWebsite
        "company.footerNote" -> ctx.companyFooterNote
        "company.footerNoteAr" -> ctx.companyFooterNoteAr

        "branch.name" -> ctx.branchName
        "branch.address" -> ctx.branchAddress
        "branch.phone" -> ctx.branchPhone

        "invoice.number" -> ctx.invoiceNumber
        "invoice.kind" -> ctx.invoiceKind
        "invoice.kindName" -> VoucherKinds.arabicName(ctx.invoiceKind)
        "invoice.date" -> ctx.invoiceDate
        "invoice.time" -> ctx.invoiceTime
        "invoice.cashier" -> ctx.cashier
        "invoice.customer.name" -> ctx.customerName
        "invoice.customer.number" -> ctx.customerNumber
        "invoice.customer.phone" -> ctx.customerPhone
        "invoice.customer.taxNumber" -> ctx.customerTaxNumber
        "invoice.customer.address" -> ctx.customerAddress
        "invoice.store" -> ctx.store
        "invoice.fromStore" -> ctx.fromStore
        "invoice.toStore" -> ctx.toStore
        "invoice.reference" -> ctx.reference
        "invoice.note" -> ctx.note
        "invoice.itemCount" -> ctx.lines.size.toString()
        "invoice.subtotal" -> money(ctx.subtotal, ctx)
        "invoice.discount" -> money(ctx.discount, ctx)
        "invoice.taxTotal" -> money(ctx.taxTotal, ctx)
        "invoice.total" -> money(ctx.total, ctx)
        "invoice.paid" -> money(ctx.paid, ctx)
        "invoice.change" -> money(ctx.change, ctx)
        "invoice.payments" -> ctx.payments
        "invoice.paymentType" -> ctx.paymentType
        "invoice.taxExempt" -> if (ctx.isTaxExempt) ctx.taxExemptStamp else ""
        "invoice.taxExemptionNumber" -> ctx.taxExemptionNumber
        "invoice.qrData" -> ctx.qrData
        else -> ""
    }

    /** Amount with the company decimals and currency, e.g. "6.500 JOD". */
    fun money(value: Double, ctx: TemplateContext): String {
        val number = formatAmount(value, ctx.decimals)
        return if (ctx.currency.isBlank()) number else "$number ${ctx.currency}"
    }

    /**
     * The text of one items-table cell. Money columns carry the bare amount (the currency
     * is in the header, not repeated on every row); [line.isGift] suffixes the name.
     */
    fun cell(key: String, line: TemplateLine, ctx: TemplateContext): String = when (key) {
        "name" -> if (line.isGift) line.name + GIFT_SUFFIX else line.name
        "sku" -> line.sku
        "barcode" -> line.barcode
        "qty" -> formatQty(line.qty)
        "unit" -> line.unit
        "price" -> formatAmount(line.price, ctx.decimals)
        "taxPct" -> if (line.taxRate > 0.0) "${(line.taxRate * 100).roundToInt()}%" else ""
        "discount" -> formatAmount(line.discount, ctx.decimals)
        "tax" -> formatAmount(line.tax, ctx.decimals)
        "gross" -> formatAmount(line.gross, ctx.decimals)
        "total" -> formatAmount(line.total, ctx.decimals)
        else -> ""
    }

    fun formatQty(qty: Double): String =
        if (qty == qty.toLong().toDouble()) qty.toLong().toString() else formatAmount(qty, 2)

    /**
     * Bare number at the given precision, built from Latin digits explicitly. Never
     * `String.format`: it follows the default locale, which the app forces to Arabic, and
     * would emit Arabic-Indic numerals and an Arabic decimal separator.
     */
    fun formatAmount(value: Double, decimals: Int): String {
        var factor = 1L
        repeat(decimals.coerceIn(0, 9)) { factor *= 10 }
        val scaled = (abs(value) * factor).roundToLong()
        val whole = scaled / factor
        val frac = scaled % factor
        val sb = StringBuilder()
        if (value < 0 && scaled != 0L) sb.append('-')
        sb.append(whole.toString())
        if (decimals > 0) sb.append('.').append(frac.toString().padStart(decimals, '0'))
        return sb.toString()
    }
}

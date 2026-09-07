package com.jehadalomour.flowvan.core.model.print

import kotlinx.serialization.Serializable

/**
 * A print template designed on the dashboard (Template Designer) and resolved for this
 * device by `GET /invoice-templates/resolve-all`. Mirrors `docs/SPEC-print-templates.md` §3
 * in the cash-van repo — the contract the API, the dashboard and this app all render from.
 *
 * Every field carries a default so a partial or older JSON still parses: a template saved
 * before a property existed must keep printing, not throw the whole cache away.
 */
@Serializable
data class Template(
    /** null = the API's built-in layout for the kind. */
    val id: String? = null,
    val name: String = "",
    /** The voucher kind (`transKind`): SALE, RETURN, ORDER, … — see [VoucherKinds]. */
    val documentType: String = "",
    /** One of [PaperSize]. */
    val paperSize: String = PaperSize.THERMAL_80,
    val isDefault: Boolean = false,
    val branchId: String? = null,
    val layout: TemplateLayout = TemplateLayout(),
) {
    val isThermal: Boolean get() = paperSize == PaperSize.THERMAL_80
}

object PaperSize {
    const val A4 = "A4"
    const val A5 = "A5"
    const val THERMAL_80 = "THERMAL_80"

    /** Paper width in mm, the unit every template coordinate is expressed in. */
    fun widthMm(paperSize: String): Double = when (paperSize) {
        A4 -> 210.0
        A5 -> 148.0
        else -> 80.0
    }
}

@Serializable
data class TemplateLayout(
    val version: Int = 1,
    val layout: PageLayout = PageLayout(),
    val elements: List<Element> = emptyList(),
) {
    /** Elements of one zone, in element order — the order flow elements stack in. */
    fun elementsIn(zone: String): List<Element> = elements.filter { it.zone == zone }
}

@Serializable
data class PageLayout(
    /** Paper width in [unit]. */
    val width: Double = 80.0,
    /** null = continuous roll: the page is as tall as its content. */
    val height: Double? = null,
    val unit: String = "mm",
    val margins: Margins = Margins(),
    val zones: Zones = Zones(),
)

@Serializable
data class Margins(
    val top: Double = 0.0,
    val right: Double = 0.0,
    val bottom: Double = 0.0,
    val left: Double = 0.0,
)

@Serializable
data class Zones(
    val header: ZoneSpec = ZoneSpec(),
    val body: ZoneSpec = ZoneSpec(flex = true),
    val footer: ZoneSpec = ZoneSpec(),
) {
    fun spec(zone: String): ZoneSpec = when (zone) {
        Zone.HEADER -> header
        Zone.FOOTER -> footer
        else -> body
    }
}

@Serializable
data class ZoneSpec(
    /** Minimum zone height in mm (header/footer). */
    val minHeight: Double = 0.0,
    /** The body grows with its content. */
    val flex: Boolean = false,
)

object Zone {
    const val HEADER = "header"
    const val BODY = "body"
    const val FOOTER = "footer"
    val ALL = listOf(HEADER, BODY, FOOTER)
}

/** The element types both renderers understand. A type outside this set is skipped. */
object ElementType {
    const val LOGO = "LOGO"
    const val TEXT = "TEXT"
    const val DIVIDER = "DIVIDER"
    const val SPACER = "SPACER"
    const val ITEMS_TABLE = "ITEMS_TABLE"
    const val TOTALS_BLOCK = "TOTALS_BLOCK"
    const val QR_CODE = "QR_CODE"
    const val TAX_INVOICE = "TAX_INVOICE"
    const val REPORT_TABLE = "REPORT_TABLE"
    const val SHIFTS_TABLE = "SHIFTS_TABLE"

    /**
     * Flow elements are laid out top to bottom in element order, each shifted by its
     * (x, y) from where the flow put it; their height is their content (spec §3).
     */
    val FLOW = setOf(ITEMS_TABLE, TOTALS_BLOCK, TAX_INVOICE, REPORT_TABLE, SHIFTS_TABLE)

    /** Fixed elements are positioned absolutely at (x, y) inside their zone. */
    val FIXED = setOf(LOGO, TEXT, DIVIDER, SPACER, QR_CODE)
}

@Serializable
data class Element(
    val id: String = "",
    val type: String = ElementType.TEXT,
    val zone: String = Zone.BODY,
    /** mm, inside the zone. */
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    /** mm; null for flow elements, whose height is their content. */
    val height: Double? = null,
    val props: ElementProps = ElementProps(),
) {
    /** True for the element types that stack in a Column; false for absolute placement. */
    val isFlow: Boolean get() = type in ElementType.FLOW

    /** True for a type this renderer knows how to draw. */
    val isSupported: Boolean get() = type in ElementType.FLOW || type in ElementType.FIXED
}

@Serializable
data class ElementProps(
    // TEXT
    /** May contain `{{tokens}}` and "\n". */
    val content: String? = null,
    /** Points (default 10). */
    val fontSize: Double? = null,
    /** "normal" | "bold". */
    val fontWeight: String? = null,
    /** "left" | "center" | "right". */
    val align: String? = null,
    /** "ltr" | "rtl". */
    val direction: String? = null,
    /** CSS hex; a monochrome device may ignore it. */
    val color: String? = null,
    /** "sans" | "arabic" | "mono". */
    val fontFamily: String? = null,
    /** Multiplier of the font size (default 1.4). */
    val lineHeight: Double? = null,
    // LOGO
    /** "contain" | "cover" | "fill". */
    val fit: String? = null,
    // ITEMS_TABLE
    val columns: List<TableColumn>? = null,
    // TOTALS_BLOCK
    val rows: List<TotalsRow>? = null,
    // QR_CODE
    /** A token or a literal; an image URL / data: URL is drawn as-is. */
    val data: String? = null,
    // DIVIDER
    /** "solid" | "dashed" | "dotted". */
    val style: String? = null,
) {
    val isBold: Boolean get() = fontWeight == "bold"
    val fontSizePt: Double get() = fontSize ?: DEFAULT_FONT_SIZE_PT
    val lineHeightFactor: Double get() = lineHeight ?: DEFAULT_LINE_HEIGHT

    companion object {
        const val DEFAULT_FONT_SIZE_PT = 10.0
        const val DEFAULT_LINE_HEIGHT = 1.4
    }
}

/** One items-table column. [key] is one of [TableColumn.KEYS]. */
@Serializable
data class TableColumn(
    val key: String = "",
    val labelEn: String = "",
    val labelAr: String = "",
    /** Relative width; columns share the table width proportionally. */
    val width: Double = 1.0,
    /** "left" | "center" | "right". */
    val align: String? = null,
    val hide: Boolean = false,
) {
    companion object {
        val KEYS = listOf("name", "sku", "barcode", "qty", "unit", "price", "taxPct", "discount", "tax", "total")
    }
}

/** One totals-block row; [value] holds a token such as `{{invoice.total}}`. */
@Serializable
data class TotalsRow(
    val label: String = "",
    val labelAr: String = "",
    val value: String = "",
    /** "bold" | "normal". */
    val style: String? = null,
    val hide: Boolean = false,
) {
    val isBold: Boolean get() = style == "bold"
}

/** `GET /invoice-templates/resolve-all` — the `data` of the API envelope. */
@Serializable
data class PrintTemplatesResponse(
    /** Keyed by document type (SALE, RETURN, …). */
    val templates: Map<String, Template> = emptyMap(),
    /** Newest `updatedAt` among saved templates, or "builtin". */
    val version: String = "",
)

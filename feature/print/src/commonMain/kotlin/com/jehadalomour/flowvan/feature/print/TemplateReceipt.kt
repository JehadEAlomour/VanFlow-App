package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.print_company_tax_number
import com.jehadalomour.flowvan.core.designsystem.resources.print_customer_tax_number
import com.jehadalomour.flowvan.core.designsystem.resources.print_tax_invoice_title
import com.jehadalomour.flowvan.core.designsystem.resources.voucher_tax_exempt_number
import com.jehadalomour.flowvan.core.designsystem.theme.almaraiFamily
import com.jehadalomour.flowvan.core.model.print.Element
import com.jehadalomour.flowvan.core.model.print.ElementProps
import com.jehadalomour.flowvan.core.model.print.ElementType
import com.jehadalomour.flowvan.core.model.print.TableColumn
import com.jehadalomour.flowvan.core.model.print.Template
import com.jehadalomour.flowvan.core.model.print.TemplateContext
import com.jehadalomour.flowvan.core.model.print.TemplateTokens
import com.jehadalomour.flowvan.core.model.print.Zone
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource

// ── Scale ─────────────────────────────────────────────────────────────────────

/** 1 pt = 25.4/72 mm (spec §3). */
private const val MM_PER_PT = 25.4 / 72.0

/**
 * Converts template units to screen units for ONE paper width. The composable's width is
 * the paper, so `pxPerMm = widthPx / layout.width`; every mm and every pt goes through it,
 * which is what keeps an 80 mm design at 320 dp and the same design captured for a 576-dot
 * print head proportionally identical.
 */
private class TemplateScale(private val pxPerMm: Float, private val density: Density) {
    fun mm(v: Double): Dp = with(density) { (v * pxPerMm).toFloat().toDp() }
    fun pt(v: Double): Dp = mm(v * MM_PER_PT)
    fun ptPx(v: Double): Float = (v * MM_PER_PT * pxPerMm).toFloat()
    fun ptSp(v: Double): TextUnit = with(density) { ptPx(v).toSp() }
}

private val LocalTemplateScale = staticCompositionLocalOf<TemplateScale> {
    error("TemplateScale is provided by TemplateReceipt")
}

/** Latin digits in every locale, whatever the paragraph direction. */
private val LatinDigits = LocaleList("en-US")

/** The table/totals/tax block sizes the spec leaves to the renderer. */
private const val TABLE_FONT_PT = 9.0
private const val TOTALS_FONT_PT = 10.0
private const val RULE_PT = 0.5

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * Renders a dashboard-designed [template] at whatever width it is given — that width IS the
 * paper (80 mm for THERMAL_80, 210 mm for A4), so callers size the container, never this.
 *
 * Layout follows spec §3: the three zones stacked, header/footer at least their minHeight,
 * the body as tall as its content, page padding = margins. Fixed elements (LOGO, TEXT,
 * DIVIDER, SPACER, QR_CODE) sit at (x, y) inside their zone; flow elements (ITEMS_TABLE,
 * TOTALS_BLOCK, TAX_INVOICE) stack in element order, each shifted by its (x, y). Geometry is
 * always left-to-right — x is from the left edge — while text direction is per element.
 *
 * [monochrome] drops element colours to black: a thermal head prints black regardless, and
 * the same composable is what gets captured, so the preview shows what the roll will show.
 */
@Composable
fun TemplateReceipt(
    template: Template,
    ctx: TemplateContext,
    modifier: Modifier = Modifier,
    monochrome: Boolean = template.isThermal,
    isArabic: Boolean = true,
) {
    BoxWithConstraints(modifier.fillMaxWidth().background(Color.White)) {
        val density = LocalDensity.current
        val page = template.layout.layout
        val paperMm = page.width.takeIf { it > 0.0 } ?: 80.0
        val widthPx = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: 1f
        val scale = remember(widthPx, paperMm, density) { TemplateScale(widthPx / paperMm.toFloat(), density) }

        CompositionLocalProvider(
            LocalTemplateScale provides scale,
            LocalLayoutDirection provides LayoutDirection.Ltr,
        ) {
            val m = page.margins
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = scale.mm(m.left), top = scale.mm(m.top), end = scale.mm(m.right), bottom = scale.mm(m.bottom)),
            ) {
                Zone.ALL.forEach { zone ->
                    ZoneBox(zone, template, ctx, monochrome, isArabic)
                }
            }
        }
    }
}

// ── Zones ─────────────────────────────────────────────────────────────────────

@Composable
private fun ZoneBox(
    zone: String,
    template: Template,
    ctx: TemplateContext,
    monochrome: Boolean,
    isArabic: Boolean,
) {
    val scale = LocalTemplateScale.current
    val spec = template.layout.layout.zones.spec(zone)
    val elements = template.layout.elementsIn(zone).filter { it.isSupported }
    val flow = elements.filter { it.isFlow }
    val fixed = elements.filter { !it.isFlow }

    Box(Modifier.fillMaxWidth().heightIn(min = scale.mm(spec.minHeight))) {
        // Flow: top to bottom in element order. The (x, y) is a shift from where the flow
        // put the element and does not move the ones after it (CSS "relative").
        if (flow.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                flow.forEach { el ->
                    Box(
                        Modifier
                            .absoluteOffset(x = scale.mm(el.x), y = scale.mm(el.y))
                            .width(scale.mm(el.width))
                            .clipToBounds(),
                    ) {
                        FlowElement(el, ctx, monochrome, isArabic)
                    }
                }
            }
        }
        // Fixed: absolute at (x, y). They neither grow the zone nor push flow content —
        // the zone's minHeight is what makes room for them (spec §3 convention), so a
        // header holding only fixed text is exactly as tall as the designer said.
        fixed.forEach { el ->
            Box(
                Modifier
                    .absoluteOffset(x = scale.mm(el.x), y = scale.mm(el.y))
                    .size(width = scale.mm(el.width), height = scale.mm(el.height ?: 0.0))
                    .clipToBounds(),
            ) {
                FixedElement(el, ctx, monochrome, isArabic)
            }
        }
    }
}

// ── Fixed elements ────────────────────────────────────────────────────────────

@Composable
private fun FixedElement(el: Element, ctx: TemplateContext, monochrome: Boolean, isArabic: Boolean) {
    when (el.type) {
        ElementType.TEXT -> TemplateText(el.props, TemplateTokens.resolve(el.props.content, ctx), monochrome)
        ElementType.LOGO -> TemplateLogo(el.props, ctx)
        ElementType.DIVIDER -> TemplateDivider(el.props, monochrome)
        ElementType.SPACER -> Spacer(Modifier.fillMaxSize())
        ElementType.QR_CODE -> TemplateQr(el.props, ctx)
    }
}

@Composable
private fun TemplateText(props: ElementProps, text: String, monochrome: Boolean, modifier: Modifier = Modifier) {
    if (text.isEmpty()) return
    val scale = LocalTemplateScale.current
    val direction = when (props.direction) {
        "rtl" -> TextDirection.Rtl
        "ltr" -> TextDirection.Ltr
        else -> TextDirection.Content
    }
    val align = when (props.align) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        "left" -> TextAlign.Left
        else -> if (direction == TextDirection.Rtl) TextAlign.Right else TextAlign.Start
    }
    val family: FontFamily? = when (props.fontFamily) {
        "arabic" -> almaraiFamily()
        "mono" -> FontFamily.Monospace
        "sans" -> FontFamily.SansSerif
        else -> null
    }
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = TextStyle(
            fontSize = scale.ptSp(props.fontSizePt),
            lineHeight = scale.ptSp(props.fontSizePt * props.lineHeightFactor),
            fontWeight = if (props.isBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = family,
            color = inkColor(props.color, monochrome),
            textAlign = align,
            textDirection = direction,
            localeList = LatinDigits,
        ),
    )
}

@Composable
private fun TemplateLogo(props: ElementProps, ctx: TemplateContext) {
    val bitmap = remember(ctx.companyLogo) { decodeBase64Image(ctx.companyLogo) } ?: return
    val fit = when (props.fit) {
        "cover" -> ContentScale.Crop
        "fill" -> ContentScale.FillBounds
        else -> ContentScale.Fit
    }
    val alignment = when (props.align) {
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = fit,
        alignment = alignment,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TemplateDivider(props: ElementProps, monochrome: Boolean) {
    val scale = LocalTemplateScale.current
    val color = inkColor(props.color, monochrome)
    val strokePx = scale.ptPx(RULE_PT).coerceAtLeast(1f)
    Canvas(Modifier.fillMaxSize()) {
        val y = size.height / 2f
        val effect = when (props.style) {
            "dashed" -> PathEffect.dashPathEffect(floatArrayOf(strokePx * 6f, strokePx * 4f))
            "dotted" -> PathEffect.dashPathEffect(floatArrayOf(strokePx, strokePx * 3f))
            else -> null
        }
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokePx,
            pathEffect = effect,
            cap = if (props.style == "dotted") StrokeCap.Round else StrokeCap.Butt,
        )
    }
}

/**
 * QR from a token or literal. A `data:` image is drawn as-is; a plain string is encoded.
 * An `http(s)` URL cannot be fetched by this offline renderer and is skipped.
 */
@Composable
private fun TemplateQr(props: ElementProps, ctx: TemplateContext) {
    val data = TemplateTokens.resolve(props.data, ctx).trim()
    if (data.isEmpty()) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            data.startsWith("data:") -> {
                val bmp = remember(data) { decodeBase64Image(data) } ?: return
                Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            data.startsWith("http://") || data.startsWith("https://") -> Unit
            else -> Image(
                painter = rememberQrCodePainter(data),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Flow elements ─────────────────────────────────────────────────────────────

@Composable
private fun FlowElement(el: Element, ctx: TemplateContext, monochrome: Boolean, isArabic: Boolean) {
    when (el.type) {
        ElementType.ITEMS_TABLE -> ItemsTable(el.props, ctx, monochrome, isArabic)
        ElementType.TOTALS_BLOCK -> TotalsBlock(el.props, ctx, monochrome, isArabic)
        ElementType.TAX_INVOICE -> TaxInvoiceBlock(ctx, monochrome)
        // REPORT_TABLE / SHIFTS_TABLE carry report data a voucher does not have.
        else -> Unit
    }
}

/** Columns when the template names none — the built-in receipt's own four. */
private val DefaultColumns = listOf(
    TableColumn("name", "Item", "الصنف", 3.0, "right"),
    TableColumn("qty", "Qty", "الكمية", 1.0, "center"),
    TableColumn("price", "Price", "السعر", 1.5, "center"),
    TableColumn("total", "Total", "المجموع", 1.5, "center"),
)

@Composable
private fun ItemsTable(props: ElementProps, ctx: TemplateContext, monochrome: Boolean, isArabic: Boolean) {
    val scale = LocalTemplateScale.current
    val columns = (props.columns?.takeIf { it.isNotEmpty() } ?: DefaultColumns)
        .filter { !it.hide && it.width > 0.0 }
    if (columns.isEmpty()) return
    val ink = inkColor(props.color, monochrome)
    val cellStyle = TextStyle(
        fontSize = scale.ptSp(TABLE_FONT_PT),
        lineHeight = scale.ptSp(TABLE_FONT_PT * ElementProps.DEFAULT_LINE_HEIGHT),
        color = ink,
        localeList = LatinDigits,
    )
    val rulePx = scale.ptPx(RULE_PT).coerceAtLeast(1f)
    val cellPad = scale.pt(1.5)

    // An Arabic receipt reads its first column on the right, exactly as an RTL table does.
    CompositionLocalProvider(LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                columns.forEach { col ->
                    Text(
                        text = if (isArabic) col.labelAr.ifBlank { col.labelEn } else col.labelEn.ifBlank { col.labelAr },
                        modifier = Modifier.weight(col.width.toFloat()).padding(horizontal = cellPad, vertical = cellPad),
                        style = cellStyle.copy(fontWeight = FontWeight.Bold, textAlign = cellAlign(col.align)),
                    )
                }
            }
            Rule(ink, rulePx)
            ctx.lines.forEach { line ->
                Row(Modifier.fillMaxWidth()) {
                    columns.forEach { col ->
                        val numeric = col.key != "name" && col.key != "unit"
                        Text(
                            text = TemplateTokens.cell(col.key, line, ctx),
                            modifier = Modifier.weight(col.width.toFloat()).padding(horizontal = cellPad, vertical = cellPad),
                            style = cellStyle.copy(
                                textAlign = cellAlign(col.align),
                                textDirection = if (numeric) TextDirection.Ltr else TextDirection.Content,
                            ),
                        )
                    }
                }
            }
            Rule(ink, rulePx)
        }
    }
}

@Composable
private fun TotalsBlock(props: ElementProps, ctx: TemplateContext, monochrome: Boolean, isArabic: Boolean) {
    val scale = LocalTemplateScale.current
    val rows = props.rows.orEmpty().filter { !it.hide }
    if (rows.isEmpty()) return
    val ink = inkColor(props.color, monochrome)
    val style = TextStyle(
        fontSize = scale.ptSp(TOTALS_FONT_PT),
        lineHeight = scale.ptSp(TOTALS_FONT_PT * ElementProps.DEFAULT_LINE_HEIGHT),
        color = ink,
        localeList = LatinDigits,
    )
    val rulePx = scale.ptPx(RULE_PT).coerceAtLeast(1f)
    val rowPad = scale.pt(1.0)

    CompositionLocalProvider(LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                if (row.isBold) Rule(ink, rulePx)
                val weight = if (row.isBold) FontWeight.Bold else FontWeight.Normal
                Row(
                    Modifier.fillMaxWidth().padding(vertical = rowPad),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (isArabic) row.labelAr.ifBlank { row.label } else row.label.ifBlank { row.labelAr },
                        style = style.copy(fontWeight = weight),
                    )
                    Text(
                        text = TemplateTokens.resolve(row.value, ctx),
                        style = style.copy(fontWeight = weight, textDirection = TextDirection.Ltr),
                    )
                }
            }
        }
    }
}

/**
 * The tax-invoice block: the two tax registrations and, on an exempt voucher, the stamp
 * and the exemption number. The spec names the element without describing it, so this is
 * the block the built-in receipt prints for the same purpose.
 */
@Composable
private fun TaxInvoiceBlock(ctx: TemplateContext, monochrome: Boolean) {
    val scale = LocalTemplateScale.current
    val ink = inkColor(null, monochrome)
    val style = TextStyle(
        fontSize = scale.ptSp(TOTALS_FONT_PT),
        lineHeight = scale.ptSp(TOTALS_FONT_PT * ElementProps.DEFAULT_LINE_HEIGHT),
        color = ink,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Content,
        localeList = LatinDigits,
    )
    val lines = buildList {
        if (ctx.isTaxExempt) {
            if (ctx.taxExemptStamp.isNotBlank()) add(ctx.taxExemptStamp)
            if (ctx.taxExemptionNumber.isNotBlank()) add("${stringResource(Res.string.voucher_tax_exempt_number)}: ${ctx.taxExemptionNumber}")
        } else {
            add(stringResource(Res.string.print_tax_invoice_title))
        }
        if (ctx.companyTaxNumber.isNotBlank()) add("${stringResource(Res.string.print_company_tax_number)}: ${ctx.companyTaxNumber}")
        if (ctx.customerTaxNumber.isNotBlank()) add("${stringResource(Res.string.print_customer_tax_number)}: ${ctx.customerTaxNumber}")
    }
    if (lines.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .border(scale.pt(RULE_PT * 2), ink)
            .padding(scale.pt(2.0)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        lines.forEachIndexed { i, text ->
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = if (i == 0) style.copy(fontWeight = FontWeight.Bold) else style,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun Rule(color: Color, strokePx: Float) {
    val scale = LocalTemplateScale.current
    Canvas(Modifier.fillMaxWidth().height(scale.pt(RULE_PT).coerceAtLeast(1.dp))) {
        drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = strokePx)
    }
}

private fun cellAlign(align: String?): TextAlign = when (align) {
    "left" -> TextAlign.Left
    "right" -> TextAlign.Right
    "center" -> TextAlign.Center
    else -> TextAlign.Start
}

/** Black on a monochrome device; the element's CSS hex otherwise (black when unparseable). */
private fun inkColor(hex: String?, monochrome: Boolean): Color =
    if (monochrome) Color.Black else parseHexColor(hex) ?: Color.Black

internal fun parseHexColor(hex: String?): Color? {
    val h = hex?.trim()?.removePrefix("#") ?: return null
    val rgb = when (h.length) {
        3 -> h.map { "$it$it" }.joinToString("")
        6 -> h
        else -> return null
    }
    val value = rgb.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or value)
}

package com.jehadalomour.flowvan.core.domain.printer

/**
 * Platform-agnostic description of what to print on a POS / thermal printer.
 *
 * Build one with the [buildPrintContent] DSL and hand it to [ReceiptPrinter.print].
 * The same model serves a sales voucher, an end-of-day summary, a test page, or any
 * ad-hoc content the app wants on paper. Contains no UI types so it lives in `:shared`
 * and can be driven entirely from a ViewModel.
 */
data class PrintContent(
    val paperWidth: PaperWidth = PaperWidth.MM80,
    val nodes: List<PrintNode>,
)

/** Physical roll width. [dots] = print head resolution, [charsPerLine] = monospaced font A. */
enum class PaperWidth(val dots: Int, val charsPerLine: Int) {
    MM58(384, 32),
    MM80(576, 48),
}

enum class PrintAlign { LEFT, CENTER, RIGHT }

enum class BarcodeType { UPCA, UPCE, EAN13, EAN8, CODE39, ITF, CODABAR, CODE93, CODE128 }

/** A single printable element. */
sealed interface PrintNode {

    /**
     * One line of text. [widthScale]/[heightScale] are 1..8 character magnification.
     * Note: built-in printer fonts do not shape Arabic — for Arabic content render it
     * to an [Image] instead of using [Text].
     */
    data class Text(
        val text: String,
        val align: PrintAlign = PrintAlign.LEFT,
        val bold: Boolean = false,
        val underline: Boolean = false,
        val widthScale: Int = 1,
        val heightScale: Int = 1,
    ) : PrintNode

    /** A raster image (PNG bytes), scaled to fit the paper width. The Arabic-safe way to print text. */
    data class Image(
        val png: ByteArray,
        val align: PrintAlign = PrintAlign.CENTER,
    ) : PrintNode {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Image && png.contentEquals(other.png) && align == other.align)

        override fun hashCode(): Int = 31 * png.contentHashCode() + align.hashCode()
    }

    data class QrCode(
        val data: String,
        val moduleSize: Int = 6,
        val align: PrintAlign = PrintAlign.CENTER,
    ) : PrintNode

    data class Barcode(
        val data: String,
        val type: BarcodeType = BarcodeType.CODE128,
        val height: Int = 80,
        val align: PrintAlign = PrintAlign.CENTER,
        val showText: Boolean = true,
    ) : PrintNode

    /** A full-width separator line made of [char]. */
    data class Divider(val char: Char = '-') : PrintNode

    /** Advance the paper by [lines] blank lines. */
    data class Feed(val lines: Int = 1) : PrintNode

    /** Cut the paper. [partial] = leave a small tab connected. */
    data class Cut(val partial: Boolean = false) : PrintNode

    /** Pop the cash drawer connected to the printer. */
    data object OpenCashDrawer : PrintNode
}

// ── DSL ─────────────────────────────────────────────────────────────────────

class PrintContentBuilder(private val paperWidth: PaperWidth) {
    private val nodes = mutableListOf<PrintNode>()

    fun text(
        text: String,
        align: PrintAlign = PrintAlign.LEFT,
        bold: Boolean = false,
        underline: Boolean = false,
        widthScale: Int = 1,
        heightScale: Int = 1,
    ) {
        nodes += PrintNode.Text(text, align, bold, underline, widthScale, heightScale)
    }

    /** Centered, bold, double-size heading. */
    fun title(text: String) =
        text(text, align = PrintAlign.CENTER, bold = true, widthScale = 2, heightScale = 2)

    /** Left key + right value on one line, padded to the paper width (monospaced). */
    fun keyValue(key: String, value: String, bold: Boolean = false) {
        val width = paperWidth.charsPerLine
        val space = (width - key.length - value.length).coerceAtLeast(1)
        text(key + " ".repeat(space) + value, bold = bold)
    }

    fun image(png: ByteArray, align: PrintAlign = PrintAlign.CENTER) {
        nodes += PrintNode.Image(png, align)
    }

    fun qrCode(data: String, moduleSize: Int = 6, align: PrintAlign = PrintAlign.CENTER) {
        nodes += PrintNode.QrCode(data, moduleSize, align)
    }

    fun barcode(
        data: String,
        type: BarcodeType = BarcodeType.CODE128,
        height: Int = 80,
        align: PrintAlign = PrintAlign.CENTER,
        showText: Boolean = true,
    ) {
        nodes += PrintNode.Barcode(data, type, height, align, showText)
    }

    fun divider(char: Char = '-') { nodes += PrintNode.Divider(char) }

    fun feed(lines: Int = 1) { nodes += PrintNode.Feed(lines) }

    fun cut(partial: Boolean = false) { nodes += PrintNode.Cut(partial) }

    fun openCashDrawer() { nodes += PrintNode.OpenCashDrawer }

    fun build() = PrintContent(paperWidth, nodes.toList())
}

/**
 * Build a [PrintContent] declaratively:
 * ```
 * val content = buildPrintContent {
 *     title("FlowVan")
 *     divider()
 *     keyValue("Total", "120.000")
 *     qrCode("https://...")
 *     feed(2); cut()
 * }
 * printer.print(content)
 * ```
 */
fun buildPrintContent(
    paperWidth: PaperWidth = PaperWidth.MM80,
    block: PrintContentBuilder.() -> Unit,
): PrintContent = PrintContentBuilder(paperWidth).apply(block).build()

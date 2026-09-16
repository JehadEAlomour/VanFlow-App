package com.jehadalomour.flowvan.feature.print

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How a receipt has to be drawn if a thermal head is going to print it.
 *
 * A thermal head is one bit per dot: it burns a dot or it leaves the paper
 * alone. There is no grey. Everything that is neither black nor white — a
 * #888888 label, the soft edge of small antialiased type, a tinted panel — has
 * to be resolved into one or the other, and the printer resolves it by
 * scattering dots to approximate the tone. On screen that is a smooth grey; on
 * an 80mm roll it is speckle. It is why these receipts printed as noise with
 * the horizontal rules still running straight through them: solid black was the
 * only thing on the page the printer had no decision to make about.
 *
 * So hierarchy on a receipt cannot be carried by colour, the way it is in the
 * app. It has to come from size, weight, case and rules — the things that are
 * still there after every pixel has been forced to black or white.
 *
 * These are the sizes that survive. They are large compared to a screen, and
 * they are meant to be: a 384dp-wide paper is about 80mm of roll, so a 13sp
 * label lands at roughly 2.7mm tall — fine on a phone held at reading distance,
 * and not enough dots to hold the dots and strokes that distinguish one Arabic
 * letter from another.
 */
object ThermalInk {
    /** The only ink there is. */
    val Ink = Color.Black
    val Paper = Color.White

    /** Company name at the head of the receipt. */
    const val FS_COMPANY = 30

    /** Branch, tax number — the line under the company name. */
    const val FS_SUB = 18

    /** The document's title tag. */
    const val FS_TITLE = 22

    /** Section headings inside the body. */
    const val FS_SECTION = 20

    /** Ordinary label/value rows — the bulk of a receipt. */
    const val FS_ROW = 18

    /** A row that carries a total. */
    const val FS_TOTAL = 22

    /** Table column headings. */
    const val FS_HEAD = 17

    /** The smallest type allowed on a receipt. Below this Arabic stops resolving. */
    const val FS_MIN = 16

    /** Rules are drawn, not typed: solid black, thick enough to survive. */
    val RuleThickness: Dp = 2.dp
}

/**
 * The width, in dots, that captured paper is rendered at.
 *
 * 576 is an 80mm head. A 58mm head takes 384 and the printer scales this down
 * to it, which is safe — detail is being given up, not invented. Rendering at
 * the narrower number instead would mean enlarging for every 80mm printer in
 * the fleet, and enlarging cannot add back what it did not draw.
 */
const val THERMAL_DOTS = 576

/**
 * Draws [content] off-screen at exactly [dots] pixels wide and records it into
 * [layer] for printing.
 *
 * This exists because of what a capture's resolution actually depends on. The
 * bitmap comes out at the paper's dp width times the SCREEN's density, so the
 * same receipt is 1008 pixels wide on a modern phone and 384 on the Sunmi
 * terminal — for the same 80mm of paper. The terminal was handing the printer a
 * third of the detail and there was nothing the print path could do about it,
 * because the dots had never been drawn.
 *
 * Pinning the density makes the capture a fact about the paper instead of a
 * fact about the device: [paperDp] at this density is exactly [dots] pixels, on
 * every phone and every terminal.
 *
 * The font scale is pinned to 1 for the same reason. It is a display preference
 * — someone who sets large text wants the app easier to read, not the totals
 * column pushed off the side of a receipt.
 *
 * The paper is measured at its natural size with unbounded height and then
 * reports zero size to its parent, so it occupies no space on screen while
 * still being drawn in full into the layer. Wrapping it in a plain `size(0)`
 * would instead clamp its height to nothing, and capturing a 0-high layer
 * crashes.
 */
@Composable
fun ThermalCapture(
    layer: GraphicsLayer,
    modifier: Modifier = Modifier,
    paperDp: Dp = 384.dp,
    dots: Int = THERMAL_DOTS,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.layout { measurable, _ ->
            val placeable = measurable.measure(Constraints())
            layout(0, 0) { placeable.place(0, 0) }
        },
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = dots / paperDp.value, fontScale = 1f),
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(paperDp)
                    // Order matters twice over. `drawWithContent` records what is
                    // INSIDE it and draws nothing to the screen, so the paper's
                    // white ground has to sit after it to be part of the capture
                    // — put it before and the receipt records on a transparent
                    // ground while the white rectangle paints itself onto the
                    // screen underneath the preview, which is exactly what it did.
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                    }
                    .background(ThermalInk.Paper),
            ) {
                content()
            }
        }
    }
}

/**
 * The on-screen preview of the same paper.
 *
 * Deliberately a separate call from [ThermalCapture]: the preview is sized to
 * the phone and drawn at the phone's density, while the capture is sized to the
 * head. Trying to serve both from one subtree is what tied the printed
 * resolution to whatever screen the app happened to be running on.
 */
@Composable
fun ThermalPreview(
    modifier: Modifier = Modifier,
    paperDp: Dp = 384.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.requiredWidth(paperDp).background(ThermalInk.Paper)) {
        content()
    }
}

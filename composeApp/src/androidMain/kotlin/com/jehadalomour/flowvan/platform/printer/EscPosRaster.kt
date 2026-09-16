package com.jehadalomour.flowvan.platform.printer

import android.graphics.Bitmap

/**
 * Turns a bitmap into ESC/POS raster commands — every byte of them.
 *
 * The SDK has its own `printBitmap`, and we used it for a long time. On the
 * Sunmi terminal it returned pages of speckle with the horizontal rules still
 * printing straight through the middle of it: the solid black survived and
 * everything that needed a decision did not. What decision it was making, and
 * on which of scaling, dithering or packing it was making it, is not something
 * the jar will say — it is obfuscated, and no combination of paper width,
 * image size or print mode changed the result.
 *
 * So this stops asking it. A thermal head takes one bit per dot, `GS v 0`
 * carries those bits, and both are completely specified. We threshold, pack and
 * send; there is no conversion left in between to get it wrong.
 */
internal object EscPosRaster {

    // ESC @ — reset. Clears whatever mode a previous job left behind.
    private val INIT = byteArrayOf(0x1B, 0x40)

    /**
     * Rows per `GS v 0` command.
     *
     * The command's height field would allow 65535, but a printer only has so
     * much buffer and a receipt is thousands of rows; asking for all of them at
     * once is how a print ends up truncated or garbled on a small head. Slices
     * abut exactly — the raster leaves the print position at the start of the
     * next row — so the page is continuous regardless of where they fall.
     */
    private const val SLICE_ROWS = 64

    /** ESC a n — 0 left, 1 centre, 2 right. */
    fun align(n: Int): ByteArray = byteArrayOf(0x1B, 0x61, n.toByte())

    /** ESC d n — feed n lines, to clear the tear bar. */
    fun feed(lines: Int): ByteArray = byteArrayOf(0x1B, 0x64, lines.toByte())

    /** GS V 1 — partial cut. */
    fun cut(): ByteArray = byteArrayOf(0x1D, 0x56, 0x01)

    fun init(): ByteArray = INIT

    /**
     * The bitmap as a list of `GS v 0` commands, one per slice.
     *
     * Anything at or below [threshold] luminance burns. The cutoff is
     * deliberately high: at this size an Arabic stroke is mostly antialiased
     * edge, and a strict threshold eats the letter rather than the fringe.
     * Fully transparent pixels are paper — a rounded corner is a hole in the
     * image, not ink.
     */
    fun rasterCommands(bitmap: Bitmap, threshold: Int = 0xB0): List<ByteArray> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return pack(pixels, width, height, threshold)
    }

    /**
     * The packing itself, over plain ARGB pixels so it can be tested without a
     * device. Every byte the printer receives for the image is decided here.
     */
    fun pack(pixels: IntArray, width: Int, height: Int, threshold: Int = 0xB0): List<ByteArray> {
        // A raster row is whole bytes, 8 dots each. A width that is not a
        // multiple of 8 is padded with white on the right rather than rounded,
        // so nothing shifts: every row must start on the same dot column as the
        // one above it or the page shears.
        val widthBytes = (width + 7) / 8

        val commands = ArrayList<ByteArray>((height + SLICE_ROWS - 1) / SLICE_ROWS)
        var top = 0
        while (top < height) {
            val rows = minOf(SLICE_ROWS, height - top)
            val out = ByteArray(8 + widthBytes * rows)
            // GS v 0 m xL xH yL yH — m=0 is unscaled; x is in BYTES, y in dots.
            out[0] = 0x1D
            out[1] = 0x76
            out[2] = 0x30
            out[3] = 0x00
            out[4] = (widthBytes and 0xFF).toByte()
            out[5] = ((widthBytes shr 8) and 0xFF).toByte()
            out[6] = (rows and 0xFF).toByte()
            out[7] = ((rows shr 8) and 0xFF).toByte()

            var o = 8
            for (y in top until top + rows) {
                val rowStart = y * width
                for (bx in 0 until widthBytes) {
                    var bits = 0
                    val x0 = bx * 8
                    for (bit in 0 until 8) {
                        val x = x0 + bit
                        if (x >= width) break // past the edge: leave it white
                        val c = pixels[rowStart + x]
                        val a = (c ushr 24) and 0xFF
                        if (a < 0x80) continue // transparent is paper
                        val r = (c ushr 16) and 0xFF
                        val g = (c ushr 8) and 0xFF
                        val b = c and 0xFF
                        // Weighted luminance: a plain average prints red and
                        // blue text as mud.
                        val lum = (r * 299 + g * 587 + b * 114) / 1000
                        // MSB is the leftmost dot of the byte.
                        if (lum <= threshold) bits = bits or (0x80 ushr bit)
                    }
                    out[o++] = bits.toByte()
                }
            }
            commands += out
            top += rows
        }
        return commands
    }
}

package com.jehadalomour.flowvan.platform.printer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The printer sees only these bytes, and a receipt that comes out as speckle
 * looks exactly the same whether the image was wrong or the packing was. These
 * pin the packing, so the next time a print is wrong we can rule it out in a
 * second instead of guessing at it for three rounds.
 */
class EscPosRasterTest {

    private val BLACK = 0xFF000000.toInt()
    private val WHITE = 0xFFFFFFFF.toInt()

    private fun header(cmd: ByteArray) = cmd.copyOfRange(0, 8).map { it.toInt() and 0xFF }
    private fun data(cmd: ByteArray) = cmd.copyOfRange(8, cmd.size).map { it.toInt() and 0xFF }

    @Test
    fun `emits a GS v 0 header with width in bytes and height in dots`() {
        val cmds = EscPosRaster.pack(IntArray(16 * 2) { WHITE }, width = 16, height = 2)
        assertEquals(1, cmds.size)
        // GS v 0, m=0, xL=2 xH=0 (16 dots = 2 bytes), yL=2 yH=0
        assertEquals(listOf(0x1D, 0x76, 0x30, 0x00, 2, 0, 2, 0), header(cmds[0]))
        assertEquals(2 * 2, data(cmds[0]).size)
    }

    @Test
    fun `the most significant bit is the leftmost dot`() {
        // One row, 8 dots, only the leftmost black.
        val pixels = IntArray(8) { WHITE }
        pixels[0] = BLACK
        assertEquals(listOf(0x80), data(EscPosRaster.pack(pixels, width = 8, height = 1)[0]))

        // Only the rightmost black.
        val right = IntArray(8) { WHITE }
        right[7] = BLACK
        assertEquals(listOf(0x01), data(EscPosRaster.pack(right, width = 8, height = 1)[0]))
    }

    @Test
    fun `a solid black row is every bit set - this is the rule that always printed`() {
        val cmds = EscPosRaster.pack(IntArray(24) { BLACK }, width = 24, height = 1)
        assertEquals(listOf(0xFF, 0xFF, 0xFF), data(cmds[0]))
    }

    @Test
    fun `a width that is not a multiple of 8 pads with white on the right`() {
        // 12 dots, all black: 1 full byte then a half byte. If the padding were
        // black instead, every row would carry 4 dots of ink past the content
        // and the page would grow a black right margin.
        val cmds = EscPosRaster.pack(IntArray(12) { BLACK }, width = 12, height = 1)
        assertEquals(listOf(0x1D, 0x76, 0x30, 0x00, 2, 0, 1, 0), header(cmds[0]))
        assertEquals(listOf(0xFF, 0xF0), data(cmds[0]))
    }

    @Test
    fun `rows stay in order and do not shift - a shear here is the classic garbled print`() {
        // Row 0 leftmost dot, row 1 rightmost. If the rows sheared, these swap.
        val pixels = IntArray(16) { WHITE }
        pixels[0] = BLACK      // row 0, x=0
        pixels[8 + 7] = BLACK  // row 1, x=7
        val cmds = EscPosRaster.pack(pixels, width = 8, height = 2)
        assertEquals(listOf(0x80, 0x01), data(cmds[0]))
    }

    @Test
    fun `transparent is paper, not ink`() {
        // A captured layer is transparent outside the paper. Treating alpha as
        // ink would print a solid block wherever the receipt did not draw.
        val clear = IntArray(8) { 0x00000000 }
        assertEquals(listOf(0x00), data(EscPosRaster.pack(clear, width = 8, height = 1)[0]))
    }

    @Test
    fun `mid grey burns but light grey does not`() {
        val mid = IntArray(8) { WHITE }
        mid[0] = 0xFF808080.toInt()   // 50% grey — the worst case for dithering
        mid[1] = 0xFFE8E8E8.toInt()   // near-white
        assertEquals(listOf(0x80), data(EscPosRaster.pack(mid, width = 8, height = 1)[0]))
    }

    @Test
    fun `a tall image is sliced, and the slices reassemble to the whole`() {
        val height = 150
        val pixels = IntArray(8 * height) { WHITE }
        // One black dot per row, marching right, so a dropped or reordered
        // slice is visible rather than plausible.
        for (y in 0 until height) pixels[y * 8 + (y % 8)] = BLACK

        val cmds = EscPosRaster.pack(pixels, width = 8, height = height)
        assertEquals(3, cmds.size) // 64 + 64 + 22

        val rows = cmds.flatMap { cmd ->
            val h = (cmd[6].toInt() and 0xFF) or ((cmd[7].toInt() and 0xFF) shl 8)
            assertEquals(1, (cmd[4].toInt() and 0xFF)) // 8 dots = 1 byte wide
            (0 until h).map { cmd[8 + it].toInt() and 0xFF }
        }
        assertEquals(height, rows.size)
        rows.forEachIndexed { y, bits -> assertEquals(0x80 ushr (y % 8), bits, "row $y") }
    }

    @Test
    fun `commands carry no stray bytes between them`() {
        // Each command is exactly its header plus its own data: anything extra
        // would be interpreted as ESC/POS text and print as junk characters.
        EscPosRaster.pack(IntArray(576 * 200) { WHITE }, width = 576, height = 200).forEach {
            val h = (it[6].toInt() and 0xFF) or ((it[7].toInt() and 0xFF) shl 8)
            assertEquals(8 + 72 * h, it.size)
        }
    }

    @Test
    fun `alignment and feed are the documented escape sequences`() {
        assertEquals(listOf(0x1B, 0x61, 0x01), EscPosRaster.align(1).map { it.toInt() and 0xFF })
        assertEquals(listOf(0x1B, 0x64, 0x03), EscPosRaster.feed(3).map { it.toInt() and 0xFF })
        assertEquals(listOf(0x1B, 0x40), EscPosRaster.init().map { it.toInt() and 0xFF })
        assertTrue(EscPosRaster.cut().isNotEmpty())
    }
}

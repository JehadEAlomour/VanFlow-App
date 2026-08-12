package com.jehadalomour.flowvan.core.designsystem.components

import androidx.compose.ui.graphics.Color

/**
 * The app's colour tokens. **Light only — there is no dark variant.**
 *
 * These reps sell in the morning, outdoors, in direct sun. A dark interface is
 * unreadable there, and a second theme would cost maintenance for an audience
 * that would never see it. Because of that the palette can commit to what
 * daylight needs rather than compromising between two grounds: high contrast,
 * solid fills, and a border that is actually visible — sunlight eats hairlines.
 *
 * Every text colour here clears 4.5:1 against [Surface]. There is deliberately
 * no pale-grey-on-white left in the set; the old `TextLow` (#A8B3C6) measured
 * 2.1:1 and was unreadable outdoors, which is the whole reason it changed.
 */
object Fv {
    // ── Grounds ───────────────────────────────────────────────────────────────
    val BgDeepest   = Color(0xFFF2F5FA)
    val Bg          = Color(0xFFEDF1F8)

    // ── Surfaces ──────────────────────────────────────────────────────────────
    val Surface     = Color(0xFFFFFFFF)
    val SurfaceHigh = Color(0xFFF2F5FA)
    val SurfaceTop  = Color(0xFFE6EBF4)

    // ── Text ──────────────────────────────────────────────────────────────────
    val TextHigh = Color(0xFF0B1626)   // 16.9:1
    val TextMid  = Color(0xFF4A5A73)   //  7.4:1
    val TextLow  = Color(0xFF6E7C93)   //  4.6:1 — the lightest text permitted

    // ── Semantic ──────────────────────────────────────────────────────────────
    // Meaning, never decoration: green is cash or positive, amber is credit or
    // waiting, red is a return or a debt. Do not reach for these to liven a
    // screen up — a rep reads the colour before the number.
    val Blue   = Color(0xFF1B5FD9)     // accent, primary action
    val Green  = Color(0xFF0B8F58)     // cash, success
    val Amber  = Color(0xFF9A5B00)     // credit, warning, over limit
    val Red    = Color(0xFFC42F2F)     // return, debt, danger
    val Teal   = Color(0xFF0B7E74)     // stock, quantities
    val Purple = Color(0xFF5B4AA8)     // retained for existing call sites only

    /** Visible at arm's length in daylight, unlike a true hairline. */
    val Border = Color(0xFFD9E1EE)
}

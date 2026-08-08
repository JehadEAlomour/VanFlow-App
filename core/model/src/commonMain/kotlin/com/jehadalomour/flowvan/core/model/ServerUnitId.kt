package com.jehadalomour.flowvan.core.model

/**
 * True when this is a server `item_units.id` rather than a locally synthesised one.
 *
 * [ProductUnit.id] is the server's uuid for a real unit row, but the catalogue
 * falls back to the barcode — and then to `"productId:code:conversionQty"` — for
 * units the server describes without one. Above all the BASE unit, which has no
 * `item_units` row at all and so ends up carrying the item's barcode, which for
 * most items IS its sku.
 *
 * Every API that accepts `itemUnitId` validates it as a UUID, so those fallbacks
 * must never be sent. Omitting it means "the item's base pool", which is exactly
 * what a synthesised id represents anyway.
 *
 * Lives here, in the module that owns [ProductUnit], because the rule is a
 * property of that id and not of any one caller. It was previously private to
 * the voucher sync mapper, and a second caller that reimplemented the check as
 * `isNotBlank()` shipped a 400 on every base-unit line.
 */
fun String.isServerUnitId(): Boolean =
    length == 36 &&
        this[8] == '-' && this[13] == '-' && this[18] == '-' && this[23] == '-' &&
        withIndex().all { (i, c) ->
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                c == '-'
            } else {
                c.isDigit() || c in 'a'..'f' || c in 'A'..'F'
            }
        }

/** The id to send to the API, or null when this unit has no server row. */
val ProductUnit.serverUnitId: String? get() = id.takeIf { it.isServerUnitId() }

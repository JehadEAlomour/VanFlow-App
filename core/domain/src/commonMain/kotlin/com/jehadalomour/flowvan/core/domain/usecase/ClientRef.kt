package com.jehadalomour.flowvan.core.domain.usecase

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A globally-unique, stable id for a locally-created voucher. Used as the primary key AND the
 * sync `clientRef` (idempotency key). It must NOT be derived from the human voucher number:
 * that number comes from a per-install counter that resets to 1 on reinstall, so reusing it as
 * the clientRef makes a fresh sale collide with an old server voucher (the server dedupes by
 * clientRef and returns the stale row instead of creating the new one). A random UUID stays
 * unique across reinstalls and devices; the server assigns the authoritative voucher number.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun newVoucherClientRef(): String = Uuid.random().toString()

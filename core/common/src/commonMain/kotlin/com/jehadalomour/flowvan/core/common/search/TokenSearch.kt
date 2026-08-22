package com.jehadalomour.flowvan.core.common.search

/**
 * The one search strategy, matching the backend's applyTokenSearch.
 *
 * Splits the query into whitespace-separated tokens and matches only when EVERY
 * token appears — as a case-insensitive substring — somewhere across the given
 * fields. Word order does not matter, and a token may sit in a different field
 * than its neighbour.
 *
 *   "abu market"  →  every result contains both "abu" and "market"
 *
 * So "Abu Rayash Market" matches while "Abu Bakr Bakery" does not — more words
 * narrow the result, they do not widen it. Numbers ride the same path: "77" is
 * just another substring token and finds "1772".
 *
 * A blank query matches everything (an empty filter), so callers can pass the
 * raw box text without a special-case.
 */
fun matchesTokenSearch(query: String, vararg fields: String?): Boolean {
    val tokens = query.trim().lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true
    // One haystack across all fields, so a token in the name and another in the
    // code both count — same as the backend's OR-across-fields per token.
    val haystack = fields.filterNotNull().joinToString(" ").lowercase()
    return tokens.all { haystack.contains(it) }
}

private val WHITESPACE = Regex("\\s+")

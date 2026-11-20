package com.lensstock.app.domain

/**
 * The shop counts lens stock in pairs (2 pcs = 1 pair), not individual pieces. Quantities are
 * still stored and adjusted one piece at a time internally (a single lens scanned in/out is 1
 * piece = half a pair), but are always shown to the user in pair form, e.g. 5 pieces -> "2½".
 */
object PairFormat {
    fun format(pieces: Long): String {
        val whole = pieces / 2
        val half = pieces % 2 != 0L
        return when {
            whole == 0L && half -> "½"
            half -> "$whole½"
            else -> "$whole"
        }
    }
}

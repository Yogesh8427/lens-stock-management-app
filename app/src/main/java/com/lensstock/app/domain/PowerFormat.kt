package com.lensstock.app.domain

/** Canonical power formatting shared by the OCR parser and manual entry. */
object PowerFormat {
    /** "-1.25" stays "-1.25"; "1.25" becomes "+1.25"; "0.00" stays "0.00". Null if not a multiple of 0.25. */
    fun normalize(raw: String): String? {
        val value = raw.trim().toDoubleOrNull() ?: return null
        val steps = value / 0.25
        if (Math.abs(steps - Math.round(steps)) > 1e-6) return null
        if (value == 0.0) return "0.00"
        val sign = if (value > 0) "+" else "-"
        return "$sign${"%.2f".format(Math.abs(value))}"
    }

    /** Axis is a plain 0-180 degree integer — no sign, no decimal. */
    fun normalizeAxis(raw: String): Long? {
        val value = raw.trim().toLongOrNull() ?: return null
        return if (value in 0..180) value else null
    }
}

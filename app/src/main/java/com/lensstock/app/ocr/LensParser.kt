package com.lensstock.app.ocr

import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.domain.PowerFormat

data class ParsedLens(
    val type: String,
    val sph: String,
    val cyl: String,
    val axis: Long = 0,
    val add: String = "",
)

/**
 * Reads one packet's OCR text. Patterns are tuned to the suppliers whose packets were
 * photographed during shop setup — add a new pattern here when a new supplier's print differs.
 */
object LensParser {
    // A bare number, allowing 0-2 digits before the point and 1-2 after (suppliers print "-2.5"
    // as often as "-2.50"), and either "." or "," as the decimal separator (OCR/locale noise).
    private const val NUMBER = """([+-]?\d{0,2}[.,]\d{1,2})"""
    // Label-to-value separator: an optional trailing period (e.g. "SPH."), then a colon or an
    // equals sign, or just whitespace. Deliberately NOT a dash — that would swallow a "-" that
    // actually belongs to the number as its negative sign (e.g. "SPH-2.00").
    private const val SEP = """\.?\s*[:=]?\s*"""

    // SPH is also printed as "PWR" (power) or "SPHERE" on single-vision-only packets. \b around
    // every label so a short one (e.g. "ADD") never matches as a substring of another word.
    private val sphRegex = Regex("""\b(?:SPH|PWR|SPHERE)$SEP$NUMBER""")
    private val cylRegex = Regex("""\b(?:CYL|CYLINDER)$SEP$NUMBER""")
    // ADD is the near-vision addition on bifocal/progressive packets, e.g. "ADD: +2.50".
    private val addRegex = Regex("""\b(?:ADD|ADDITION)$SEP$NUMBER""")
    // AXIS is a plain 0-180 degree integer, e.g. "AXIS: 180" — never has a sign or decimal.
    private val axisRegex = Regex("""\bAXIS$SEP(\d{1,3})""")
    private val blueCutRegex = Regex("""\b(?:BLUE|BLU|BC)\b""")

    // Fallback for suppliers that print power without "SPH/CYL" labels, e.g. "-1.25 / -0.50".
    private val unlabeledPairRegex = Regex("""$NUMBER\s*/\s*$NUMBER""")

    fun parse(text: String): ParsedLens? {
        // Normalize comma decimals ("-2,50") to periods before any pattern runs.
        val upper = text.uppercase().replace(Regex("""(\d),(\d{1,2})(?!\d)"""), "$1.$2")

        val sphMatch = sphRegex.find(upper)?.groupValues?.get(1)
        val (sphRaw, cylRaw) = if (sphMatch != null) {
            sphMatch to (cylRegex.find(upper)?.groupValues?.get(1) ?: "0.00")
        } else {
            val fallback = unlabeledPairRegex.find(upper) ?: return null
            fallback.groupValues[1] to fallback.groupValues[2]
        }

        val sph = PowerFormat.normalize(sphRaw) ?: return null
        val cyl = PowerFormat.normalize(cylRaw) ?: return null
        val type = if (blueCutRegex.containsMatchIn(upper)) LensTypes.BLUE_CUT else LensTypes.NORMAL
        val axis = axisRegex.find(upper)?.groupValues?.get(1)?.let { PowerFormat.normalizeAxis(it) } ?: 0
        val add = addRegex.find(upper)?.groupValues?.get(1)?.let { PowerFormat.normalize(it) } ?: ""
        return ParsedLens(type, sph, cyl, axis, add)
    }
}

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
    // Separator is ":" only — a "-" here would also swallow a negative sign belonging to the number.
    private val sphRegex = Regex("""SPH\s*:?\s*([+-]?\d{1,2}\.\d{2})""")
    private val cylRegex = Regex("""CYL\s*:?\s*([+-]?\d{1,2}\.\d{2})""")
    // ADD is the near-vision addition on bifocal/progressive packets, e.g. "ADD: +2.50".
    private val addRegex = Regex("""ADD\s*:?\s*([+-]?\d{1,2}\.\d{2})""")
    // AXIS is a plain 0-180 degree integer, e.g. "AXIS: 180" — never has a sign or decimal.
    private val axisRegex = Regex("""AXIS\s*:?\s*(\d{1,3})""")
    private val blueCutRegex = Regex("BLUE|BC")

    // Fallback for suppliers that print power without "SPH/CYL" labels, e.g. "-1.25 / -0.50".
    private val unlabeledPairRegex = Regex("""([+-]?\d{1,2}\.\d{2})\s*/\s*([+-]?\d{1,2}\.\d{2})""")

    fun parse(text: String): ParsedLens? {
        val upper = text.uppercase()

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

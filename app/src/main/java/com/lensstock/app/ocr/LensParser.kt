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
    // A decimal-formatted number: 0-2 digits, a decimal point/comma, then 1-2 digits.
    private const val DECIMAL = """[+-]?\d{0,2}[.,]\d{1,2}"""
    // Some suppliers print the power as a bare integer with no decimal point at all — see
    // [expandImpliedDecimal] for how that gets turned back into a real power.
    private const val BARE_INT = """[+-]?\d{1,4}"""
    private const val VALUE = """(?:$DECIMAL|$BARE_INT)"""

    // Label-to-value separator: an optional trailing period (e.g. "SPH."), then a colon or an
    // equals sign, or just whitespace. Deliberately NOT a dash — that would swallow a "-" that
    // actually belongs to the number as its negative sign (e.g. "SPH-2.00").
    private const val SEP = """\.?\s*[:=]?\s*"""

    // Some packets print one shared "SPH/CYL:" label followed by both values in order, e.g.
    // "SPH/CYL: 100  -075" (itself often in the no-decimal form below). Others print "SPH:" and
    // "CYL:" as two separate labels stacked together on one header row, with both values printed
    // below rather than next to either label — ML Kit then often reads the whole header row
    // before either value, scrambling the normal "label directly followed by its own value"
    // layout. The gap between SPH and CYL is required to contain no digit, which is what tells
    // these two-separate-labels-no-value-between-them apart from the ordinary "SPH: -125 CYL:
    // -075" layout (where SPH's own value sits in that gap, and the direct sphRegex/cylRegex
    // below already handles it). Either way, this only locates the label pair; [parse] then takes
    // the next couple of number-like tokens that follow it, wherever they land.
    private val sphCylLabelRegex = Regex("""\bSPH\b[^\d]{0,30}?\bCYL\b""")
    private val anyValueRegex = Regex(VALUE)
    // How far past the "SPH/CYL" label to keep looking for its two values — far enough to skip
    // a short line or two of unrelated print, not so far it picks up a date or price instead.
    private const val COMBO_LABEL_WINDOW = 100

    // SPH is also printed as "PWR" (power) or "SPHERE" on single-vision-only packets. \b around
    // every label so a short one (e.g. "ADD") never matches as a substring of another word.
    private val sphRegex = Regex("""\b(?:SPH|PWR|SPHERE)$SEP($VALUE)""")
    private val cylRegex = Regex("""\b(?:CYL|CYLINDER)$SEP($VALUE)""")
    // ADD is the near-vision addition on bifocal/progressive packets, e.g. "ADD: +2.50".
    private val addRegex = Regex("""\b(?:ADD|ADDITION)$SEP($VALUE)""")
    // AXIS is a plain 0-180 degree integer, e.g. "AXIS: 180" — never has a sign or decimal.
    private val axisRegex = Regex("""\bAXIS$SEP(\d{1,3})""")
    private val blueCutRegex = Regex("""\b(?:BLUE|BLU|BC)\b""")

    // Fallback for suppliers that print power with no label at all, either side of a "/" or just
    // separated by space, e.g. "-1.25 / -0.50" or "+3.00    -1.00". Decimal-only (not [BARE_INT])
    // since nothing here anchors it to a label — an unlabeled bare-integer pair is too easy to
    // confuse with an unrelated pair of numbers elsewhere on the packet.
    private val unlabeledPairRegex = Regex("""($DECIMAL)(?:\s*/\s*|\s+)($DECIMAL)""")

    fun parse(text: String): ParsedLens? {
        // Normalize comma decimals ("-2,50") to periods, and an OCR-misread "O" (a very common
        // stand-in for a handwritten or printed "0", whether alone or butted up against real
        // digits, e.g. faint dot-matrix print read as "O.0O" instead of "0.00") to an actual
        // digit, before any pattern runs.
        val upper = text.uppercase()
            .replace(Regex("""(\d),(\d{1,2})(?!\d)"""), "$1.$2")
            .replace(Regex("""O(?=[\d.])|(?<=[\d.])O"""), "0")
            .replace(Regex("""\bO\b"""), "0")

        val comboValues = sphCylLabelRegex.find(upper)?.let { label ->
            val windowEnd = (label.range.last + 1 + COMBO_LABEL_WINDOW).coerceAtMost(upper.length)
            val window = upper.substring(label.range.last + 1, windowEnd)
            anyValueRegex.findAll(window).filterNot { isLensIndexSpec(window, it) }.take(2).map { it.value }.toList()
        }
        val (sphRaw, cylRaw) = when {
            !comboValues.isNullOrEmpty() -> comboValues[0] to (comboValues.getOrNull(1) ?: "0.00")
            else -> {
                val sphMatch = sphRegex.find(upper)?.groupValues?.get(1)
                if (sphMatch != null) {
                    sphMatch to (cylRegex.find(upper)?.groupValues?.get(1) ?: "0.00")
                } else {
                    val fallback = unlabeledPairRegex.find(upper) ?: return null
                    fallback.groupValues[1] to fallback.groupValues[2]
                }
            }
        }

        val sph = PowerFormat.normalize(expandImpliedDecimal(sphRaw)) ?: return null
        val cyl = PowerFormat.normalize(expandImpliedDecimal(cylRaw)) ?: return null
        val type = if (blueCutRegex.containsMatchIn(upper)) LensTypes.BLUE_CUT else LensTypes.NORMAL
        val axis = axisRegex.find(upper)?.groupValues?.get(1)?.let { PowerFormat.normalizeAxis(it) } ?: 0
        val add = addRegex.find(upper)?.groupValues?.get(1)
            ?.let { PowerFormat.normalize(expandImpliedDecimal(it)) } ?: ""
        return ParsedLens(type, sph, cyl, axis, add)
    }

    // "1.56 Index"/"1.61 Index" (the lens's refractive index) is printed on almost every packet
    // and matches the generic numeric VALUE pattern just as well as a real power would — skip it.
    private fun isLensIndexSpec(window: String, match: MatchResult): Boolean =
        window.substring(match.range.last + 1).trimStart().startsWith("INDEX")

    /**
     * Many suppliers print the power as a bare integer with no decimal point at all — the last
     * two digits are always the hundredths place (e.g. "125" -> "1.25", "075" -> "0.75",
     * "50" -> "0.50") since no real lens power needs finer precision than that. Left untouched
     * if a decimal point/comma is already present.
     */
    private fun expandImpliedDecimal(raw: String): String {
        if ('.' in raw || ',' in raw) return raw
        val sign = if (raw.startsWith("-")) "-" else ""
        val digits = raw.trimStart('+', '-').padStart(3, '0')
        val whole = digits.dropLast(2).trimStart('0').ifEmpty { "0" }
        val frac = digits.takeLast(2)
        return "$sign$whole.$frac"
    }
}

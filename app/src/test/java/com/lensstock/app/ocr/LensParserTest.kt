package com.lensstock.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LensParserTest {
    @Test
    fun readsBlueCut() {
        val text = "BLUE CUT LENS\nSPH -1.25 CYL -0.50\nINDEX 1.56"
        assertEquals(ParsedLens("BLUE_CUT", "-1.25", "-0.50"), LensParser.parse(text))
    }

    @Test
    fun defaultsToNormalWhenNotMarkedBlueCut() {
        val text = "SPH -0.50 CYL 0.00"
        assertEquals(ParsedLens("NORMAL", "-0.50", "0.00"), LensParser.parse(text))
    }

    @Test
    fun defaultsCylToZeroWhenMissing() {
        val text = "BC SPH -2.00"
        assertEquals(ParsedLens("BLUE_CUT", "-2.00", "0.00"), LensParser.parse(text))
    }

    @Test
    fun addsPlusSignToPositiveSph() {
        val text = "NORMAL SPH 1.25 CYL -0.25"
        assertEquals(ParsedLens("NORMAL", "+1.25", "-0.25"), LensParser.parse(text))
    }

    @Test
    fun fallsBackToUnlabeledSlashFormat() {
        val text = "BLUE CUT\n-1.25 / -0.50"
        assertEquals(ParsedLens("BLUE_CUT", "-1.25", "-0.50"), LensParser.parse(text))
    }

    @Test
    fun rejectsStepsThatAreNotMultiplesOf0point25() {
        val text = "SPH -1.30 CYL -0.50"
        assertNull(LensParser.parse(text))
    }

    @Test
    fun returnsNullWhenNoPowerFound() {
        assertNull(LensParser.parse("INDEX 1.56 MADE IN INDIA"))
    }

    @Test
    fun readsBifocalWithAxisAndAdd() {
        val text = "SPH: -1.00 CYL: -0.75\nADD: +2.50 AXIS: 180\nCooL KT BIFOCAL"
        assertEquals(ParsedLens("NORMAL", "-1.00", "-0.75", axis = 180, add = "+2.50"), LensParser.parse(text))
    }

    @Test
    fun axisAndAddDefaultWhenAbsent() {
        val text = "SPH -1.25 CYL -0.50"
        assertEquals(ParsedLens("NORMAL", "-1.25", "-0.50", axis = 0, add = ""), LensParser.parse(text))
    }

    @Test
    fun fallsBackToUnlabeledSpaceSeparatedFormat() {
        // No "SPH"/"CYL" labels at all, no slash — just two numbers side by side, e.g. the
        // "Cheers" packet format: "+3.00    -1.00" followed by the refractive index on its own line.
        val text = "+3.00    -1.00\n1.56  HC"
        assertEquals(ParsedLens("NORMAL", "+3.00", "-1.00"), LensParser.parse(text))
    }

    @Test
    fun readsImpliedDecimalWithoutDecimalPoint() {
        // The "Float" packet format: "SPH : -125 CYL : -075" — the supplier's printer omits the
        // decimal point; the last two digits are always the hundredths place.
        val text = "SPH : -125 CYL : -075 ADD : AXIS :"
        assertEquals(ParsedLens("NORMAL", "-1.25", "-0.75"), LensParser.parse(text))
    }

    @Test
    fun readsCombinedSphCylLabelWithImpliedDecimal() {
        // The "Sharvin" packet format: one shared "SPH/CYL:" label followed by both bare values.
        val text = "SPH/CYL: 100   -075"
        assertEquals(ParsedLens("NORMAL", "+1.00", "-0.75"), LensParser.parse(text))
    }

    @Test
    fun treatsLoneLetterOAsZero() {
        // Common OCR misread of a handwritten/printed "0", e.g. the "Dinzania" packet's "CYL: O".
        val text = "SPH: +2.50 CYL: O"
        assertEquals(ParsedLens("NORMAL", "+2.50", "0.00"), LensParser.parse(text))
    }

    @Test
    fun readsCombinedSphCylLabelWhenValuesAreNotAdjacent() {
        // The real "Sharvin" packet's OCR text: an eye chart and "Spectacle Lenses" print get
        // read in between the "SPH/CYL:" label and its two values.
        val text = "SPH/CYL:\nacle Lenses\nE\n100 -075\nSharvin"
        assertEquals(ParsedLens("NORMAL", "+1.00", "-0.75"), LensParser.parse(text))
    }

    @Test
    fun readsSeparateSphAndCylLabelsWhenValuesComeMuchLater() {
        // The real "Dinzania" packet's OCR text: "SPH:" and "CYL:" are two separate labels
        // stacked on one header row with no values between them (both values are printed below
        // the header, read out much later by ML Kit), and faint dot-matrix "0.00"/"0.50" come
        // back misread as "O.0O"/"O.5O". The lens index ("1.56 Index") sitting in between must
        // not be mistaken for the CYL value.
        val text = "SPH:\nCYL:\nO.0O\nCR\nHard Coat\n1.56 Index\nDinzania\nSIZE:\nO.5O"
        assertEquals(ParsedLens("NORMAL", "0.00", "+0.50"), LensParser.parse(text))
    }

    @Test
    fun stillReadsSeparateLabelsWhenSphValueSitsBetweenThem() {
        // Guards against the above fix over-firing: when SPH's own value really does sit between
        // the two labels (the ordinary non-scrambled layout), it must still be read as SPH, not
        // skipped in favor of treating CYL's value as both.
        val text = "SPH : -125 CYL : -075 ADD : AXIS :"
        assertEquals(ParsedLens("NORMAL", "-1.25", "-0.75"), LensParser.parse(text))
    }
}

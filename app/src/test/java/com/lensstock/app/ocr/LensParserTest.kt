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
}

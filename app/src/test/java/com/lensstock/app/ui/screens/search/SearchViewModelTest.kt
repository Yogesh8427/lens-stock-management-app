package com.lensstock.app.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchViewModelTest {
    @Test
    fun `keeps negative values as-is`() {
        assertEquals("-1.25", SearchViewModel.normalizePower("-1.25"))
    }

    @Test
    fun `adds a plus sign to positive values`() {
        assertEquals("+1.25", SearchViewModel.normalizePower("1.25"))
    }

    @Test
    fun `zero has no sign`() {
        assertEquals("0.00", SearchViewModel.normalizePower("0.00"))
        assertEquals("0.00", SearchViewModel.normalizePower("0"))
    }

    @Test
    fun `rejects steps that are not multiples of 0point25`() {
        assertNull(SearchViewModel.normalizePower("-1.30"))
    }

    @Test
    fun `rejects non-numeric input`() {
        assertNull(SearchViewModel.normalizePower("abc"))
        assertNull(SearchViewModel.normalizePower(""))
    }
}

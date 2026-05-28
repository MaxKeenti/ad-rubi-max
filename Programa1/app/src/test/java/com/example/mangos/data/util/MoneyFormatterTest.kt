package com.example.mangos.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun `formats 123456 centavos as canonical MXN string`() {
        assertEquals("$1,234.56 MXN", 123456L.centavosToMxnString())
    }

    @Test
    fun `formats null centavos as em dash`() {
        val value: Long? = null
        assertEquals("—", value.centavosToMxnString())
    }

    @Test
    fun `formats zero centavos`() {
        assertEquals("$0.00 MXN", 0L.centavosToMxnString())
    }

    @Test
    fun `formats large amount with grouping`() {
        // 100 tons * $999,999.99/ton ≈ 10^10 centavos
        assertEquals("$9,999,999.99 MXN", 999999999L.centavosToMxnString())
    }

    @Test
    fun `parses plain decimal string`() {
        assertEquals(123456L, "1234.56".parseMxnToCentavos())
    }

    @Test
    fun `parses grouped decimal string`() {
        assertEquals(123456L, "1,234.56".parseMxnToCentavos())
    }

    @Test
    fun `parses string with leading dollar sign`() {
        assertEquals(123456L, "$1,234.56".parseMxnToCentavos())
    }

    @Test
    fun `parses integer string as whole pesos`() {
        assertEquals(100000L, "1000".parseMxnToCentavos())
    }

    @Test
    fun `empty string returns null`() {
        assertNull("".parseMxnToCentavos())
    }

    @Test
    fun `blank string returns null`() {
        assertNull("   ".parseMxnToCentavos())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-numeric string throws`() {
        "abc".parseMxnToCentavos()
    }
}

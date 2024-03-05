package com.seiko.tl.gradle.plugin.generator

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun testGenerateClassName01() {
        val prefix = "TL_"
        val name = "help.asd"
        val result = generateClassName(prefix, name)
        assertEquals("TL_help_Asd", result)
    }

    @Test
    fun testGenerateClassName012() {
        val prefix = "TL_"
        val name = "asd"
        val result = generateClassName(prefix, name)
        assertEquals("TL_Asd", result)
    }
}

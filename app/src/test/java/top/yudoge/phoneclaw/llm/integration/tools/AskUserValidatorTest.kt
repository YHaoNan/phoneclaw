package top.yudoge.phoneclaw.llm.integration.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AskUserValidatorTest {

    @Test
    fun `validate should fail when question is blank`() {
        val error = AskUserValidator.validate("  ", listOf("A"))
        assertEquals("Question must not be empty", error)
    }

    @Test
    fun `validate should fail when options exceed limit`() {
        val error = AskUserValidator.validate("pick one", listOf("1", "2", "3", "4", "5", "6"))
        assertEquals("At most 5 answer options are allowed", error)
    }

    @Test
    fun `validate should fail when option is blank`() {
        val error = AskUserValidator.validate("pick one", listOf("A", " "))
        assertEquals("Answer options must not be empty", error)
    }

    @Test
    fun `validate should pass for normal request`() {
        val error = AskUserValidator.validate("pick one", listOf("A", "B"))
        assertNull(error)
    }
}


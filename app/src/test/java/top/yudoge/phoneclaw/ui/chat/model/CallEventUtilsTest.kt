package top.yudoge.phoneclaw.ui.chat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallEventUtilsTest {

    @Test
    fun `extractSkillName should parse skillName field`() {
        val name = CallEventUtils.extractSkillName("""{"skillName":"phone_emulation"}""")
        assertEquals("phone_emulation", name)
    }

    @Test
    fun `extractSkillName should fallback to first value`() {
        val name = CallEventUtils.extractSkillName("""{"arg0":"quick-start"}""")
        assertEquals("quick-start", name)
    }

    @Test
    fun `displayArguments should provide placeholder for empty values`() {
        assertEquals(CallEventUtils.EMPTY_ARGUMENTS_PLACEHOLDER, CallEventUtils.displayArguments(" "))
    }

    @Test
    fun `displayArguments should truncate long text safely`() {
        val text = "a".repeat(1200)
        val display = CallEventUtils.displayArguments(text, maxChars = 100)
        assertTrue(display.length < text.length)
        assertTrue(display.endsWith("...(已截断)"))
    }
}

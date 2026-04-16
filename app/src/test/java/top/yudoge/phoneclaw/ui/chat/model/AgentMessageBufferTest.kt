package top.yudoge.phoneclaw.ui.chat.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentMessageBufferTest {

    @Test
    fun `complete should not duplicate when deltas already appended`() {
        val buffer = AgentMessageBuffer()
        buffer.start("A")
        buffer.appendDelta("hello")
        buffer.appendDelta(" world")

        val complete = buffer.complete("hello world")

        assertEquals("hello world", complete)
        assertEquals("hello world", buffer.clearAndGetFinalContent())
    }

    @Test
    fun `new message should start from empty buffer`() {
        val buffer = AgentMessageBuffer()
        buffer.start("A")
        buffer.appendDelta("message A")
        buffer.clearAndGetFinalContent()

        buffer.start("B")
        buffer.appendDelta("message B")
        val complete = buffer.complete("message B")

        assertEquals("message B", complete)
        assertEquals("message B", buffer.clearAndGetFinalContent())
    }

    @Test
    fun `complete should use full text when no delta received`() {
        val buffer = AgentMessageBuffer()
        buffer.start("A")

        val complete = buffer.complete("full response")

        assertEquals("full response", complete)
    }
}

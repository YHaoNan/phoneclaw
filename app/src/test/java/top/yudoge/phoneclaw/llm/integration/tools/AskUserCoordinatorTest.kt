package top.yudoge.phoneclaw.llm.integration.tools

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yudoge.phoneclaw.llm.domain.objects.AskUserAnswerSource
import top.yudoge.phoneclaw.llm.domain.objects.AskUserRequest
import top.yudoge.phoneclaw.llm.domain.objects.AskUserResponse

class AskUserCoordinatorTest {

    @After
    fun tearDown() {
        AskUserCoordinator.setListener(null)
    }

    @Test
    fun `requestUserAnswer should return confirmed answer`() {
        AskUserCoordinator.setListener(object : AskUserCoordinator.Listener {
            override fun onAskUserRequested(request: AskUserRequest) {
                AskUserCoordinator.submitResponse(
                    AskUserResponse(
                        requestId = request.requestId,
                        confirmed = true,
                        answer = "A",
                        source = AskUserAnswerSource.OPTION
                    )
                )
            }
        })

        val response = AskUserCoordinator.requestUserAnswer(
            AskUserRequest(
                requestId = "req-1",
                question = "pick",
                answers = listOf("A", "B"),
                timeoutMs = 1000
            )
        )

        assertTrue(response.confirmed)
        assertEquals("A", response.answer)
        assertEquals(AskUserAnswerSource.OPTION, response.source)
    }

    @Test
    fun `requestUserAnswer should timeout when no response`() {
        AskUserCoordinator.setListener(object : AskUserCoordinator.Listener {
            override fun onAskUserRequested(request: AskUserRequest) = Unit
        })

        val response = AskUserCoordinator.requestUserAnswer(
            AskUserRequest(
                requestId = "req-timeout",
                question = "pick",
                answers = listOf("A"),
                timeoutMs = 10
            )
        )

        assertFalse(response.confirmed)
        assertEquals(AskUserAnswerSource.TIMEOUT, response.source)
    }
}

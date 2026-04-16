package top.yudoge.phoneclaw.llm.integration.tools

import android.util.Log
import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.domain.objects.AskUserAnswerSource
import top.yudoge.phoneclaw.llm.domain.objects.AskUserRequest
import java.util.UUID

class AskUserTool {
    companion object {
        private const val TAG = "AskUserTool"
    }

    @Tool(
        name = "askUser",
        value = ["Ask user a multiple-choice question. Supports 1-5 answers and an additional 'Other' text input that requires explicit confirmation."]
    )
    fun askUser(
        @P("Question to ask user")
        question: String,
        @P("Answer options as an array of strings, between 1 and 5 items")
        answers: List<String>
    ): String {
        val validationError = AskUserValidator.validate(question, answers)
        if (validationError != null) {
            Log.w(TAG, "askUser: invalid request, error=$validationError")
            return buildResultJson(
                confirmed = false,
                answer = null,
                source = AskUserAnswerSource.CANCELLED,
                error = validationError
            )
        }

        val request = AskUserRequest(
            requestId = UUID.randomUUID().toString(),
            question = question.trim(),
            answers = answers.map { it.trim() }
        )
        Log.i(TAG, "askUser: request start, requestId=${request.requestId}")
        val response = AskUserCoordinator.requestUserAnswer(request)
        Log.i(TAG, "askUser: request finished, requestId=${request.requestId}, confirmed=${response.confirmed}")

        return buildResultJson(
            confirmed = response.confirmed,
            answer = response.answer,
            source = response.source,
            error = response.error
        )
    }

    private fun buildResultJson(
        confirmed: Boolean,
        answer: String?,
        source: AskUserAnswerSource,
        error: String?
    ): String {
        val json = JSONObject()
            .put("confirmed", confirmed)
            .put("source", source.name.lowercase())
            .put("answer", answer ?: JSONObject.NULL)
            .put("error", error ?: JSONObject.NULL)
        return json.toString()
    }
}


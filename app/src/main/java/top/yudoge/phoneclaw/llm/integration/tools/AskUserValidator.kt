package top.yudoge.phoneclaw.llm.integration.tools

object AskUserValidator {
    private const val MAX_ANSWERS = 5

    fun validate(question: String, answers: List<String>): String? {
        if (question.isBlank()) {
            return "Question must not be empty"
        }
        if (answers.isEmpty()) {
            return "At least one answer option is required"
        }
        if (answers.size > MAX_ANSWERS) {
            return "At most $MAX_ANSWERS answer options are allowed"
        }
        if (answers.any { it.isBlank() }) {
            return "Answer options must not be empty"
        }
        return null
    }
}


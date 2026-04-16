package top.yudoge.phoneclaw.ui.chat.model

object CallEventUtils {
    const val EMPTY_ARGUMENTS_PLACEHOLDER = "(无参数)"

    fun normalizeArguments(raw: String?): String {
        return raw?.trim().orEmpty()
    }

    fun displayArguments(raw: String?, maxChars: Int = 800): String {
        val normalized = normalizeArguments(raw)
        if (normalized.isEmpty()) {
            return EMPTY_ARGUMENTS_PLACEHOLDER
        }
        if (normalized.length <= maxChars) {
            return normalized
        }
        return normalized.take(maxChars) + "...(已截断)"
    }

    fun extractSkillName(arguments: String?): String {
        val normalized = normalizeArguments(arguments)
        if (normalized.isEmpty()) {
            return "useSkill"
        }
        if (!normalized.startsWith("{")) {
            return normalized
        }
        val skillNameMatch = "\"skillName\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(normalized)
        if (skillNameMatch != null) {
            return skillNameMatch.groupValues[1]
        }
        val nameMatch = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(normalized)
        if (nameMatch != null) {
            return nameMatch.groupValues[1]
        }
        val firstValue = ":\\s*\"([^\"]+)\"".toRegex().find(normalized)
        return firstValue?.groupValues?.get(1) ?: normalized
    }
}

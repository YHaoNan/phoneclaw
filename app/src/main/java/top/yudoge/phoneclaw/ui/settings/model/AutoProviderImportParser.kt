package top.yudoge.phoneclaw.ui.settings.model

import java.net.URI
import java.util.Locale

data class AutoProviderImportResult(
    val providerName: String,
    val baseUrl: String?,
    val apiKey: String?,
    val models: List<String>,
    val warnings: List<String>
) {
    fun toPreviewText(): String {
        val lines = mutableListOf<String>()
        lines += "providerName: ${providerName.ifBlank { "(未识别)" }}"
        lines += "baseUrl: ${baseUrl ?: "(未识别)"}"
        lines += "apiKey: ${apiKey ?: "(未识别)"}"
        lines += "models(${models.size}): ${if (models.isEmpty()) "(未识别)" else models.joinToString(", ")}"
        if (warnings.isNotEmpty()) {
            lines += "warnings: ${warnings.joinToString("; ")}"
        }
        return lines.joinToString("\n")
    }
}

object AutoProviderImportParser {
    private val urlRegex = Regex("""https?://[^\s'"]+""", RegexOption.IGNORE_CASE)
    private val keyValueRegex = Regex("""(?im)^\s*(base[_\-\s]?url|url|endpoint|api[_\-\s]?base)\s*[:=]\s*['"]?([^\s'"]+)['"]?\s*$""")
    private val apiKeyValueRegex = Regex("""(?im)^\s*(api[_\-\s]?key|token|access[_\-\s]?token)\s*[:=]\s*['"]?([^\s'"]+)['"]?\s*$""")
    private val modelValueRegex = Regex("""(?im)^\s*(model|models|model[_\-\s]?id)\s*[:=]\s*['"]?(.+?)['"]?\s*$""")
    private val baseUrlAssignRegex = Regex("""(?i)\bbase_url\s*=\s*['"]([^'"]+)['"]""")
    private val apiKeyAssignRegex = Regex("""(?i)\bapi_key\s*=\s*['"]([^'"]+)['"]""")
    private val modelAssignRegex = Regex("""(?i)\bmodel\s*=\s*['"]([^'"]+)['"]""")
    private val likelyApiKeyRegex = Regex("""\b(sk-[A-Za-z0-9_\-]{12,}|ms-[A-Za-z0-9_\-]{12,}|rk-[A-Za-z0-9_\-]{12,}|[A-Za-z0-9_\-]{24,})\b""")
    private val modelTokenRegex = Regex("""\b([A-Za-z0-9][A-Za-z0-9._\-]*/[A-Za-z0-9][A-Za-z0-9._\-]{1,}|gpt-[A-Za-z0-9._\-]+|qwen[0-9A-Za-z._\-/\-]*|claude-[A-Za-z0-9._\-]+|gemini-[A-Za-z0-9._\-]+)\b""")

    fun parse(raw: String): AutoProviderImportResult {
        val text = raw.trim()
        val warnings = mutableListOf<String>()

        val baseUrl = extractBaseUrl(text)
        val apiKey = extractApiKey(text)
        val models = extractModels(text)
        val providerName = deriveProviderName(baseUrl)

        if (baseUrl == null) warnings += "未识别到 baseUrl"
        if (apiKey == null) warnings += "未识别到 apiKey"
        if (models.isEmpty()) warnings += "未识别到 model"

        return AutoProviderImportResult(
            providerName = providerName,
            baseUrl = baseUrl,
            apiKey = apiKey,
            models = models,
            warnings = warnings
        )
    }

    private fun extractBaseUrl(text: String): String? {
        keyValueRegex.find(text)?.groupValues?.getOrNull(2)?.let { return cleanUrl(it) }
        baseUrlAssignRegex.find(text)?.groupValues?.getOrNull(1)?.let { return cleanUrl(it) }

        val candidates = urlRegex.findAll(text)
            .map { cleanUrl(it.value) }
            .toList()
        if (candidates.isEmpty()) return null

        return candidates.firstOrNull { it.contains("/v1", ignoreCase = true) }
            ?: candidates.firstOrNull { it.contains("api", ignoreCase = true) }
            ?: candidates.first()
    }

    private fun extractApiKey(text: String): String? {
        apiKeyValueRegex.find(text)?.groupValues?.getOrNull(2)?.let { return cleanToken(it) }
        apiKeyAssignRegex.find(text)?.groupValues?.getOrNull(1)?.let { return cleanToken(it) }

        val matched = likelyApiKeyRegex.findAll(text)
            .map { cleanToken(it.value) }
            .firstOrNull { token -> token.length >= 16 && !token.startsWith("http", ignoreCase = true) }
        return matched
    }

    private fun extractModels(text: String): List<String> {
        val values = linkedSetOf<String>()

        modelValueRegex.findAll(text).forEach { m ->
            val rawValue = m.groupValues.getOrNull(2).orEmpty()
            splitModelCandidates(rawValue).forEach { values += it }
        }
        modelAssignRegex.findAll(text).forEach { m ->
            val rawValue = m.groupValues.getOrNull(1).orEmpty()
            splitModelCandidates(rawValue).forEach { values += it }
        }
        modelTokenRegex.findAll(text).forEach { m ->
            values += m.groupValues[1].trim()
        }

        return values
            .map { it.trim().trim('"', '\'', ',', ';') }
            .filter { it.isNotBlank() && it.length >= 2 }
            .toList()
    }

    private fun splitModelCandidates(raw: String): List<String> {
        return raw.split(",", ";", "\n", "\r")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun deriveProviderName(baseUrl: String?): String {
        if (baseUrl.isNullOrBlank()) return "Auto Imported Provider"
        return try {
            val host = URI(baseUrl).host ?: return "Auto Imported Provider"
            val parts = host.split(".").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val core = when {
                    parts[0].lowercase(Locale.ROOT) in setOf("api", "gateway", "openai", "chat", "inference")
                        && parts.size >= 3 -> parts[1]
                    else -> parts[parts.size - 2]
                }
                core.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            } else {
                host
            }
        } catch (_: Exception) {
            "Auto Imported Provider"
        }
    }

    private fun cleanUrl(value: String): String {
        return value.trim().trim('\'', '"', ',', ';')
    }

    private fun cleanToken(value: String): String {
        return value.trim().trim('\'', '"', ',', ';')
    }
}


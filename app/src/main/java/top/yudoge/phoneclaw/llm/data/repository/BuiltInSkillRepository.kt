package top.yudoge.phoneclaw.llm.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.data.entity.SkillEntityWithContent
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class BuiltInSkillRepository(
    private val context: Context
) : SkillRepository {
    companion object {
        private const val SKILLS_ROOT = "skills"
        private const val INDEX_FILE = "$SKILLS_ROOT/index.json"
        private const val SKILL_FILE = "SKILL.md"
        private const val LEGACY_SKILL_FILE = "skill.md"
    }

    private var cachedIndex: List<SkillEntity>? = null
    
    override fun getAll(): List<SkillEntity> {
        cachedIndex?.let { return it }

        val entities = loadFromIndex().ifEmpty { loadFromAssetsFallback() }
        cachedIndex = entities
        return entities
    }
    
    override fun getByName(name: String): SkillEntity? {
        return getAll().find { it.name == name }
    }
    
    override fun getContent(entity: SkillEntity): SkillEntityWithContent? {
        val skillDir = entity.skillDir?.trim('/') ?: return null
        val relativeDir = if (skillDir.startsWith("$SKILLS_ROOT/")) skillDir else "$SKILLS_ROOT/$skillDir"
        val contentPathCandidates = listOf(
            "$relativeDir/$SKILL_FILE",
            "$relativeDir/$LEGACY_SKILL_FILE",
            "$skillDir/$SKILL_FILE",
            "$skillDir/$LEGACY_SKILL_FILE"
        ).distinct()

        for (path in contentPathCandidates) {
            try {
                val content = context.assets.open(path).use {
                    InputStreamReader(it, StandardCharsets.UTF_8).readText()
                }
                return SkillEntityWithContent(entity, content)
            } catch (_: Exception) {
                // Ignore and try next candidate.
            }
        }
        return null
    }
    
    override fun insert(entity: SkillEntity, content: String): Boolean {
        throw UnsupportedOperationException("BuiltInSkillRepository is read-only")
    }
    
    override fun update(entity: SkillEntity, content: String?): Boolean {
        throw UnsupportedOperationException("BuiltInSkillRepository is read-only")
    }
    
    override fun delete(name: String): Boolean {
        throw UnsupportedOperationException("BuiltInSkillRepository is read-only")
    }

    private fun loadFromIndex(): List<SkillEntity> {
        return try {
            val content = context.assets.open(INDEX_FILE).use {
                InputStreamReader(it, StandardCharsets.UTF_8).readText()
            }
            val jsonArray = JSONArray(content)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    add(parseEntityFromJson(jsonArray.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadFromAssetsFallback(): List<SkillEntity> {
        val skillDirs = context.assets.list(SKILLS_ROOT) ?: return emptyList()
        return skillDirs.mapNotNull { dirName ->
            if (dirName == "index.json") {
                return@mapNotNull null
            }

            val skillAssetDir = "$SKILLS_ROOT/$dirName"
            val content = tryReadSkillContent(skillAssetDir) ?: return@mapNotNull null
            val frontMatter = parseFrontMatter(content)

            SkillEntity(
                name = frontMatter["name"]?.takeIf { it.isNotBlank() } ?: dirName,
                description = frontMatter["description"]?.takeIf { it.isNotBlank() }
                    ?: "Built-in skill: $dirName",
                argumentHint = frontMatter["argumentHint"]?.takeIf { it.isNotBlank() },
                disableModelInvocation = frontMatter["disableModelInvocation"]?.toBooleanStrictOrNull() ?: false,
                userInvocable = frontMatter["userInvocable"]?.toBooleanStrictOrNull() ?: true,
                allowedTools = frontMatter["allowedTools"]?.takeIf { it.isNotBlank() },
                context = frontMatter["context"]?.takeIf { it.isNotBlank() },
                skillDir = skillAssetDir,
                supportingFiles = frontMatter["supportingFiles"]?.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun tryReadSkillContent(skillAssetDir: String): String? {
        val candidates = listOf(
            "$skillAssetDir/$SKILL_FILE",
            "$skillAssetDir/$LEGACY_SKILL_FILE"
        )

        for (candidate in candidates) {
            try {
                return context.assets.open(candidate).use {
                    InputStreamReader(it, StandardCharsets.UTF_8).readText()
                }
            } catch (_: Exception) {
                // Ignore and try next candidate.
            }
        }
        return null
    }

    private fun parseFrontMatter(content: String): Map<String, String> {
        if (!content.startsWith("---")) return emptyMap()
        val lines = content.lines()
        if (lines.size < 3) return emptyMap()

        val frontMatter = mutableMapOf<String, String>()
        var i = 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.trim() == "---") break
            val sep = line.indexOf(':')
            if (sep > 0) {
                val key = line.substring(0, sep).trim()
                val value = line.substring(sep + 1).trim().trim('"')
                if (key.isNotEmpty()) {
                    frontMatter[key] = value
                }
            }
            i++
        }
        return frontMatter
    }
    
    private fun parseEntityFromJson(json: JSONObject): SkillEntity {
        val skillDir = json.optString("skillDir").takeIf { it.isNotEmpty() }
        return SkillEntity(
            name = json.getString("name"),
            description = json.optString("description"),
            argumentHint = json.optString("argumentHint").takeIf { it.isNotEmpty() },
            disableModelInvocation = json.optBoolean("disableModelInvocation", false),
            userInvocable = json.optBoolean("userInvocable", true),
            allowedTools = json.optString("allowedTools").takeIf { it.isNotEmpty() },
            context = json.optString("context").takeIf { it.isNotEmpty() },
            skillDir = skillDir?.let {
                if (it.startsWith("$SKILLS_ROOT/")) it else "$SKILLS_ROOT/$it"
            },
            supportingFiles = json.optString("supportingFiles").takeIf { it.isNotEmpty() }
        )
    }
}

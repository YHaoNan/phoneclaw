package top.yudoge.phoneclaw.llm.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.data.entity.SkillEntityWithContent
import java.io.InputStreamReader

class BuiltInSkillRepository(
    private val context: Context
) : SkillRepository {
    private var cachedIndex: List<SkillEntity>? = null
    
    override fun getAll(): List<SkillEntity> {
        cachedIndex?.let { return it }
        
        val entities = mutableListOf<SkillEntity>()
        try {
            val indexJson = context.assets.open("skills/index.json")
            val reader = InputStreamReader(indexJson)
            val content = reader.readText()
            reader.close()
            indexJson.close()
            
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                entities.add(parseEntityFromJson(json))
            }
            cachedIndex = entities
        } catch (e: Exception) {
        }
        return entities
    }
    
    override fun getByName(name: String): SkillEntity? {
        return getAll().find { it.name == name }
    }
    
    override fun getContent(entity: SkillEntity): SkillEntityWithContent? {
        val skillDir = entity.skillDir ?: return null
        return try {
            val content = readAssetText("$skillDir/skill.md")
                ?: readAssetText("$skillDir/SKILL.md")
                ?: return null
            SkillEntityWithContent(entity, content)
        } catch (e: Exception) {
            null
        }
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
    
    private fun parseEntityFromJson(json: JSONObject): SkillEntity {
        return SkillEntity(
            name = json.getString("name"),
            description = json.getString("description"),
            argumentHint = json.optString("argumentHint").takeIf { it.isNotEmpty() },
            disableModelInvocation = json.optBoolean("disableModelInvocation", false),
            userInvocable = json.optBoolean("userInvocable", true),
            allowedTools = json.optString("allowedTools").takeIf { it.isNotEmpty() },
            context = json.optString("context").takeIf { it.isNotEmpty() },
            skillDir = json.optString("skillDir").takeIf { it.isNotEmpty() },
            supportingFiles = json.optString("supportingFiles").takeIf { it.isNotEmpty() }
        )
    }

    private fun readAssetText(path: String): String? {
        return try {
            context.assets.open(path).use {
                InputStreamReader(it).readText()
            }
        } catch (e: Exception) {
            null
        }
    }
}

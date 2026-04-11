package top.yudoge.phoneclaw.llm.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.domain.objects.Skill
import top.yudoge.phoneclaw.llm.domain.objects.SkillSource
import top.yudoge.phoneclaw.llm.domain.objects.SkillWithContent
import java.io.InputStreamReader

class BuiltInSkillRepository(
    private val context: Context
) {
    private var cachedIndex: List<Skill>? = null
    
    fun getAll(): List<Skill> {
        cachedIndex?.let { return it }
        
        val skills = mutableListOf<Skill>()
        try {
            val indexJson = context.assets.open("skills/index.json")
            val reader = InputStreamReader(indexJson)
            val content = reader.readText()
            reader.close()
            indexJson.close()
            
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                skills.add(parseSkillFromJson(json, SkillSource.BUILT_IN))
            }
            cachedIndex = skills
        } catch (e: Exception) {
            // No built-in skills or error reading
        }
        return skills
    }
    
    fun getByName(name: String): Skill? {
        return getAll().find { it.name == name }
    }
    
    fun getContent(skill: Skill): SkillWithContent? {
        val skillDir = skill.skillDir ?: return null
        return try {
            val contentPath = "$skillDir/skill.md"
            val content = context.assets.open(contentPath).use { 
                InputStreamReader(it).readText() 
            }
            SkillWithContent(skill, content)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseSkillFromJson(json: JSONObject, source: SkillSource): Skill {
        return Skill(
            name = json.getString("name"),
            description = json.getString("description"),
            argumentHint = json.optString("argumentHint").takeIf { it.isNotEmpty() },
            disableModelInvocation = json.optBoolean("disableModelInvocation", false),
            userInvocable = json.optBoolean("userInvocable", true),
            allowedTools = json.optString("allowedTools").takeIf { it.isNotEmpty() }?.split(","),
            context = json.optString("context").takeIf { it.isNotEmpty() },
            source = source,
            skillDir = json.optString("skillDir").takeIf { it.isNotEmpty() }
        )
    }
}

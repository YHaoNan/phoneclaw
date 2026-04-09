package top.yudoge.phoneclaw.llm.skills

import android.content.Context
import android.util.Log
import java.io.File

class AssetSkillRepository(
    private val context: Context,
    private val assetsPath: String = "skills"
) : SkillRepository {

    companion object {
        private const val TAG = "AssetSkillRepository"
    }

    private var cachedSkills: List<Skill>? = null

    override fun loadSkills(): List<Skill> {
        cachedSkills?.let { return it }
        
        val skills = mutableListOf<Skill>()
        
        try {
            val skillDirs = context.assets.list(assetsPath)?.filter { it.isNotEmpty() } ?: emptyList()
            
            for (skillDirName in skillDirs) {
                try {
                    val skill = loadSkillFromAssets(skillDirName)
                    if (skill != null) {
                        skills.add(skill)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading skill $skillDirName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing skills in assets", e)
        }
        
        cachedSkills = skills
        return skills
    }

    private fun loadSkillFromAssets(skillDirName: String): Skill? {
        val skillMdPath = "$assetsPath/$skillDirName/SKILL.md"
        
        return try {
            val content = context.assets.open(skillMdPath).bufferedReader().readText()
            val (frontmatter, markdownContent) = parseFrontmatter(content)
            
            val name = frontmatter["name"] ?: skillDirName
            val description = frontmatter["description"] ?: ""
            val argumentHint = frontmatter["argumentHint"]
            val disableModelInvocation = frontmatter["disableModelInvocation"]?.toBooleanStrictOrNull() ?: false
            val userInvocable = frontmatter["userInvocable"]?.toBooleanStrictOrNull() ?: true
            val allowedTools = frontmatter["allowedTools"]?.split(",")?.map { it.trim() }?.takeIf { it.isNotEmpty() }
            val contextValue = frontmatter["context"]
            
            val supportingFiles = detectSupportingFiles(skillDirName)
            
            Skill(
                name = name,
                description = description,
                argumentHint = argumentHint,
                disableModelInvocation = disableModelInvocation,
                userInvocable = userInvocable,
                allowedTools = allowedTools,
                context = contextValue,
                content = markdownContent.trim(),
                skillDir = skillDirName,
                supportingFiles = supportingFiles,
                isBuiltIn = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading skill $skillDirName", e)
            null
        }
    }

    private fun detectSupportingFiles(skillDirName: String): List<String> {
        val files = mutableListOf<String>()
        try {
            val allFiles = context.assets.list("$assetsPath/$skillDirName")?.toList() ?: emptyList()
            for (fileName in allFiles) {
                if (fileName != "SKILL.md") {
                    files.add(fileName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing supporting files for $skillDirName", e)
        }
        return files
    }

    override fun getSkill(name: String): Skill? {
        return loadSkills().find { it.name == name }
    }

    override fun addSkill(skill: Skill) {
        throw UnsupportedOperationException("Cannot add skills to built-in repository")
    }

    override fun updateSkill(skill: Skill) {
        throw UnsupportedOperationException("Cannot update built-in skills")
    }

    override fun deleteSkill(skill: Skill) {
        throw UnsupportedOperationException("Cannot delete built-in skills")
    }

    override fun deleteSkillByName(name: String) {
        throw UnsupportedOperationException("Cannot delete built-in skills")
    }

    override fun skillExists(name: String): Boolean {
        return loadSkills().any { it.name == name }
    }

    private fun parseFrontmatter(content: String): Pair<Map<String, String>, String> {
        val frontmatterRegex = Regex("^---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n([\\s\\S]*)$")
        val matchResult = frontmatterRegex.find(content)

        return if (matchResult != null) {
            val frontmatterBlock = matchResult.groupValues[1]
            val markdownContent = matchResult.groupValues[2]

            val frontmatterMap = frontmatterBlock.split("\n")
                .filter { it.contains(":") }
                .associate { line ->
                    val colonIndex = line.indexOf(":")
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    key to value
                }

            frontmatterMap to markdownContent
        } else {
            emptyMap<String, String>() to content
        }
    }
}

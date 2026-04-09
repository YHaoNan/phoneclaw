package top.yudoge.phoneclaw.llm.skills

import java.io.File

class FileBasedSkillRepository(private val skillsDir: File) : SkillRepository {

    override fun loadSkills(): List<Skill> {
        if (!skillsDir.exists()) {
            skillsDir.mkdirs()
            return emptyList()
        }

        return skillsDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            val skillMdFile = File(dir, "SKILL.md")
            if (skillMdFile.exists()) {
                parseSkill(dir, skillMdFile)
            } else {
                null
            }
        } ?: emptyList()
    }

    override fun getSkill(name: String): Skill? {
        val skillDir = File(skillsDir, name)
        val skillMdFile = File(skillDir, "SKILL.md")
        return if (skillMdFile.exists()) {
            parseSkill(skillDir, skillMdFile)
        } else {
            null
        }
    }

    override fun addSkill(skill: Skill) {
        if (skillExists(skill.name)) {
            throw IllegalArgumentException("Skill '${skill.name}' already exists")
        }

        val skillDir = File(skillsDir, skill.name)
        skillDir.mkdirs()

        val skillMdContent = buildSkillMdContent(skill)
        File(skillDir, "SKILL.md").writeText(skillMdContent)
    }

    override fun updateSkill(skill: Skill) {
        if (!skillExists(skill.name)) {
            throw IllegalArgumentException("Skill '${skill.name}' does not exist")
        }

        val skillDir = File(skillsDir, skill.name)
        val skillMdContent = buildSkillMdContent(skill)
        File(skillDir, "SKILL.md").writeText(skillMdContent)
    }

    override fun deleteSkill(skill: Skill) {
        val skillDir = File(skillsDir, skill.skillDir ?: skill.name)
        if (!skillDir.exists()) {
            throw IllegalArgumentException("Skill directory does not exist: ${skillDir.path}")
        }
        skillDir.deleteRecursively()
    }

    override fun deleteSkillByName(name: String) {
        val skillDir = File(skillsDir, name)
        if (!skillDir.exists()) {
            throw IllegalArgumentException("Skill directory does not exist: ${skillDir.path}")
        }
        skillDir.deleteRecursively()
    }

    override fun skillExists(name: String): Boolean {
        val skillDir = File(skillsDir, name)
        return skillDir.isDirectory && File(skillDir, "SKILL.md").exists()
    }

    private fun parseSkill(skillDir: File, skillMdFile: File): Skill {
        val content = skillMdFile.readText()
        val (frontmatter, markdownContent) = parseFrontmatter(content)

        val name = frontmatter["name"] ?: skillDir.name
        val description = frontmatter["description"] ?: ""
        val argumentHint = frontmatter["argumentHint"]
        val disableModelInvocation = frontmatter["disableModelInvocation"]?.toBooleanStrictOrNull() ?: false
        val userInvocable = frontmatter["userInvocable"]?.toBooleanStrictOrNull() ?: true
        val allowedTools = frontmatter["allowedTools"]?.split(",")?.map { it.trim() }?.takeIf { it.isNotEmpty() }
        val context = frontmatter["context"]

        val supportingFiles = detectSupportingFiles(skillDir)
        val relativeSkillDir = skillDir.name

        return Skill(
            name = name,
            description = description,
            argumentHint = argumentHint,
            disableModelInvocation = disableModelInvocation,
            userInvocable = userInvocable,
            allowedTools = allowedTools,
            context = context,
            content = markdownContent.trim(),
            skillDir = relativeSkillDir,
            supportingFiles = supportingFiles,
            isBuiltIn = false
        )
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

    private fun detectSupportingFiles(skillDir: File): List<String> {
        val supportingFiles = mutableListOf<String>()
        skillDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name != "SKILL.md") {
                supportingFiles.add(file.relativeTo(skillDir).path.replace(File.separatorChar, '/'))
            }
        }
        return supportingFiles
    }

    private fun buildSkillMdContent(skill: Skill): String {
        val frontmatterLines = mutableListOf<String>()
        frontmatterLines.add("---")
        frontmatterLines.add("name: ${skill.name}")
        frontmatterLines.add("description: ${skill.description}")

        skill.argumentHint?.let { frontmatterLines.add("argumentHint: $it") }
        frontmatterLines.add("disableModelInvocation: ${skill.disableModelInvocation}")
        frontmatterLines.add("userInvocable: ${skill.userInvocable}")
        skill.allowedTools?.let { frontmatterLines.add("allowedTools: ${it.joinToString(", ")}") }
        skill.context?.let { frontmatterLines.add("context: $it") }

        frontmatterLines.add("---")
        frontmatterLines.add("")
        frontmatterLines.add(skill.content)

        return frontmatterLines.joinToString("\n")
    }
}

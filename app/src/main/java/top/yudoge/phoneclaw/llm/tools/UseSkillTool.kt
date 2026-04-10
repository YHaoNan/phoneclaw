package top.yudoge.phoneclaw.llm.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import top.yudoge.phoneclaw.llm.skills.SkillRepository

class UseSkillTool(
    private val skillRepository: SkillRepository
) {

    @Tool(
        name = "useSkill",
        value = ["Use a skill to get its content. Returns the skill documentation which provides additional capabilities."]
    )
    fun useSkill(
        @P("Name of the skill to use")
        skillName: String
    ): String {
        val skill = skillRepository.getSkill(skillName)
        return if (skill != null) {
            skill.content
        } else {
            "Skill '$skillName' not found. Available skills: ${skillRepository.loadSkills().joinToString(", ") { it.name }}"
        }
    }
}

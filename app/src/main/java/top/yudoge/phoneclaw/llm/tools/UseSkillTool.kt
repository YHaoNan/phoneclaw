package top.yudoge.phoneclaw.llm.tools

import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import top.yudoge.phoneclaw.llm.skills.SkillRepository

@LLMDescription("Tool for using skills to extend capabilities")
class UseSkillTool(
    private val skillRepository: SkillRepository
) : ToolSet {

    @Tool
    @LLMDescription("Use a skill to get its content. Returns the skill documentation which provides additional capabilities.")
    fun useSkill(
        @LLMDescription("Name of the skill to use")
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

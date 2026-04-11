package top.yudoge.phoneclaw.llm.integration.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import top.yudoge.phoneclaw.app.AppContainer

class UseSkillTool {

    @Tool(
        name = "useSkill",
        value = ["Use a skill to get its content. Returns the skill documentation which provides additional capabilities."]
    )
    fun useSkill(
        @P("Name of the skill to use")
        skillName: String
    ): String {
        val skillFacade = AppContainer.getInstance().skillFacade
        val skill = skillFacade.getSkillByName(skillName)
        return if (skill != null) {
            val content = skillFacade.getSkillContent(skill)
            content?.content ?: "Skill content not found for '$skillName'"
        } else {
            val available = skillFacade.getAllSkills().joinToString(", ") { it.name }
            "Skill '$skillName' not found. Available skills: $available"
        }
    }
}

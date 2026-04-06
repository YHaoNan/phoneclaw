package top.yudoge.phoneclaw.llm.tools

import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import top.yudoge.phoneclaw.llm.skills.Skill
import top.yudoge.phoneclaw.llm.skills.SkillRepository

@LLMDescription("Tools for loading and managing skills dynamically")
class UseSkillTool(
    private val skillRepository: SkillRepository
) : ToolSet {

    private val loadedSkills = mutableMapOf<String, Skill>()

    @Tool
    @LLMDescription("List all available skills that can be loaded")
    fun listSkills(): String {
        val skills = skillRepository.loadSkills()
        if (skills.isEmpty()) {
            return "No skills available."
        }
        
        val sb = StringBuilder("Available skills:\n")
        skills.forEach { skill ->
            val loaded = if (loadedSkills.containsKey(skill.name)) " [LOADED]" else ""
            sb.append("- ${skill.name}$loaded: ${skill.description}\n")
        }
        return sb.toString()
    }

    @Tool
    @LLMDescription("Load a skill to get its full documentation and capabilities. Returns the skill content that describes how to use it.")
    fun loadSkill(
        @LLMDescription("Name of the skill to load")
        skillName: String
    ): String {
        if (loadedSkills.containsKey(skillName)) {
            val existingSkill = loadedSkills[skillName]!!
            return "Skill '$skillName' is already loaded.\n\n${existingSkill.content}"
        }

        val skill = skillRepository.getSkill(skillName)
            ?: return "Skill '$skillName' not found. Use listSkills to see available skills."

        loadedSkills[skillName] = skill

        val sb = StringBuilder()
        sb.append("Skill '$skillName' loaded successfully.\n\n")
        sb.append("=== SKILL: $skillName ===\n")
        sb.append("Description: ${skill.description}\n\n")
        
        if (!skill.argumentHint.isNullOrEmpty()) {
            sb.append("Arguments: ${skill.argumentHint}\n\n")
        }
        
        sb.append("Content:\n${skill.content}\n")
        
        if (skill.supportingFiles.isNotEmpty()) {
            sb.append("\nSupporting files: ${skill.supportingFiles.joinToString(", ")}\n")
        }

        return sb.toString()
    }

    @Tool
    @LLMDescription("Get the content of a loaded skill without loading it")
    fun getSkillContent(
        @LLMDescription("Name of the skill")
        skillName: String
    ): String {
        val skill = loadedSkills[skillName]
            ?: return "Skill '$skillName' is not loaded. Use loadSkill first."
        
        return skill.content
    }

    @Tool
    @LLMDescription("Unload a previously loaded skill to free context")
    fun unloadSkill(
        @LLMDescription("Name of the skill to unload")
        skillName: String
    ): String {
        if (!loadedSkills.containsKey(skillName)) {
            return "Skill '$skillName' is not loaded."
        }
        
        loadedSkills.remove(skillName)
        return "Skill '$skillName' unloaded successfully."
    }

    @Tool
    @LLMDescription("Check which skills are currently loaded")
    fun status(): String {
        if (loadedSkills.isEmpty()) {
            return "No skills currently loaded."
        }
        
        val sb = StringBuilder("Currently loaded skills:\n")
        loadedSkills.forEach { (name, skill) ->
            sb.append("- $name: ${skill.description}\n")
        }
        return sb.toString()
    }

    fun getLoadedSkill(name: String): Skill? = loadedSkills[name]
    
    fun getAllLoadedSkills(): Map<String, Skill> = loadedSkills.toMap()
}

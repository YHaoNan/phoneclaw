package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.repository.BuiltInSkillRepository
import top.yudoge.phoneclaw.llm.data.repository.UserSkillRepository
import top.yudoge.phoneclaw.llm.domain.objects.Skill
import top.yudoge.phoneclaw.llm.domain.objects.SkillSource
import top.yudoge.phoneclaw.llm.domain.objects.SkillWithContent

class SkillFacade(
    private val builtInSkillRepository: BuiltInSkillRepository,
    private val userSkillRepository: UserSkillRepository
) {
    fun getAllSkills(): List<Skill> {
        val builtIn = builtInSkillRepository.getAll()
        val user = userSkillRepository.getAll()
        return builtIn + user
    }
    
    fun getBuiltInSkills(): List<Skill> = builtInSkillRepository.getAll()
    
    fun getUserSkills(): List<Skill> = userSkillRepository.getAll()
    
    fun getSkillByName(name: String): Skill? {
        return builtInSkillRepository.getByName(name) 
            ?: userSkillRepository.getByName(name)
    }
    
    fun getSkillContent(skill: Skill): SkillWithContent? {
        return when (skill.source) {
            SkillSource.BUILT_IN -> builtInSkillRepository.getContent(skill)
            SkillSource.USER -> userSkillRepository.getContent(skill)
        }
    }
    
    fun createUserSkill(skill: Skill, content: String): Boolean {
        return userSkillRepository.insert(skill, content)
    }
    
    fun updateUserSkill(skill: Skill, content: String?): Boolean {
        return userSkillRepository.update(skill, content)
    }
    
    fun deleteUserSkill(name: String): Boolean {
        return userSkillRepository.delete(name)
    }
    
    fun searchSkills(query: String): List<Skill> {
        return getAllSkills().filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }
}

package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.data.entity.SkillEntityWithContent
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
        val builtIn = builtInSkillRepository.getAll().map { it.toDomain(SkillSource.BUILT_IN) }
        val user = userSkillRepository.getAll().map { it.toDomain(SkillSource.USER) }
        return builtIn + user
    }
    
    fun getBuiltInSkills(): List<Skill> = 
        builtInSkillRepository.getAll().map { it.toDomain(SkillSource.BUILT_IN) }
    
    fun getUserSkills(): List<Skill> = 
        userSkillRepository.getAll().map { it.toDomain(SkillSource.USER) }
    
    fun getSkillByName(name: String): Skill? {
        return builtInSkillRepository.getByName(name)?.toDomain(SkillSource.BUILT_IN)
            ?: userSkillRepository.getByName(name)?.toDomain(SkillSource.USER)
    }
    
    fun getSkillContent(skill: Skill): SkillWithContent? {
        return when (skill.source) {
            SkillSource.BUILT_IN -> builtInSkillRepository.getContent(skill.toEntity())?.toDomain(SkillSource.BUILT_IN)
            SkillSource.USER -> userSkillRepository.getContent(skill.toEntity())?.toDomain(SkillSource.USER)
        }
    }
    
    fun createUserSkill(skill: Skill, content: String): Boolean {
        return userSkillRepository.insert(skill.toEntity(), content)
    }
    
    fun updateUserSkill(skill: Skill, content: String?): Boolean {
        return userSkillRepository.update(skill.toEntity(), content)
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
    
    private fun SkillEntity.toDomain(source: SkillSource) = Skill(
        name = name,
        description = description,
        argumentHint = argumentHint,
        disableModelInvocation = disableModelInvocation,
        userInvocable = userInvocable,
        allowedTools = allowedTools?.split(","),
        context = context,
        source = source,
        skillDir = skillDir,
        supportingFiles = supportingFiles?.split(",") ?: emptyList()
    )
    
    private fun Skill.toEntity() = SkillEntity(
        name = name,
        description = description,
        argumentHint = argumentHint,
        disableModelInvocation = disableModelInvocation,
        userInvocable = userInvocable,
        allowedTools = allowedTools?.joinToString(","),
        context = context,
        skillDir = skillDir,
        supportingFiles = supportingFiles.joinToString(",")
    )
    
    private fun SkillEntityWithContent.toDomain(source: SkillSource) = SkillWithContent(
        skill = entity.toDomain(source),
        content = content
    )
}

package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.data.entity.SkillEntityWithContent
import top.yudoge.phoneclaw.llm.data.repository.BuiltInSkillRepository
import top.yudoge.phoneclaw.llm.data.repository.UserSkillRepository
import top.yudoge.phoneclaw.llm.domain.objects.Skill
import top.yudoge.phoneclaw.llm.domain.objects.SkillSource
import top.yudoge.phoneclaw.llm.domain.objects.SkillWithContent
import java.util.LinkedHashMap

class SkillFacade(
    private val builtInSkillRepository: BuiltInSkillRepository,
    private val userSkillRepository: UserSkillRepository
) {
    fun getAllSkills(): List<Skill> {
        val builtIn = getBuiltInSkills()
        val user = getUserSkills()

        // Deterministic precedence: user skill overrides built-in skill with the same name.
        val merged = LinkedHashMap<String, Skill>()
        builtIn.forEach { merged[it.name] = it }
        user.forEach { merged[it.name] = it }
        return merged.values.toList()
    }
    
    fun getBuiltInSkills(): List<Skill> = 
        builtInSkillRepository.getAll().map { it.toDomain(SkillSource.BUILT_IN) }
    
    fun getUserSkills(): List<Skill> = 
        userSkillRepository.getAll().map { it.toDomain(SkillSource.USER) }
    
    fun getSkillByName(name: String): Skill? {
        return userSkillRepository.getByName(name)?.toDomain(SkillSource.USER)
            ?: builtInSkillRepository.getByName(name)?.toDomain(SkillSource.BUILT_IN)
    }
    
    fun getSkillContent(skill: Skill): SkillWithContent? {
        return when (skill.source) {
            SkillSource.BUILT_IN -> {
                builtInSkillRepository.getContent(skill.toEntity())?.toDomain(SkillSource.BUILT_IN)
                    ?: builtInSkillRepository.getByName(skill.name)
                        ?.let { builtInSkillRepository.getContent(it) }
                        ?.toDomain(SkillSource.BUILT_IN)
            }
            SkillSource.USER -> {
                userSkillRepository.getContent(skill.toEntity())?.toDomain(SkillSource.USER)
                    ?: userSkillRepository.getByName(skill.name)
                        ?.let { userSkillRepository.getContent(it) }
                        ?.toDomain(SkillSource.USER)
            }
        }
    }
    
    fun createUserSkill(skill: Skill, content: String): Boolean {
        if (skill.source == SkillSource.BUILT_IN) return false
        if (userSkillRepository.getByName(skill.name) != null) return false
        return userSkillRepository.insert(skill.copy(source = SkillSource.USER).toEntity(), content)
    }
    
    fun updateUserSkill(skill: Skill, content: String?): Boolean {
        if (skill.source == SkillSource.BUILT_IN) return false
        val existing = userSkillRepository.getByName(skill.name) ?: return false
        val merged = skill.toEntity().copy(
            skillDir = existing.skillDir ?: skill.skillDir,
            createdAt = existing.createdAt
        )
        return userSkillRepository.update(merged, content)
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
        allowedTools = allowedTools
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() },
        context = context,
        source = source,
        skillDir = skillDir,
        supportingFiles = supportingFiles
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    )
    
    private fun Skill.toEntity() = SkillEntity(
        name = name,
        description = description,
        argumentHint = argumentHint,
        disableModelInvocation = disableModelInvocation,
        userInvocable = userInvocable,
        allowedTools = allowedTools?.takeIf { it.isNotEmpty() }?.joinToString(","),
        context = context,
        skillDir = skillDir?.takeIf { it.isNotBlank() },
        supportingFiles = supportingFiles.takeIf { it.isNotEmpty() }?.joinToString(",")
    )
    
    private fun SkillEntityWithContent.toDomain(source: SkillSource) = SkillWithContent(
        skill = entity.toDomain(source),
        content = content
    )
}

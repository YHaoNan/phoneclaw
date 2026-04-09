package top.yudoge.phoneclaw.llm.skills

class CompositeSkillRepository(
    private val builtInRepository: SkillRepository,
    private val userRepository: SkillRepository
) : SkillRepository {

    override fun loadSkills(): List<Skill> {
        val builtInSkills = builtInRepository.loadSkills()
        val userSkills = userRepository.loadSkills()
        return builtInSkills + userSkills
    }

    override fun getSkill(name: String): Skill? {
        return builtInRepository.getSkill(name) ?: userRepository.getSkill(name)
    }

    override fun addSkill(skill: Skill) {
        if (builtInRepository.skillExists(skill.name)) {
            throw IllegalArgumentException("Skill '${skill.name}' already exists as built-in skill")
        }
        userRepository.addSkill(skill)
    }

    override fun updateSkill(skill: Skill) {
        if (builtInRepository.skillExists(skill.name)) {
            throw IllegalArgumentException("Cannot update built-in skill '${skill.name}'")
        }
        userRepository.updateSkill(skill)
    }

    override fun deleteSkill(skill: Skill) {
        if (skill.isBuiltIn) {
            throw IllegalArgumentException("Cannot delete built-in skill '${skill.name}'")
        }
        userRepository.deleteSkill(skill)
    }

    override fun deleteSkillByName(name: String) {
        if (builtInRepository.skillExists(name)) {
            throw IllegalArgumentException("Cannot delete built-in skill '$name'")
        }
        userRepository.deleteSkillByName(name)
    }

    override fun skillExists(name: String): Boolean {
        return builtInRepository.skillExists(name) || userRepository.skillExists(name)
    }

    fun getBuiltInSkills(): List<Skill> = builtInRepository.loadSkills()
    
    fun getUserSkills(): List<Skill> = userRepository.loadSkills()
    
    fun isBuiltInSkill(name: String): Boolean = builtInRepository.skillExists(name)
}

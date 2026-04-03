package top.yudoge.phoneclaw.llm.skills

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class FileBasedSkillRepositoryTest {

    private lateinit var skillsDir: File
    private lateinit var repository: FileBasedSkillRepository

    @Before
    fun setUp() {
        skillsDir = File.createTempFile("skills-test", "").apply {
            delete()
            mkdirs()
        }
        repository = FileBasedSkillRepository(skillsDir)
    }

    @Test
    fun `loadSkills returns empty list when directory is empty`() {
        val skills = repository.loadSkills()
        assertTrue(skills.isEmpty())
    }

    @Test
    fun `loadSkills returns all valid skills`() {
        createSkillDirectory("skill-one", "First skill", "Content one")
        createSkillDirectory("skill-two", "Second skill", "Content two")
        createInvalidDirectory("invalid-skill")

        val skills = repository.loadSkills()

        assertEquals(2, skills.size)
        val names = skills.map { it.name }
        assertTrue(names.contains("skill-one"))
        assertTrue(names.contains("skill-two"))
    }

    @Test
    fun `getSkill returns skill by name`() {
        createSkillDirectory("test-skill", "Test skill", "Test content")

        val skill = repository.getSkill("test-skill")

        assertNotNull(skill)
        assertEquals("test-skill", skill?.name)
        assertEquals("Test skill", skill?.description)
        assertEquals("Test content", skill?.content)
    }

    @Test
    fun `getSkill returns null for non-existent skill`() {
        val skill = repository.getSkill("non-existent")
        assertNull(skill)
    }

    @Test
    fun `addSkill creates skill directory and files`() {
        val skill = Skill(
            name = "new-skill",
            description = "A new skill",
            content = "New skill content"
        )

        repository.addSkill(skill)

        val skillDir = File(skillsDir, "new-skill")
        assertTrue(skillDir.exists())
        assertTrue(skillDir.isDirectory)

        val skillMdFile = File(skillDir, "SKILL.md")
        assertTrue(skillMdFile.exists())

        val loadedSkill = repository.getSkill("new-skill")
        assertNotNull(loadedSkill)
        assertEquals("new-skill", loadedSkill?.name)
        assertEquals("A new skill", loadedSkill?.description)
        assertEquals("New skill content", loadedSkill?.content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addSkill throws when skill already exists`() {
        val skill = Skill(
            name = "duplicate-skill",
            description = "Duplicate",
            content = "Content"
        )

        repository.addSkill(skill)
        repository.addSkill(skill)
    }

    @Test
    fun `addSkill with full metadata preserves all fields`() {
        val skill = Skill(
            name = "full-skill",
            description = "Full metadata skill",
            argumentHint = "<file>",
            disableModelInvocation = true,
            userInvocable = false,
            allowedTools = listOf("Read", "Write", "Bash"),
            context = "fork",
            content = "Content"
        )

        repository.addSkill(skill)

        val loadedSkill = repository.getSkill("full-skill")

        assertNotNull(loadedSkill)
        assertEquals("full-skill", loadedSkill?.name)
        assertEquals("Full metadata skill", loadedSkill?.description)
        assertEquals("<file>", loadedSkill?.argumentHint)
        assertTrue(loadedSkill?.disableModelInvocation == true)
        assertFalse(loadedSkill?.userInvocable == true)
        assertEquals(listOf("Read", "Write", "Bash"), loadedSkill?.allowedTools)
        assertEquals("fork", loadedSkill?.context)
    }

    @Test
    fun `updateSkill updates existing skill`() {
        createSkillDirectory("update-skill", "Old description", "Old content")

        val updatedSkill = Skill(
            name = "update-skill",
            description = "New description",
            content = "New content"
        )

        repository.updateSkill(updatedSkill)

        val loadedSkill = repository.getSkill("update-skill")
        assertNotNull(loadedSkill)
        assertEquals("New description", loadedSkill?.description)
        assertEquals("New content", loadedSkill?.content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateSkill throws when skill does not exist`() {
        val skill = Skill(
            name = "non-existent",
            description = "Test",
            content = "Test"
        )

        repository.updateSkill(skill)
    }

    @Test
    fun `deleteSkill deletes skill directory`() {
        createSkillDirectory("delete-me", "Delete this", "Content")

        val skill = repository.getSkill("delete-me")
        assertNotNull(skill)

        repository.deleteSkill(skill!!)

        val skillDir = File(skillsDir, "delete-me")
        assertFalse(skillDir.exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deleteSkill throws when directory does not exist`() {
        val skill = Skill(
            name = "non-existent",
            description = "Test",
            content = "Test",
            skillDir = "non-existent"
        )

        repository.deleteSkill(skill)
    }

    @Test
    fun `deleteSkillByName deletes skill directory`() {
        createSkillDirectory("delete-by-name", "Delete this", "Content")

        repository.deleteSkillByName("delete-by-name")

        val skillDir = File(skillsDir, "delete-by-name")
        assertFalse(skillDir.exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deleteSkillByName throws when skill does not exist`() {
        repository.deleteSkillByName("non-existent")
    }

    @Test
    fun `skillExists returns true for existing skill`() {
        createSkillDirectory("exists-skill", "Test", "Content")

        assertTrue(repository.skillExists("exists-skill"))
    }

    @Test
    fun `skillExists returns false for non-existent skill`() {
        assertFalse(repository.skillExists("non-existent"))
    }

    @Test
    fun `skill relative path is correct`() {
        createSkillDirectory("path-test", "Test", "Content")

        val skill = repository.getSkill("path-test")
        assertNotNull(skill)
        assertEquals("path-test", skill?.skillDir)
    }

    @Test
    fun `supporting files are detected`() {
        val skillDir = File(skillsDir, "with-files")
        skillDir.mkdirs()

        File(skillDir, "SKILL.md").writeText("""
            ---
            name: with-files
            description: Skill with supporting files
            ---

            Content
        """.trimIndent())

        val scriptsDir = File(skillDir, "scripts").apply { mkdirs() }
        File(scriptsDir, "helper.sh").writeText("#!/bin/bash\necho hello")

        val skill = repository.getSkill("with-files")
        assertNotNull(skill)
        assertTrue(skill?.supportingFiles?.contains("scripts/helper.sh") == true)
    }

    @Test
    fun `loadSkills creates directory if not exists`() {
        val emptyDir = File.createTempFile("empty-dir", "").apply {
            delete()
        }

        val repo = FileBasedSkillRepository(emptyDir)
        val skills = repo.loadSkills()

        assertTrue(emptyDir.exists())
        assertTrue(skills.isEmpty())

        emptyDir.delete()
    }

    private fun createSkillDirectory(name: String, description: String, content: String) {
        val skillDir = File(skillsDir, name)
        skillDir.mkdirs()

        File(skillDir, "SKILL.md").writeText("""
            ---
            name: $name
            description: $description
            ---

            $content
        """.trimIndent())
    }

    private fun createInvalidDirectory(name: String) {
        val dir = File(skillsDir, name)
        dir.mkdirs()
        // No SKILL.md file
    }
}

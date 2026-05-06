package top.yudoge.phoneclaw.llm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yudoge.phoneclaw.llm.data.entity.TaskScriptEntity
import top.yudoge.phoneclaw.llm.data.repository.TaskScriptRepository

class TaskScriptFacadeTest {

    @Test
    fun `create and update keep createdAt immutable`() {
        val repo = InMemoryTaskScriptRepository()
        val facade = TaskScriptFacade(repo)

        assertTrue(facade.createScript("daily-checkin", "summary", "print('ok')"))
        val created = facade.getScriptByName("daily-checkin")!!
        val createdAt = created.createdTime

        assertTrue(facade.updateScript(created.id, "daily-checkin", "summary2", "print('ok2')"))
        val updated = facade.getScriptById(created.id)!!
        assertEquals(createdAt, updated.createdTime)
        assertEquals("summary2", updated.summary)
    }

    @Test
    fun `create validation should reject blank values`() {
        val facade = TaskScriptFacade(InMemoryTaskScriptRepository())
        assertFalse(runCatching { facade.createScript(" ", "s", "c") }.isSuccess)
        assertFalse(runCatching { facade.createScript("name", " ", "c") }.isSuccess)
        assertFalse(runCatching { facade.createScript("name", "s", " ") }.isSuccess)
    }

    private class InMemoryTaskScriptRepository : TaskScriptRepository {
        private val map = linkedMapOf<String, TaskScriptEntity>()
        override fun getAll(): List<TaskScriptEntity> = map.values.toList()
        override fun getById(id: String): TaskScriptEntity? = map[id]
        override fun getByName(name: String): TaskScriptEntity? = map.values.firstOrNull { it.name == name }
        override fun insert(entity: TaskScriptEntity): Boolean {
            map[entity.id] = entity
            return true
        }
        override fun update(entity: TaskScriptEntity): Boolean {
            map[entity.id] = entity
            return true
        }
        override fun delete(id: String): Boolean = map.remove(id) != null
    }
}

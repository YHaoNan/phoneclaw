package top.yudoge.phoneclaw.llm.domain

import java.util.UUID
import top.yudoge.phoneclaw.llm.data.entity.TaskScriptEntity
import top.yudoge.phoneclaw.llm.data.repository.TaskScriptRepository
import top.yudoge.phoneclaw.llm.domain.objects.TaskScript

class TaskScriptFacade(
    private val repository: TaskScriptRepository
) {

    fun getAllScripts(): List<TaskScript> = repository.getAll().map { it.toDomain() }

    fun getScriptById(id: String): TaskScript? = repository.getById(id)?.toDomain()

    fun getScriptByName(name: String): TaskScript? = repository.getByName(name)?.toDomain()

    fun createScript(name: String, summary: String, codeContent: String): Boolean {
        validate(name, summary, codeContent)
        if (repository.getByName(name) != null) return false
        val now = System.currentTimeMillis()
        return repository.insert(
            TaskScriptEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                summary = summary.trim(),
                createdAt = now,
                content = codeContent
            )
        )
    }

    fun updateScript(id: String, name: String, summary: String, codeContent: String): Boolean {
        validate(name, summary, codeContent)
        val existing = repository.getById(id) ?: return false
        val byName = repository.getByName(name)
        if (byName != null && byName.id != id) return false
        return repository.update(
            existing.copy(
                name = name.trim(),
                summary = summary.trim(),
                content = codeContent
            )
        )
    }

    fun deleteScript(id: String): Boolean = repository.delete(id)

    private fun validate(name: String, summary: String, codeContent: String) {
        require(name.trim().isNotEmpty()) { "Script name is required" }
        require(summary.trim().isNotEmpty()) { "Script summary is required" }
        require(codeContent.isNotBlank()) { "Script content is required" }
    }

    private fun TaskScriptEntity.toDomain(): TaskScript {
        return TaskScript(
            id = id,
            name = name,
            summary = summary,
            createdTime = createdAt,
            codeContent = content
        )
    }
}

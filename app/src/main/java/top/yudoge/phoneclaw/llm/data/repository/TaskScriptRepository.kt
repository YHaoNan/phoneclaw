package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.TaskScriptEntity

interface TaskScriptRepository {
    fun getAll(): List<TaskScriptEntity>
    fun getById(id: String): TaskScriptEntity?
    fun getByName(name: String): TaskScriptEntity?
    fun insert(entity: TaskScriptEntity): Boolean
    fun update(entity: TaskScriptEntity): Boolean
    fun delete(id: String): Boolean
}

package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.SkillEntity
import top.yudoge.phoneclaw.llm.data.entity.SkillEntityWithContent

interface SkillRepository {
    fun getAll(): List<SkillEntity>
    fun getByName(name: String): SkillEntity?
    fun getContent(entity: SkillEntity): SkillEntityWithContent?
    fun insert(entity: SkillEntity, content: String): Boolean
    fun update(entity: SkillEntity, content: String?): Boolean
    fun delete(name: String): Boolean
}

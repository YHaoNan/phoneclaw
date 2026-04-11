package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.ModelEntity

interface ModelRepository {
    fun getAll(): List<ModelEntity>
    fun getById(id: String): ModelEntity?
    fun getByProviderId(providerId: Long): List<ModelEntity>
    fun insert(entity: ModelEntity)
    fun update(entity: ModelEntity)
    fun delete(id: String)
}

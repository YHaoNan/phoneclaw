package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity

interface ModelProviderRepository {
    fun getAll(): List<ModelProviderEntity>
    fun getById(id: Long): ModelProviderEntity?
    fun insert(entity: ModelProviderEntity): Long
    fun update(entity: ModelProviderEntity)
    fun delete(id: Long)
}

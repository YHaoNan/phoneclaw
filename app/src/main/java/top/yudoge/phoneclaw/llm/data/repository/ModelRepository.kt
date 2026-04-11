package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.domain.objects.Model

interface ModelRepository {
    fun getAll(): List<Model>
    fun getById(id: String): Model?
    fun getByProviderId(providerId: Long): List<Model>
    fun insert(model: Model)
    fun update(model: Model)
    fun delete(id: String)
}

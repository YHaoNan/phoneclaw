package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

interface ModelProviderRepository {
    fun getAll(): List<ModelProvider>
    fun getById(id: Long): ModelProvider?
    fun insert(modelProvider: ModelProvider): Long
    fun update(modelProvider: ModelProvider)
    fun delete(id: Long)
}

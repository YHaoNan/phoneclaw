package top.yudoge.phoneclaw.llm.provider

interface ModelRepository {
    fun addModel(model: ModelEntity)
    fun updateModel(model: ModelEntity)
    fun deleteModel(id: String)
    fun getModelsByProvider(providerId: Long): List<ModelEntity>
    fun getModel(id: String): ModelEntity?
}

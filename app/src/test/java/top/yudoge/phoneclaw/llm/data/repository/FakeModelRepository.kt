package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.ModelEntity

class FakeModelRepository : ModelRepository {
    private val models = mutableMapOf<String, ModelEntity>()

    override fun getAll(): List<ModelEntity> = models.values.toList()

    override fun getById(id: String): ModelEntity? = models[id]

    override fun getByProviderId(providerId: Long): List<ModelEntity> =
        models.values.filter { it.providerId == providerId }

    override fun insert(entity: ModelEntity) {
        models[entity.id] = entity
    }

    override fun update(entity: ModelEntity) {
        models[entity.id] = entity
    }

    override fun delete(id: String) {
        models.remove(id)
    }

    fun clear() {
        models.clear()
    }
}

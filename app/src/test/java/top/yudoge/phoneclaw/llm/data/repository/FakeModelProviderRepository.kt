package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity

class FakeModelProviderRepository : ModelProviderRepository {
    private val providers = mutableMapOf<Long, ModelProviderEntity>()
    private var nextId = 1L

    override fun getAll(): List<ModelProviderEntity> = providers.values.toList()

    override fun getById(id: Long): ModelProviderEntity? = providers[id]

    override fun insert(entity: ModelProviderEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        providers[id] = entity.copy(id = id)
        return id
    }

    override fun update(entity: ModelProviderEntity) {
        providers[entity.id] = entity
    }

    override fun delete(id: Long) {
        providers.remove(id)
    }

    fun clear() {
        providers.clear()
        nextId = 1L
    }
}

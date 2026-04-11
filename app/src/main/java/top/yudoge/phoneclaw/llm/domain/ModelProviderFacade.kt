package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.entity.ModelEntity
import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity
import top.yudoge.phoneclaw.llm.data.repository.ModelProviderRepository
import top.yudoge.phoneclaw.llm.data.repository.ModelRepository
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType
import top.yudoge.phoneclaw.llm.domain.objects.ProviderWithModels

class ModelProviderFacade(
    private val modelProviderRepository: ModelProviderRepository,
    private val modelRepository: ModelRepository,
    private val modelProviderFactory: ModelProviderFactory
) {
    fun getAllProviders(): List<ModelProvider> =
        modelProviderRepository.getAll().map { entityToProvider(it) }

    fun getProviderById(id: Long): ModelProvider? =
        modelProviderRepository.getById(id)?.let { entityToProvider(it) }

    fun addProvider(name: String, providerType: ProviderType, config: String = ""): Long {
        val entity = ModelProviderEntity(
            id = 0,
            name = name,
            providerType = providerType.name,
            modelProviderConfig = config
        )
        return modelProviderRepository.insert(entity)
    }

    fun updateProvider(provider: ModelProvider) {
        val entity = ModelProviderEntity(
            id = provider.id,
            name = provider.name,
            providerType = provider.providerType.name,
            modelProviderConfig = provider.parseToConfig()
        )
        modelProviderRepository.update(entity)
    }

    fun deleteProvider(id: Long) {
        modelRepository.getByProviderId(id).forEach { modelRepository.delete(it.id) }
        modelProviderRepository.delete(id)
    }

    fun getModelsByProvider(providerId: Long): List<Model> =
        modelRepository.getByProviderId(providerId).map { it.toDomain() }

    fun getAllModels(): List<Model> =
        modelRepository.getAll().map { it.toDomain() }

    fun getModelById(id: String): Model? =
        modelRepository.getById(id)?.toDomain()

    fun addModel(model: Model) =
        modelRepository.insert(model.toEntity())

    fun updateModel(model: Model) =
        modelRepository.update(model.toEntity())

    fun deleteModel(id: String) = modelRepository.delete(id)

    fun getAllProvidersWithModels(): List<ProviderWithModels> {
        return getAllProviders().map { provider ->
            ProviderWithModels(
                provider = provider,
                models = getModelsByProvider(provider.id)
            )
        }
    }

    private fun entityToProvider(entity: ModelProviderEntity): ModelProvider {
        val providerType = try {
            ProviderType.valueOf(entity.providerType)
        } catch (e: Exception) {
            ProviderType.OpenAICompatible
        }
        return modelProviderFactory.create(entity.id, entity.name, providerType, entity.modelProviderConfig)
    }

    private fun ModelEntity.toDomain() = Model(
        id = id,
        providerId = providerId,
        displayName = displayName,
        hasVisualCapability = hasVisualCapability
    )

    private fun Model.toEntity() = ModelEntity(
        id = id,
        providerId = providerId,
        displayName = displayName,
        hasVisualCapability = hasVisualCapability
    )
}

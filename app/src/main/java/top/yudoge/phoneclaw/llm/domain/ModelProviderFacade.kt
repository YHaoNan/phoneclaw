package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.entity.ModelEntity
import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity
import top.yudoge.phoneclaw.llm.data.repository.ModelProviderRepository
import top.yudoge.phoneclaw.llm.data.repository.ModelRepository
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class ModelProviderFacade(
    private val modelProviderRepository: ModelProviderRepository,
    private val modelRepository: ModelRepository
) {
    fun getAllProviders(): List<ModelProvider> = 
        modelProviderRepository.getAll().map { it.toDomain() }
    
    fun getProviderById(id: Long): ModelProvider? = 
        modelProviderRepository.getById(id)?.toDomain()
    
    fun addProvider(provider: ModelProvider): Long = 
        modelProviderRepository.insert(provider.toEntity())
    
    fun updateProvider(provider: ModelProvider) = 
        modelProviderRepository.update(provider.toEntity())
    
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
    
    private fun ModelProviderEntity.toDomain() = ModelProvider(
        id = id,
        name = name,
        providerType = providerType,
        config = modelProviderConfig
    )
    
    private fun ModelProvider.toEntity() = ModelProviderEntity(
        id = id,
        name = name,
        providerType = providerType,
        modelProviderConfig = config
    )
    
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

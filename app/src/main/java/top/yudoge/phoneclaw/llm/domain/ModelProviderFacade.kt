package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.repository.ModelProviderRepository
import top.yudoge.phoneclaw.llm.data.repository.ModelRepository
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class ModelProviderFacade(
    private val modelProviderRepository: ModelProviderRepository,
    private val modelRepository: ModelRepository
) {
    fun getAllProviders(): List<ModelProvider> = modelProviderRepository.getAll()
    
    fun getProviderById(id: Long): ModelProvider? = modelProviderRepository.getById(id)
    
    fun addProvider(provider: ModelProvider): Long = modelProviderRepository.insert(provider)
    
    fun updateProvider(provider: ModelProvider) = modelProviderRepository.update(provider)
    
    fun deleteProvider(id: Long) {
        modelRepository.getByProviderId(id).forEach { modelRepository.delete(it.id) }
        modelProviderRepository.delete(id)
    }
    
    fun getModelsByProvider(providerId: Long): List<Model> = modelRepository.getByProviderId(providerId)
    
    fun getAllModels(): List<Model> = modelRepository.getAll()
    
    fun getModelById(id: String): Model? = modelRepository.getById(id)
    
    fun addModel(model: Model) = modelRepository.insert(model)
    
    fun updateModel(model: Model) = modelRepository.update(model)
    
    fun deleteModel(id: String) = modelRepository.delete(id)
}

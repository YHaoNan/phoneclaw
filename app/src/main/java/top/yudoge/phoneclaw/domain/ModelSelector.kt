package top.yudoge.phoneclaw.domain

import top.yudoge.phoneclaw.llm.provider.ModelProviderRepository
import top.yudoge.phoneclaw.llm.provider.ModelRepository

class ModelSelector(
    private val providerRepo: ModelProviderRepository,
    private val modelRepo: ModelRepository
) {
    private var models: List<ModelSelection> = emptyList()
    var selectedIndex: Int = 0

    fun loadAvailableModels(): List<ModelSelection> {
        val providers = providerRepo.listProvider()
        models = providers.flatMap { provider ->
            modelRepo.getModelsByProvider(provider.id).map { model ->
                ModelSelection(provider, model)
            }
        }
        if (selectedIndex >= models.size) {
            selectedIndex = 0
        }
        return models
    }

    fun getAvailableModels(): List<ModelSelection> = models

    fun getSelectedModel(): ModelSelection? {
        return if (models.isNotEmpty() && selectedIndex in models.indices) {
            models[selectedIndex]
        } else null
    }

    fun selectModel(index: Int) {
        selectedIndex = if (index in models.indices) index else 0
    }

    fun getDisplayNames(): List<String> {
        return models.map { "${it.provider.name}: ${it.model.displayName}" }
    }
}

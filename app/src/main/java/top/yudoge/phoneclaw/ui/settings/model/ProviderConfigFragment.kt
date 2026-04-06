package top.yudoge.phoneclaw.ui.settings.model

interface ProviderConfigFragment {
    fun onNextStep(): Boolean
    fun getConfigJson(): String
    fun loadConfig(config: String)
    fun detectModels(callback: (List<String>) -> Unit, onError: (String) -> Unit)
}

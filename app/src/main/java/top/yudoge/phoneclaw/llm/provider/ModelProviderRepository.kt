package top.yudoge.phoneclaw.llm.provider

interface ModelProviderRepository {

    fun addProvider(provider: ModelProviderEntity)

    fun deleteProvider(id: Long)

    fun listProvider(): List<ModelProviderEntity>

    fun updateProvider(provider: ModelProviderEntity)

}
package top.yudoge.phoneclaw.llm.provider

interface ModelProviderRepository {

    fun addProvider(provider: ModelProviderEntity): Long

    fun deleteProvider(id: Long)

    fun getProvider(id: Long): ModelProviderEntity?

    fun listProvider(): List<ModelProviderEntity>

    fun updateProvider(provider: ModelProviderEntity)

}
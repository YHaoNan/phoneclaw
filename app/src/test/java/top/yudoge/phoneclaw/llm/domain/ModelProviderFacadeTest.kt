package top.yudoge.phoneclaw.llm.domain

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import top.yudoge.phoneclaw.llm.data.repository.FakeModelProviderRepository
import top.yudoge.phoneclaw.llm.data.repository.FakeModelRepository
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType
import top.yudoge.phoneclaw.llm.integration.ModelProviderFactoryImpl

class ModelProviderFacadeTest {
    
    private lateinit var providerRepository: FakeModelProviderRepository
    private lateinit var modelRepository: FakeModelRepository
    private lateinit var facade: ModelProviderFacade
    
    @Before
    fun setup() {
        providerRepository = FakeModelProviderRepository()
        modelRepository = FakeModelRepository()
        facade = ModelProviderFacade(
            modelProviderRepository = providerRepository,
            modelRepository = modelRepository,
            modelProviderFactory = ModelProviderFactoryImpl()
        )
    }
    
    @Test
    fun `addProvider creates provider and returns id`() {
        val id = facade.addProvider("Test Provider", ProviderType.OpenAICompatible)
        assertTrue(id > 0)
        
        val provider = facade.getProviderById(id)
        assertNotNull(provider)
        assertEquals("Test Provider", provider?.name)
        assertEquals(ProviderType.OpenAICompatible, provider?.providerType)
    }
    
    @Test
    fun `getAllProviders returns all providers`() {
        facade.addProvider("Provider 1", ProviderType.OpenAICompatible)
        facade.addProvider("Provider 2", ProviderType.OpenAICompatible)
        
        val providers = facade.getAllProviders()
        assertEquals(2, providers.size)
    }
    
    @Test
    fun `deleteProvider removes provider and its models`() {
        val providerId = facade.addProvider("Test", ProviderType.OpenAICompatible)
        
        facade.addModel(Model(
            id = "model-1",
            providerId = providerId,
            displayName = "Model 1"
        ))
        
        facade.deleteProvider(providerId)
        
        assertNull(facade.getProviderById(providerId))
        assertTrue(facade.getModelsByProvider(providerId).isEmpty())
    }
    
    @Test
    fun `addModel creates model for provider`() {
        val providerId = facade.addProvider("Test", ProviderType.OpenAICompatible)
        
        facade.addModel(Model(
            id = "gpt-4",
            providerId = providerId,
            displayName = "GPT-4",
            hasVisualCapability = true
        ))
        
        val models = facade.getModelsByProvider(providerId)
        assertEquals(1, models.size)
        assertEquals("gpt-4", models[0].id)
        assertEquals("GPT-4", models[0].displayName)
        assertTrue(models[0].hasVisualCapability)
    }
    
    @Test
    fun `getAllProvidersWithModels returns nested structure`() {
        val providerId = facade.addProvider("Test", ProviderType.OpenAICompatible)
        
        facade.addModel(Model(
            id = "model-1",
            providerId = providerId,
            displayName = "Model 1"
        ))
        
        val result = facade.getAllProvidersWithModels()
        assertEquals(1, result.size)
        assertEquals("Test", result[0].provider.name)
        assertEquals(1, result[0].models.size)
        assertEquals("model-1", result[0].models[0].id)
    }
}

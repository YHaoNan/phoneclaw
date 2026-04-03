package top.yudoge.phoneclaw.llm.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

@RunWith(AndroidJUnit4::class)
class ModelProviderEntityRepositoryImplTest {

    private lateinit var repository: ModelProviderRepositoryImpl
    private lateinit var dbHelper: PhoneClawDbHelper
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = PhoneClawDbHelper(context)
        dbHelper.writableDatabase.execSQL("DELETE FROM model_providers")
        dbHelper.writableDatabase.execSQL("DELETE FROM sqlite_sequence WHERE name='model_providers'")
        repository = ModelProviderRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        dbHelper.writableDatabase.execSQL("DELETE FROM model_providers")
        dbHelper.close()
    }

    private fun createProvider(
        name: String = "TestProvider",
        apiType: APIType = APIType.OpenAICompatible,
        hasVisualCapability: Boolean = false,
        modelProviderConfig: String = "{}"
    ): ModelProviderEntity {
        return ModelProviderEntity(
            id = 0L,
            name = name,
            apiType = apiType,
            hasVisualCapability = hasVisualCapability,
            modelProviderConfig = modelProviderConfig
        )
    }

    @Test
    fun addProvider_shouldInsertAndReturnWithGeneratedId() {
        val provider = createProvider(name = "GPT4")
        repository.addProvider(provider)

        val providers = repository.listProvider()
        Assert.assertEquals(1, providers.size)
        Assert.assertTrue(providers[0].id > 0)
        Assert.assertEquals("GPT4", providers[0].name)
    }

    @Test
    fun addProvider_shouldStoreAllFieldsCorrectly() {
        val provider = createProvider(
            name = "Claude",
            hasVisualCapability = true,
            modelProviderConfig = """{"apiKey": "test-key", "baseUrl": "https://api.anthropic.com"}"""
        )
        repository.addProvider(provider)

        val providers = repository.listProvider()
        Assert.assertEquals(1, providers.size)
        Assert.assertEquals("Claude", providers[0].name)
        Assert.assertEquals(APIType.OpenAICompatible, providers[0].apiType)
        Assert.assertTrue(providers[0].hasVisualCapability)
        Assert.assertEquals(
            """{"apiKey": "test-key", "baseUrl": "https://api.anthropic.com"}""",
            providers[0].modelProviderConfig
        )
    }

    @Test
    fun deleteProvider_shouldRemoveProviderById() {
        repository.addProvider(createProvider(name = "ToDelete"))
        val providers = repository.listProvider()
        val id = providers[0].id

        repository.deleteProvider(id)
        Assert.assertEquals(0, repository.listProvider().size)
    }

    @Test
    fun deleteProvider_withNonExistentId_shouldNotThrow() {
        repository.deleteProvider(999L)
        Assert.assertEquals(0, repository.listProvider().size)
    }

    @Test
    fun listProvider_shouldReturnEmptyWhenNoProviders() {
        val providers = repository.listProvider()
        Assert.assertTrue(providers.isEmpty())
    }

    @Test
    fun listProvider_shouldReturnAllProvidersOrderedById() {
        repository.addProvider(createProvider(name = "Provider1"))
        repository.addProvider(createProvider(name = "Provider2"))
        repository.addProvider(createProvider(name = "Provider3"))

        val providers = repository.listProvider()
        Assert.assertEquals(3, providers.size)
        Assert.assertEquals("Provider1", providers[0].name)
        Assert.assertEquals("Provider2", providers[1].name)
        Assert.assertEquals("Provider3", providers[2].name)
    }

    @Test
    fun updateProvider_shouldUpdateExistingProvider() {
        repository.addProvider(createProvider(name = "Original", hasVisualCapability = false))
        val providers = repository.listProvider()
        val id = providers[0].id

        val updated = ModelProviderEntity(
            id = id,
            name = "Updated",
            apiType = APIType.OpenAICompatible,
            hasVisualCapability = true,
            modelProviderConfig = """{"newKey": "newValue"}"""
        )
        repository.updateProvider(updated)

        val fetched = repository.listProvider()
        Assert.assertEquals(1, fetched.size)
        Assert.assertEquals("Updated", fetched[0].name)
        Assert.assertTrue(fetched[0].hasVisualCapability)
        Assert.assertEquals("""{"newKey": "newValue"}""", fetched[0].modelProviderConfig)
    }

    @Test
    fun updateProvider_withNonExistentId_shouldNotThrow() {
        val provider = ModelProviderEntity(
            id = 999L,
            name = "NonExistent",
            apiType = APIType.OpenAICompatible,
            hasVisualCapability = false,
            modelProviderConfig = "{}"
        )
        repository.updateProvider(provider)
        Assert.assertEquals(0, repository.listProvider().size)
    }

    @Test
    fun addMultipleProviders_shouldGenerateUniqueIds() {
        repository.addProvider(createProvider(name = "Provider1"))
        repository.addProvider(createProvider(name = "Provider2"))
        repository.addProvider(createProvider(name = "Provider3"))

        val providers = repository.listProvider()
        Assert.assertEquals(3, providers.size)
        val ids = providers.map { it.id }.toSet()
        Assert.assertEquals(3, ids.size)
    }

    @Test
    fun deleteAndAdd_shouldGenerateNewId() {
        repository.addProvider(createProvider(name = "First"))
        val firstId = repository.listProvider()[0].id
        repository.deleteProvider(firstId)

        repository.addProvider(createProvider(name = "Second"))
        val providers = repository.listProvider()
        Assert.assertEquals(1, providers.size)
        Assert.assertTrue(providers[0].id > firstId)
    }

    @Test
    fun listProvider_shouldReturnIndependentCopy() {
        repository.addProvider(createProvider(name = "Provider1"))
        val list1 = repository.listProvider()
        repository.addProvider(createProvider(name = "Provider2"))
        val list2 = repository.listProvider()

        Assert.assertEquals(1, list1.size)
        Assert.assertEquals(2, list2.size)
    }
}
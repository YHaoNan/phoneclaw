package top.yudoge.phoneclaw.llm.integration.openai

import org.junit.Assert.*
import org.junit.Test
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType

class OpenAIModelProviderTest {
    
    @Test
    fun `fromConfig creates provider with correct config`() {
        val configJson = """
            {
                "base_url": "https://api.custom.com",
                "api_key": "test-key",
                "chat_completion_url": "/v1/chat",
                "models_url": "/v1/models",
                "response_api_enabled": true,
                "response_url": "/v1/responses",
                "connect_timeout_millis": 10000,
                "request_timeout_millis": 120000
            }
        """.trimIndent()
        
        val provider = OpenAIModelProvider.fromConfig(1L, "Test", configJson)
        
        assertEquals(1L, provider.id)
        assertEquals("Test", provider.name)
        assertEquals(ProviderType.OpenAICompatible, provider.providerType)
        assertTrue(provider.supportAutoFetchModelList())
    }
    
    @Test
    fun `parseToConfig returns valid json`() {
        val config = OpenAIModelConfig(
            baseUrl = "https://api.test.com",
            apiKey = "test-api-key",
            responseApiEnabled = true
        )
        val provider = OpenAIModelProvider(1L, "Test", config)
        
        val json = provider.parseToConfig()
        
        assertTrue(json.contains("\"base_url\":\"https://api.test.com\""))
        assertTrue(json.contains("\"api_key\":\"test-api-key\""))
        assertTrue(json.contains("\"response_api_enabled\":true"))
    }
    
    @Test
    fun `OpenAIModelConfig toJson and fromJson are symmetrical`() {
        val original = OpenAIModelConfig(
            baseUrl = "https://api.test.com",
            apiKey = "key-123",
            chatCompletionUrl = "/v1/chat",
            modelsUrl = "/v1/models",
            responseApiEnabled = true,
            responseUrl = "/v1/responses",
            connectTimeoutMillis = 10000,
            requestTimeoutMillis = 120000
        )
        
        val json = original.toJson()
        val parsed = OpenAIModelConfig.fromJson(json)
        
        assertEquals(original.baseUrl, parsed.baseUrl)
        assertEquals(original.apiKey, parsed.apiKey)
        assertEquals(original.chatCompletionUrl, parsed.chatCompletionUrl)
        assertEquals(original.modelsUrl, parsed.modelsUrl)
        assertEquals(original.responseApiEnabled, parsed.responseApiEnabled)
        assertEquals(original.responseUrl, parsed.responseUrl)
        assertEquals(original.connectTimeoutMillis, parsed.connectTimeoutMillis)
        assertEquals(original.requestTimeoutMillis, parsed.requestTimeoutMillis)
    }
    
    @Test
    fun `OpenAIModelConfig uses defaults for missing fields`() {
        val json = """{"base_url": "https://api.openai.com"}"""
        
        val config = OpenAIModelConfig.fromJson(json)
        
        assertEquals("https://api.openai.com", config.baseUrl)
        assertEquals("", config.apiKey)
        assertEquals(OpenAIModelConfig.DEFAULT_CHAT_COMPLETION_URL, config.chatCompletionUrl)
        assertEquals(OpenAIModelConfig.DEFAULT_MODELS_URL, config.modelsUrl)
        assertFalse(config.responseApiEnabled)
        assertEquals(OpenAIModelConfig.DEFAULT_RESPONSE_URL, config.responseUrl)
        assertEquals(OpenAIModelConfig.DEFAULT_CONNECT_TIMEOUT, config.connectTimeoutMillis)
        assertEquals(OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT, config.requestTimeoutMillis)
    }
}

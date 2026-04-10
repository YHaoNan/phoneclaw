package top.yudoge.phoneclaw.ai.koog.test

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeDoNothing
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.ktor.utils.io.core.Input
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

class StrategyTest {

    private lateinit var llmClient: LLMClient
    private lateinit var llmModel: LLModel
    private val APIKEY = ""
    private val BASE_URL = ""

    private val MODEL_NAME = ""

    @OptIn(ExperimentalTime::class)
    @Before
    fun before() {

        llmClient = OpenAILLMClient(
            apiKey = APIKEY,
            settings = OpenAIClientSettings(baseUrl = BASE_URL)
        )

        llmModel = LLModel(
            provider = LLMProvider.OpenAI,
            id = MODEL_NAME,
            capabilities = listOf(
                LLMCapability.OpenAIEndpoint.Completions,
                LLMCapability.Temperature,
                LLMCapability.Tools,
                LLMCapability.ToolChoice,
                LLMCapability.Schema.JSON.Standard,
                LLMCapability.Completion
            ),
            contextLength = 128000,
            maxOutputTokens = 4096
        )
    }


    @AIAgentBuilderDslMarker
    inline fun <reified T> nodeAddSuffix(
        name: String? = null
    ): AIAgentNodeDelegate<T, String> =
        node(name) { input -> input.toString() + "cnm" }


    fun getStrategy() = strategy<String, String>("doNothingStrategy") {
        val nodeAddSuffix by nodeAddSuffix<String>()
        edge(nodeStart forwardTo nodeAddSuffix)
        edge(nodeAddSuffix forwardTo nodeFinish)
    }

    @Test
    fun testStrategy(){
        val agent = AIAgent(
            promptExecutor = MultiLLMPromptExecutor(
                mapOf(
                    llmClient.llmProvider() to llmClient
                )
            ),
            llmModel = llmModel,
            strategy = getStrategy()
        )

        runBlocking {
            var result = agent.run("hello")
            println(result)
        }

    }

}
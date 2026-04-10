package top.yudoge.phoneclaw

import dev.langchain4j.model.openai.OpenAiChatModel
import org.junit.Test

class Langchain4jTest {

    @Test
    fun testOpenAICompatibleApi() {
        val model = OpenAiChatModel.builder()
            .apiKey("5a1ed57e1bfb40b1ad1a9e49ff567296.QOGuxtGVKezNK3Hv")
            .baseUrl("https://open.bigmodel.cn/api/paas/v4")
            .modelName("GLM-4-Flash-250414")
            .temperature(0.7)
            .build()

        var chat = model.chat("hello")
        println(chat)
    }
}
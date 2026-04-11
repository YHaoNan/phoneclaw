package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

interface ModelInitializer {
    fun validate(provider: ModelProvider)
}

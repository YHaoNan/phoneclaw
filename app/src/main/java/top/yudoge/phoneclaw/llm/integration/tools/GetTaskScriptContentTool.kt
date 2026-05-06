package top.yudoge.phoneclaw.llm.integration.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import top.yudoge.phoneclaw.app.AppContainer

class GetTaskScriptContentTool {

    @Tool(
        name = "getTaskScriptContent",
        value = ["Get full content of a saved task script by id or name so the model can inspect and improve it."]
    )
    fun getTaskScriptContent(
        @P("Task script id (preferred when known)")
        scriptId: String?,
        @P("Task script name (used when id is not provided)")
        scriptName: String?
    ): String {
        val facade = AppContainer.getInstance().taskScriptFacade
        val script = when {
            !scriptId.isNullOrBlank() -> facade.getScriptById(scriptId.trim())
            !scriptName.isNullOrBlank() -> facade.getScriptByName(scriptName.trim())
            else -> null
        } ?: return "Script not found. Provide scriptId or scriptName."

        return buildString {
            appendLine("Task script:")
            appendLine("id=${script.id}")
            appendLine("name=${script.name}")
            appendLine("summary=${script.summary}")
            appendLine("createdTime=${script.createdTime}")
            appendLine("content:")
            append(script.codeContent)
        }.trim()
    }
}


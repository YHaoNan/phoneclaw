package top.yudoge.phoneclaw.llm.integration.tools

import dev.langchain4j.agent.tool.Tool
import top.yudoge.phoneclaw.app.AppContainer

class ListTaskScriptsTool {

    @Tool(
        name = "listTaskScripts",
        value = ["List available reusable task scripts with metadata for tool-based script selection."]
    )
    fun listTaskScripts(): String {
        val scripts = AppContainer.getInstance().taskScriptFacade.getAllScripts()
        if (scripts.isEmpty()) {
            return "No saved task scripts are available."
        }
        return buildString {
            appendLine("Available task scripts:")
            scripts.forEach { script ->
                appendLine("- id=${script.id}, name=${script.name}, summary=${script.summary}, createdTime=${script.createdTime}")
            }
        }.trim()
    }
}

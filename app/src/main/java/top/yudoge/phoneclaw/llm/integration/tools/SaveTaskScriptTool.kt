package top.yudoge.phoneclaw.llm.integration.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import top.yudoge.phoneclaw.app.AppContainer

class SaveTaskScriptTool {

    @Tool(
        name = "saveTaskScript",
        value = ["Create a new task script or update an existing one. This is the canonical way for the model to maintain scripts."]
    )
    fun saveTaskScript(
        @P("Task script name")
        name: String,
        @P("Task script summary")
        summary: String,
        @P("Task script content")
        codeContent: String
    ): String {
        val facade = AppContainer.getInstance().taskScriptFacade
        val existing = facade.getScriptByName(name.trim())
        val updated = if (existing != null) {
            facade.updateScript(existing.id, name, summary, codeContent)
        } else {
            facade.createScript(name, summary, codeContent)
        }

        if (!updated) {
            return "Save failed: could not create or update script."
        }

        val saved = facade.getScriptByName(name.trim())
            ?: return "Save succeeded but cannot read back the script."

        return "Script saved: id=${saved.id}, name=${saved.name}, summary=${saved.summary}"
    }
}

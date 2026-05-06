package top.yudoge.phoneclaw.llm.integration.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.scripts.domain.EvalListener
import top.yudoge.phoneclaw.scripts.domain.impl.LuaScriptEngine
import top.yudoge.phoneclaw.scripts.domain.objects.EvalResult

class ExecuteTaskScriptTool {

    private val scriptEngine = LuaScriptEngine(1)

    @Tool(
        name = "executeTaskScript",
        value = ["Execute a saved task script by id. Execution is fail-fast and returns explicit error details."]
    )
    fun executeTaskScript(
        @P("Task script id to execute")
        scriptId: String
    ): String {
        val script = AppContainer.getInstance().taskScriptFacade.getScriptById(scriptId)
            ?: return "Script not found: $scriptId"
        if (script.codeContent.isBlank()) {
            return "Script execution failed: script content is empty"
        }

        val handle = scriptEngine.newEval(script.codeContent)
        handle.inject("emu", AppContainer.getInstance().luaFriendlyEmuFacadeProxy)

        val done = CountDownLatch(1)
        var result: EvalResult? = null
        val logs = mutableListOf<String>()
        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {
                logs.addAll(lines)
            }

            override fun onFinished(evalId: String, evalResult: EvalResult) {
                result = evalResult
                done.countDown()
            }
        }, 15_000)

        if (!done.await(45, TimeUnit.SECONDS)) {
            handle.interruptEval()
            return "Script execution failed: timeout waiting for completion"
        }

        val evalResult = result ?: return "Script execution failed: no result"
        if (!evalResult.success) {
            val logText = if (logs.isEmpty()) "" else "\nLogs:\n${logs.joinToString("\n")}"
            return "Script execution failed: ${evalResult.error}$logText"
        }
        val output = evalResult.evalResult
        return buildString {
            append("Script executed successfully")
            if (output.isNotBlank()) {
                append(": ").append(output)
            }
            if (logs.isNotEmpty()) {
                append("\nLogs:\n").append(logs.joinToString("\n"))
            }
        }
    }
}

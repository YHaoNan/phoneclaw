package top.yudoge.phoneclaw.llm.integration.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.scripts.domain.EvalHandle
import top.yudoge.phoneclaw.scripts.domain.EvalListener
import top.yudoge.phoneclaw.scripts.domain.objects.EvalResult
import top.yudoge.phoneclaw.scripts.domain.ScriptEngine
import top.yudoge.phoneclaw.scripts.domain.impl.LuaScriptEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PhoneEmulationTool {

    private val scriptEngine: ScriptEngine = LuaScriptEngine()

    @Tool(
        name = "executeScript",
        value = ["Execute a Lua script on the phone. The 'emu' object is automatically available with phone automation APIs."]
    )
    fun executeScript(
        @P("Lua script to execute. Available APIs: emu:openApp(pkg), emu:clickById(id), emu:clickByPos(x,y), emu:swipe(x1,y1,x2,y2,ms), emu:inputTextById(id,text), emu:getCurrentWindowByAccessibilityService(depth,pkg,pattern,clickable,longClickable,scrollable,editable,checkable), emu:waitWindowOpened(pkg,activity,timeout), emu:waitMS(ms), emu:back(), emu:home(), emu:getInstalledApps(filterPattern). IMPORTANT: Always check return values - most methods return nil/false on failure. Check if result is nil before using.")
        script: String
    ): String {
        return executeScriptSync(script)
    }

    private fun executeScriptSync(script: String): String {
        val result = StringBuilder()
        val error = StringBuilder()
        val handle: EvalHandle = scriptEngine.newEval(script)
        val latch = CountDownLatch(1)

        handle.inject("emu", AppContainer.getInstance().luaFriendlyEmuFacadeProxy)

        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) {
                for (line in lines) {
                    result.append(line).append("\n")
                }
            }

            override fun onFinished(evalId: String, evalResult: EvalResult) {
                if (!evalResult.success) {
                    error.append(evalResult.error)
                }
                latch.countDown()
            }
        }, 60000)

        latch.await(65000, TimeUnit.MILLISECONDS)

        return if (error.isNotEmpty()) {
            "Error: $error"
        } else {
            result.toString().trim()
        }
    }
}

package top.yudoge.phoneclaw.llm.tools

import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import top.yudoge.phoneclaw.scripts.EvalHandle
import top.yudoge.phoneclaw.scripts.EvalListener
import top.yudoge.phoneclaw.scripts.EvalResult
import top.yudoge.phoneclaw.scripts.ScriptEngine
import top.yudoge.phoneclaw.scripts.impl.LuaScriptEngine

@LLMDescription("Tools for controlling Android phone via Lua scripts")
class PhoneEmulationTool : ToolSet {

    private val scriptEngine: ScriptEngine = LuaScriptEngine()

    @Tool
    @LLMDescription("Execute a Lua script on the phone. The 'emu' object is automatically available with phone automation APIs.")
    fun executeScript(
        @LLMDescription("Lua script to execute. Available APIs: emu:openApp(pkg), emu:clickById(id), emu:clickByPos(x,y), emu:swipe(x1,y1,x2,y2,ms), emu:inputTextById(id,text), emu:getCurrentWindowByAccessibilityService(depth,pkg,pattern,clickable,longClickable,scrollable,editable,checkable), emu:waitWindowOpened(pkg,activity,timeout), emu:waitMS(ms), emu:back(), emu:home()")
        script: String
    ): String {
        return executeScriptSync(script)
    }

    @Tool
    @LLMDescription("Get current screen UI structure from the phone")
    fun getCurrentScreen(
        @LLMDescription("Maximum depth to traverse UI tree (use 50 for full)")
        maxDepth: Int = 50
    ): String {
        return executeScriptSync("local ui = emu:getCurrentWindowByAccessibilityService($maxDepth, nil, nil)\nif ui then print(ui) else print('nil') end")
    }

    @Tool
    @LLMDescription("Open an app on the phone by package name")
    fun openApp(
        @LLMDescription("Package name of the app to open, e.g., com.tencent.mm for WeChat")
        packageName: String
    ): String {
        return executeScriptSync("""
            emu:openApp("$packageName")
            local window = emu:waitWindowOpened("$packageName", nil, 5000)
            if window then
                print("App opened successfully")
            else
                print("Timeout waiting for app to open")
            end
        """.trimIndent())
    }

    @Tool
    @LLMDescription("Tap on the screen at specified coordinates")
    fun tap(
        @LLMDescription("X coordinate")
        x: Double,
        @LLMDescription("Y coordinate")
        y: Double
    ): String {
        return executeScriptSync("emu:clickByPos($x, $y)\nprint('Tapped at ($x, $y)')")
    }

    @Tool
    @LLMDescription("Perform a swipe gesture on the screen")
    fun swipe(
        @LLMDescription("Starting X coordinate")
        fromX: Double,
        @LLMDescription("Starting Y coordinate")
        fromY: Double,
        @LLMDescription("Ending X coordinate")
        toX: Double,
        @LLMDescription("Ending Y coordinate")
        toY: Double,
        @LLMDescription("Duration of swipe in milliseconds")
        durationMs: Int = 300
    ): String {
        return executeScriptSync("emu:swipe($fromX, $fromY, $toX, $toY, $durationMs)\nprint('Swiped')")
    }

    @Tool
    @LLMDescription("Input text into an editable field")
    fun inputText(
        @LLMDescription("Resource ID of the input field")
        resourceId: String,
        @LLMDescription("Text to input")
        text: String
    ): String {
        val escapedText = text.replace("\"", "\\\"").replace("\n", "\\n")
        return executeScriptSync("""
            local success = emu:inputTextById("$resourceId", "$escapedText")
            if success then print("Text input successful") else print("Text input failed") end
        """.trimIndent())
    }

    @Tool
    @LLMDescription("Find UI elements matching a pattern")
    fun findElements(
        @LLMDescription("Regex pattern to match text/ID/description")
        pattern: String,
        @LLMDescription("Whether to only find clickable elements")
        clickableOnly: Boolean = false,
        @LLMDescription("Maximum depth to search")
        maxDepth: Int = 50
    ): String {
        return executeScriptSync("""
            local ui = emu:getCurrentWindowByAccessibilityService($maxDepth, nil, "$pattern", $clickableOnly, false, false, false, false)
            if ui then
                local nodes = ui:getMatchedNodes()
                if nodes and nodes:size() > 0 then
                    print("Found " .. nodes:size() .. " elements")
                    for i = 0, nodes:size() - 1 do
                        local node = nodes:get(i)
                        print(node:getId() or "no-id")
                    end
                else
                    print("No matching elements")
                end
            else
                print("Failed to get UI")
            end
        """.trimIndent())
    }

    @Tool
    @LLMDescription("Press back button")
    fun pressBack(): String {
        return executeScriptSync("emu:back()\nprint('Pressed back')")
    }

    @Tool
    @LLMDescription("Press home button")
    fun pressHome(): String {
        return executeScriptSync("emu:home()\nprint('Pressed home')")
    }

    private fun executeScriptSync(script: String): String {
        val result = StringBuilder()
        val error = StringBuilder()
        val handle: EvalHandle = scriptEngine.newEval(script)
        
        handle.inject("emu", top.yudoge.phoneclaw.emu.EmuApi())
        
        synchronized(handle) {
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
                    (handle as Object).notifyAll()
                }
            }, 60000)
            
            (handle as Object).wait(65000)
        }
        
        return if (error.isNotEmpty()) {
            "Error: $error"
        } else {
            result.toString().trim()
        }
    }
}

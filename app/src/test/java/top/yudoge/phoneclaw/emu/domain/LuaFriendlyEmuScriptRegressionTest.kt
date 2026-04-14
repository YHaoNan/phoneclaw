package top.yudoge.phoneclaw.emu.domain

import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yudoge.phoneclaw.emu.domain.objects.AppInfo
import top.yudoge.phoneclaw.emu.domain.objects.UITree
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow
import top.yudoge.phoneclaw.scripts.domain.EvalListener
import top.yudoge.phoneclaw.scripts.domain.impl.LuaScriptEngine
import top.yudoge.phoneclaw.scripts.domain.objects.EvalResult
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class LuaFriendlyEmuScriptRegressionTest {

    private val engine = LuaScriptEngine()

    @Test
    fun `app listing script can use lua length and index access`() {
        val proxy = LuaFriendlyEmuFacadeProxy(FakeEmuFacadeContract())
        val result = runScript(
            """
            local apps = emu:getInstalledApps("抖音")
            if apps == nil then return "nil" end
            return tostring(#apps) .. ":" .. apps[1].packageName .. ":" .. apps[1].appName
            """.trimIndent(),
            proxy
        )

        assertTrue(result.error, result.success)
        assertEquals("1:com.ss.android.ugc.aweme:抖音", result.evalResult)
    }

    @Test
    fun `app listing table provides pretty method`() {
        val proxy = LuaFriendlyEmuFacadeProxy(FakeEmuFacadeContract())
        val result = runScript(
            """
            local apps = emu:getInstalledApps("抖音")
            if apps == nil then return "nil" end
            apps:pretty()
            return "ok"
            """.trimIndent(),
            proxy
        )

        assertTrue(result.error, result.success)
        assertEquals("ok", result.evalResult)
    }

    @Test
    fun `open app script keeps operation semantics`() {
        val proxy = LuaFriendlyEmuFacadeProxy(FakeEmuFacadeContract())
        val result = runScript(
            """
            local ok = emu:openApp("com.ss.android.ugc.aweme")
            if ok then return "opened" else return "failed" end
            """.trimIndent(),
            proxy
        )

        assertTrue(result.success)
        assertEquals("opened", result.evalResult)
    }

    @Test
    fun `ui query script can read nested table fields`() {
        val proxy = LuaFriendlyEmuFacadeProxy(FakeEmuFacadeContract())
        val result = runScript(
            """
            local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "消息", false, false, false, false, false)
            if ui == nil then return "nil" end
            local node = ui.matchedNodes[1]
            return ui.packageName .. "|" .. node.text .. "|" .. tostring(node.bounds.left)
            """.trimIndent(),
            proxy
        )

        assertTrue(result.success)
        assertEquals("com.ss.android.ugc.aweme|消息|100", result.evalResult)
    }

    @Test
    fun `ui query exposes isIdUnique for clickById decision`() {
        val proxy = LuaFriendlyEmuFacadeProxy(FakeEmuFacadeContract())
        val result = runScript(
            """
            local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "消息", false, false, false, false, false)
            if ui == nil or #ui.matchedNodes == 0 then return "missing" end
            local node = ui.matchedNodes[1]
            return tostring(node.isIdUnique)
            """.trimIndent(),
            proxy
        )

        assertTrue(result.success)
        assertEquals("true", result.evalResult)
    }

    @Test
    fun `ui window pretty method is provided by lua model layer`() {
        val proxy = LuaFriendlyEmuFacadeProxy(FakeEmuFacadeContract())
        val result = runScript(
            """
            local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "消息", false, false, false, false, false)
            if ui == nil then return "nil" end
            ui:pretty()
            return "ok"
            """.trimIndent(),
            proxy
        )

        assertTrue(result.success)
        assertEquals("ok", result.evalResult)
    }

    private fun runScript(script: String, emuProxy: LuaFriendlyEmuFacadeProxy): EvalResult {
        val bootstrap = listOf(
            File("app/src/main/assets/lua/emu_pretty.lua"),
            File("src/main/assets/lua/emu_pretty.lua")
        ).firstOrNull { it.exists() }?.readText(Charsets.UTF_8).orEmpty()
        val fullScript = bootstrap + "\n" + script
        val handle = engine.newEval(fullScript)
        val resultRef = AtomicReference<EvalResult>()
        val latch = CountDownLatch(1)

        handle.inject("emu", emuProxy)
        handle.eval(object : EvalListener {
            override fun onLogAppended(evalId: String, lines: List<String>) = Unit

            override fun onFinished(evalId: String, result: EvalResult) {
                resultRef.set(result)
                latch.countDown()
            }
        }, 5000)

        assertTrue(latch.await(7000, TimeUnit.MILLISECONDS))
        assertNotNull(resultRef.get())
        return resultRef.get()
    }

    private class FakeEmuFacadeContract : EmuFacadeContract {
        override var screenReadMode: ScreenReadMode = ScreenReadMode.ACCESSIBILITY

        override fun openApp(packageName: String): Boolean = packageName == "com.ss.android.ugc.aweme"

        override fun waitWindowOpened(
            packageName: String?,
            activityName: String?,
            timeoutMs: Long
        ): AccessibilityWindowInfo? = null

        override fun waitMS(milliseconds: Long) = Unit

        override fun back(): Boolean = true

        override fun home(): Boolean = true

        override fun getCurrentWindowByAccessibilityService(
            maxDepth: Int,
            windowPackageName: String?,
            filterPattern: String?,
            requireClickable: Boolean,
            requireLongClickable: Boolean,
            requireScrollable: Boolean,
            requireEditable: Boolean,
            requireCheckable: Boolean
        ): UIWindow {
            val node = UITree(
                id = "com.ss.android.ugc.aweme:id/msg",
                isIdUnique = true,
                className = "android.widget.TextView",
                text = "消息",
                bounds = Rect(100, 200, 300, 400)
            )
            return UIWindow(
                packageName = "com.ss.android.ugc.aweme",
                activityName = "MainActivity",
                matchedNodes = listOf(node)
            )
        }

        override fun clickById(id: String): Boolean = true

        override fun clickByPos(x: Double, y: Double): Boolean = true

        override fun longClickById(id: String, durationMs: Long): Boolean = true

        override fun longClickByPos(x: Double, y: Double, durationMs: Long): Boolean = true

        override fun swipe(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long): Boolean = true

        override fun inputTextById(id: String, text: String): Boolean = true

        override fun inputTextByPos(x: Double, y: Double, text: String): Boolean = true

        override fun getInstalledApps(filterPattern: String?): List<AppInfo> {
            return listOf(AppInfo(packageName = "com.ss.android.ugc.aweme", appName = "抖音"))
        }
    }
}

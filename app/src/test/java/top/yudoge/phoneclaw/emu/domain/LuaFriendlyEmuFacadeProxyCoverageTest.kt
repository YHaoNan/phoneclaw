package top.yudoge.phoneclaw.emu.domain

import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yudoge.phoneclaw.emu.domain.objects.AppInfo
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow

class LuaFriendlyEmuFacadeProxyCoverageTest {

    @Test
    fun `proxy exposes all script-facing facade methods`() {
        val requiredMethods = setOf(
            "openApp",
            "waitWindowOpened",
            "waitMS",
            "back",
            "home",
            "getCurrentWindowByAccessibilityService",
            "clickById",
            "clickByPos",
            "longClickById",
            "longClickByPos",
            "swipe",
            "inputTextById",
            "inputTextByPos",
            "getInstalledApps"
        )

        val proxyMethods = LuaFriendlyEmuFacadeProxy::class.java.methods.map { it.name }.toSet()
        val contractMethods = EmuFacadeContract::class.java.methods.map { it.name }.toSet()

        requiredMethods.forEach { method ->
            assertTrue("Proxy should expose method: $method", proxyMethods.contains(method))
            assertTrue("Facade contract should expose method: $method", contractMethods.contains(method))
        }
    }

    @Test
    fun `proxy delegates action methods and preserves behavior semantics`() {
        val fakeFacade = FakeEmuFacadeContract()
        val proxy = LuaFriendlyEmuFacadeProxy(fakeFacade)

        val opened = proxy.openApp("com.demo")
        val clicked = proxy.clickById("node-id")
        val apps = proxy.getInstalledApps("demo")

        assertTrue(opened)
        assertTrue(clicked)
        assertEquals("com.demo", fakeFacade.lastOpenAppPackage)
        assertEquals("node-id", fakeFacade.lastClickId)
        assertEquals("com.demo", apps.get(1).get("packageName").tojstring())
    }

    private class FakeEmuFacadeContract : EmuFacadeContract {
        override var screenReadMode: ScreenReadMode = ScreenReadMode.ACCESSIBILITY

        var lastOpenAppPackage: String? = null
        var lastClickId: String? = null

        override fun openApp(packageName: String): Boolean {
            lastOpenAppPackage = packageName
            return true
        }

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
        ): UIWindow? = null

        override fun clickById(id: String): Boolean {
            lastClickId = id
            return true
        }

        override fun clickByPos(x: Double, y: Double): Boolean = true

        override fun longClickById(id: String, durationMs: Long): Boolean = true

        override fun longClickByPos(x: Double, y: Double, durationMs: Long): Boolean = true

        override fun swipe(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long): Boolean = true

        override fun inputTextById(id: String, text: String): Boolean = true

        override fun inputTextByPos(x: Double, y: Double, text: String): Boolean = true

        override fun getInstalledApps(filterPattern: String?): List<AppInfo>? {
            return listOf(AppInfo(packageName = "com.demo", appName = "Demo"))
        }
    }
}

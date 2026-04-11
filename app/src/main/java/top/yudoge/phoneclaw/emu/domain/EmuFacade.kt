package top.yudoge.phoneclaw.emu.domain

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import top.yudoge.phoneclaw.emu.domain.objects.AppInfo
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow

enum class ScreenReadMode {
    ACCESSIBILITY,
    VLM
}

class EmuFacade(
    private val accessibilityReader: EmuAccessibilityScreenReader,
    private val vlmReader: EmuVLMScreenReader,
    private val operator: EmuAccessibilityScreenOperator,
    private val serviceProvider: () -> EmuAccessibilityServiceInterface?
) {
    var screenReadMode: ScreenReadMode = ScreenReadMode.ACCESSIBILITY
    
    fun getCurrentScreen(
        maxDepth: Int = 50,
        windowPackageName: String? = null,
        filterPattern: String? = null,
        requireClickable: Boolean = false,
        requireLongClickable: Boolean = false,
        requireScrollable: Boolean = false,
        requireEditable: Boolean = false,
        requireCheckable: Boolean = false
    ): UIWindow? {
        return when (screenReadMode) {
            ScreenReadMode.ACCESSIBILITY -> accessibilityReader.readScreen(
                maxDepth, windowPackageName, filterPattern,
                requireClickable, requireLongClickable, requireScrollable,
                requireEditable, requireCheckable
            )
            ScreenReadMode.VLM -> vlmReader.analyzeScreen(filterPattern ?: "")
        }
    }
    
    fun clickById(id: String): Boolean {
        val node = accessibilityReader.findNodeById(id) ?: return false
        val result = operator.click(node)
        node.recycle()
        return result
    }
    
    fun clickAtPosition(x: Double, y: Double, durationMs: Long = 100): Boolean {
        return operator.clickAtPosition(x.toInt(), y.toInt(), durationMs)
    }
    
    fun longClickById(id: String, durationMs: Long = 500): Boolean {
        val node = accessibilityReader.findNodeById(id) ?: return false
        val result = operator.longClick(node, durationMs)
        node.recycle()
        return result
    }
    
    fun longClickAtPosition(x: Double, y: Double, durationMs: Long = 500): Boolean {
        return operator.clickAtPosition(x.toInt(), y.toInt(), durationMs)
    }
    
    fun swipe(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long): Boolean {
        return operator.swipe(fromX.toInt(), fromY.toInt(), toX.toInt(), toY.toInt(), durationMs)
    }
    
    fun inputTextById(id: String, text: String): Boolean {
        val node = accessibilityReader.findNodeById(id) ?: return false
        val result = operator.inputText(node, text)
        node.recycle()
        return result
    }
    
    fun inputTextAtPosition(x: Double, y: Double, text: String): Boolean {
        return operator.inputTextAtPosition(x.toInt(), y.toInt(), text)
    }
    
    fun back(): Boolean {
        return operator.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    fun home(): Boolean {
        return operator.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
    
    fun openApp(packageName: String): Boolean {
        return operator.openApp(packageName)
    }
    
    fun waitWindowOpened(packageName: String?, activityName: String?, timeoutMs: Long): AccessibilityWindowInfo? {
        val service = serviceProvider() ?: return null
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val window = service.findWindow(packageName, activityName)
            if (window != null) return window
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }
    
    fun getInstalledApps(filterPattern: String?): List<AppInfo>? {
        val service = serviceProvider() ?: return null
        return service.getInstalledApps(filterPattern)
    }
    
    companion object {
        private const val POLL_INTERVAL_MS = 1000L
    }
}

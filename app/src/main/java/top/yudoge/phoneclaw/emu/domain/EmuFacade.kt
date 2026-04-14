package top.yudoge.phoneclaw.emu.domain

import android.accessibilityservice.AccessibilityService
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
) : EmuFacadeContract {
    override var screenReadMode: ScreenReadMode = ScreenReadMode.ACCESSIBILITY
    
    // ==================== Navigation & Waiting ====================
    
    override fun openApp(packageName: String): Boolean {
        return operator.openApp(packageName)
    }
    
    override fun waitWindowOpened(packageName: String?, activityName: String?, timeoutMs: Long): AccessibilityWindowInfo? {
        val service = serviceProvider() ?: return null
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val window = service.findWindow(packageName, activityName)
            if (window != null) return window
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }
    
    override fun waitMS(milliseconds: Long) {
        Thread.sleep(milliseconds)
    }
    
    override fun back(): Boolean {
        return operator.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    override fun home(): Boolean {
        return operator.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
    
    // ==================== Screen Analysis ====================
    
    override fun getCurrentWindowByAccessibilityService(
        maxDepth: Int,
        windowPackageName: String?,
        filterPattern: String?,
        requireClickable: Boolean,
        requireLongClickable: Boolean,
        requireScrollable: Boolean,
        requireEditable: Boolean,
        requireCheckable: Boolean
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
    
    // ==================== Click Operations ====================
    
    override fun clickById(id: String): Boolean {
        val node = accessibilityReader.findNodeById(id) ?: return false
        val result = operator.click(node)
        node.recycle()
        return result
    }
    
    override fun clickByPos(x: Double, y: Double): Boolean {
        return operator.clickAtPosition(x.toInt(), y.toInt(), 100)
    }
    
    override fun longClickById(id: String, durationMs: Long): Boolean {
        val node = accessibilityReader.findNodeById(id) ?: return false
        val result = operator.longClick(node, durationMs)
        node.recycle()
        return result
    }
    
    override fun longClickByPos(x: Double, y: Double, durationMs: Long): Boolean {
        return operator.clickAtPosition(x.toInt(), y.toInt(), durationMs)
    }
    
    // ==================== Gesture Operations ====================
    
    override fun swipe(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long): Boolean {
        return operator.swipe(fromX.toInt(), fromY.toInt(), toX.toInt(), toY.toInt(), durationMs)
    }
    
    // ==================== Text Input ====================
    
    override fun inputTextById(id: String, text: String): Boolean {
        val node = accessibilityReader.findNodeById(id) ?: return false
        val result = operator.inputText(node, text)
        node.recycle()
        return result
    }
    
    override fun inputTextByPos(x: Double, y: Double, text: String): Boolean {
        return operator.inputTextAtPosition(x.toInt(), y.toInt(), text)
    }
    
    // ==================== App Management ====================
    
    override fun getInstalledApps(filterPattern: String?): List<AppInfo>? {
        val service = serviceProvider() ?: return null
        return service.getInstalledApps(filterPattern)
    }
    
    companion object {
        private const val POLL_INTERVAL_MS = 1000L
    }
}

package top.yudoge.phoneclaw.emu.domain

import android.view.accessibility.AccessibilityNodeInfo

class EmuAccessibilityScreenOperator(
    private val serviceProvider: () -> EmuAccessibilityServiceInterface?
) {
    fun click(node: AccessibilityNodeInfo): Boolean {
        val service = serviceProvider() ?: return false
        return service.performClick(node)
    }
    
    fun clickAtPosition(x: Int, y: Int, durationMs: Long = 100): Boolean {
        val service = serviceProvider() ?: return false
        return service.performGestureClick(x, y, durationMs)
    }
    
    fun longClick(node: AccessibilityNodeInfo, durationMs: Long = 500): Boolean {
        val service = serviceProvider() ?: return false
        return service.performLongClick(node, durationMs)
    }
    
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): Boolean {
        val service = serviceProvider() ?: return false
        return service.performGestureSwipe(x1, y1, x2, y2, durationMs)
    }
    
    fun inputText(node: AccessibilityNodeInfo, text: String): Boolean {
        val service = serviceProvider() ?: return false
        return service.inputText(node, text)
    }
    
    fun inputTextAtPosition(x: Int, y: Int, text: String): Boolean {
        val service = serviceProvider() ?: return false
        return service.inputTextByPos(x, y, text)
    }
    
    fun performGlobalAction(action: Int): Boolean {
        val service = serviceProvider() ?: return false
        return service.performGlobalAction(action)
    }
    
    fun openApp(packageName: String): Boolean {
        val service = serviceProvider() ?: return false
        return service.openApp(packageName)
    }
}

package top.yudoge.phoneclaw.emu.domain

import android.view.accessibility.AccessibilityNodeInfo
import top.yudoge.phoneclaw.emu.domain.objects.UITree
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow
import java.util.regex.Pattern

class EmuAccessibilityScreenReader(
    private val serviceProvider: () -> EmuAccessibilityServiceInterface?
) {
    fun readScreen(
        maxDepth: Int = 50,
        windowPackageName: String? = null,
        filterPattern: String? = null,
        requireClickable: Boolean = false,
        requireLongClickable: Boolean = false,
        requireScrollable: Boolean = false,
        requireEditable: Boolean = false,
        requireCheckable: Boolean = false
    ): UIWindow? {
        val service = serviceProvider() ?: return null
        val rootNode = service.getTargetWindowRoot(windowPackageName) ?: return null

        val packageName = rootNode.packageName?.toString()
        val idCounts = collectIdCounts(rootNode)

        val uiWindow = if (filterPattern != null && filterPattern.isNotEmpty()) {
            val pattern = Pattern.compile(filterPattern)
            val matchedNodes = service.findNodesByPatternWithFilter(
                rootNode, pattern, maxDepth, 0,
                requireClickable, requireLongClickable, requireScrollable, 
                requireEditable, requireCheckable
            )
            assignIdUniqueness(matchedNodes, idCounts)
            UIWindow(
                packageName = packageName,
                matchedNodes = matchedNodes
            )
        } else {
            val root = service.buildUITree(rootNode, maxDepth, 0)
            root?.let { assignIdUniqueness(it, idCounts) }
            UIWindow(
                packageName = packageName,
                root = root
            )
        }
        
        rootNode.recycle()
        return uiWindow
    }

    private fun collectIdCounts(rootNode: AccessibilityNodeInfo): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(rootNode)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            val id = node.viewIdResourceName
            if (!id.isNullOrEmpty()) {
                counts[id] = (counts[id] ?: 0) + 1
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                stack.add(child)
            }
            if (node !== rootNode) {
                node.recycle()
            }
        }
        return counts
    }

    private fun assignIdUniqueness(nodes: List<UITree>, idCounts: Map<String, Int>) {
        nodes.forEach { assignIdUniqueness(it, idCounts) }
    }

    private fun assignIdUniqueness(node: UITree, idCounts: Map<String, Int>) {
        node.isIdUnique = node.id?.let { idCounts[it] == 1 }
        node.children.forEach { child ->
            assignIdUniqueness(child, idCounts)
        }
    }
    
    fun findNodeById(viewId: String): AccessibilityNodeInfo? {
        val service = serviceProvider() ?: return null
        return service.findNodeById(viewId)
    }
}

interface EmuAccessibilityServiceInterface {
    fun getTargetWindowRoot(packageName: String?): AccessibilityNodeInfo?
    fun findNodesByPatternWithFilter(
        node: AccessibilityNodeInfo,
        pattern: Pattern,
        maxDepth: Int,
        currentDepth: Int,
        requireClickable: Boolean,
        requireLongClickable: Boolean,
        requireScrollable: Boolean,
        requireEditable: Boolean,
        requireCheckable: Boolean
    ): List<UITree>
    fun buildUITree(node: AccessibilityNodeInfo, maxDepth: Int, currentDepth: Int): UITree?
    fun findNodeById(viewId: String): AccessibilityNodeInfo?
    fun findWindow(packageName: String?, activityName: String?): android.view.accessibility.AccessibilityWindowInfo?
    fun getInstalledApps(filterPattern: String?): List<top.yudoge.phoneclaw.emu.domain.objects.AppInfo>?
    
    fun performClick(node: AccessibilityNodeInfo): Boolean
    fun performGestureClick(x: Int, y: Int, durationMs: Long): Boolean
    fun performLongClick(node: AccessibilityNodeInfo, durationMs: Long): Boolean
    fun performGestureSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): Boolean
    fun inputText(node: AccessibilityNodeInfo, text: String): Boolean
    fun inputTextByPos(x: Int, y: Int, text: String): Boolean
    fun performGlobalAction(action: Int): Boolean
    fun openApp(packageName: String): Boolean
}

package top.yudoge.phoneclaw.emu.domain

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityWindowInfo
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import top.yudoge.phoneclaw.emu.domain.objects.AppInfo
import top.yudoge.phoneclaw.emu.domain.objects.UITree
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow

/**
 * Stable conversion contracts for Lua:
 * - AppInfo -> { packageName, appName }
 * - UIWindow -> { packageName, activityName, root, matchedNodes }
 * - UITree -> { id, isIdUnique, className, text, desc, hintText, bounds, clickable, longClickable, scrollable, editable, checkable, checked, children }
 * - Rect -> { left, top, right, bottom }
 *
 * Optional/null values are converted to Lua nil semantics.
 */
class LuaValueConverter {

    fun toLuaValue(value: Any?): LuaValue {
        return try {
            when (value) {
                null -> LuaValue.NIL
                is LuaValue -> value
                is Boolean -> LuaValue.valueOf(value)
                is Int -> LuaValue.valueOf(value)
                is Long -> LuaValue.valueOf(value.toDouble())
                is Float -> LuaValue.valueOf(value.toDouble())
                is Double -> LuaValue.valueOf(value)
                is Short -> LuaValue.valueOf(value.toInt())
                is Byte -> LuaValue.valueOf(value.toInt())
                is String -> LuaValue.valueOf(value)
                is AppInfo -> appInfoToTable(value)
                is UIWindow -> uiWindowToTable(value)
                is UITree -> uiTreeToTable(value)
                is Rect -> rectToTable(value)
                is AccessibilityWindowInfo -> accessibilityWindowInfoToTable(value)
                is List<*> -> {
                    if (value.all { it is AppInfo }) {
                        @Suppress("UNCHECKED_CAST")
                        appInfoListToTable(value as List<AppInfo>)
                    } else {
                        listToTable(value)
                    }
                }
                is Array<*> -> arrayToTable(value)
                is Map<*, *> -> mapToTable(value)
                else -> {
                    Log.w(TAG, "Unsupported Lua conversion for type=${value::class.java.name}, fallback to string.")
                    LuaValue.valueOf(value.toString())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert value to Lua. type=${value?.javaClass?.name}", e)
            LuaValue.NIL
        }
    }

    fun listToTable(list: List<*>): LuaTable {
        val table = LuaTable()
        list.forEachIndexed { index, item ->
            table.set(index + 1, toLuaValue(item))
        }
        return table
    }

    fun arrayToTable(array: Array<*>): LuaTable {
        val table = LuaTable()
        array.forEachIndexed { index, item ->
            table.set(index + 1, toLuaValue(item))
        }
        return table
    }

    fun mapToTable(map: Map<*, *>): LuaTable {
        val table = LuaTable()
        map.forEach { (k, v) ->
            if (k != null) {
                table.set(k.toString(), toLuaValue(v))
            }
        }
        return table
    }

    fun appInfoToTable(appInfo: AppInfo): LuaTable {
        val table = LuaTable()
        table.set("__type", LuaValue.valueOf("AppInfo"))
        table.set("packageName", LuaValue.valueOf(appInfo.packageName))
        table.set("appName", LuaValue.valueOf(appInfo.appName))
        return table
    }

    fun appInfoListToTable(apps: List<AppInfo>): LuaTable {
        val table = LuaTable()
        apps.forEachIndexed { index, appInfo ->
            table.set(index + 1, appInfoToTable(appInfo))
        }
        table.set("pretty", appListPrettyFunction)
        return table
    }

    fun uiWindowToTable(window: UIWindow): LuaTable {
        val table = LuaTable()
        table.set("__type", LuaValue.valueOf("UIWindow"))
        table.set("packageName", toLuaValue(window.packageName))
        table.set("activityName", toLuaValue(window.activityName))
        table.set("root", toLuaValue(window.root))
        table.set("matchedNodes", listToTable(window.matchedNodes))
        table.set("pretty", windowPrettyFunction)
        return table
    }

    fun uiTreeToTable(tree: UITree): LuaTable {
        val table = LuaTable()
        table.set("__type", LuaValue.valueOf("UITree"))
        table.set("id", toLuaValue(tree.id))
        table.set("isIdUnique", toLuaValue(tree.isIdUnique))
        table.set("className", toLuaValue(tree.className))
        table.set("text", toLuaValue(tree.text))
        table.set("desc", toLuaValue(tree.desc))
        table.set("hintText", toLuaValue(tree.hintText))
        table.set("bounds", toLuaValue(tree.bounds))
        table.set("clickable", LuaValue.valueOf(tree.clickable))
        table.set("longClickable", LuaValue.valueOf(tree.longClickable))
        table.set("scrollable", LuaValue.valueOf(tree.scrollable))
        table.set("editable", LuaValue.valueOf(tree.editable))
        table.set("checkable", LuaValue.valueOf(tree.checkable))
        table.set("checked", LuaValue.valueOf(tree.checked))
        table.set("children", listToTable(tree.children))
        table.set("pretty", treePrettyFunction)
        return table
    }

    fun rectToTable(rect: Rect): LuaTable {
        val table = LuaTable()
        table.set("left", LuaValue.valueOf(rect.left))
        table.set("top", LuaValue.valueOf(rect.top))
        table.set("right", LuaValue.valueOf(rect.right))
        table.set("bottom", LuaValue.valueOf(rect.bottom))
        return table
    }

    fun accessibilityWindowInfoToTable(windowInfo: AccessibilityWindowInfo): LuaTable {
        val table = LuaTable()
        table.set("id", LuaValue.valueOf(windowInfo.id.toInt()))
        table.set("displayId", LuaValue.valueOf(windowInfo.displayId))
        table.set("type", LuaValue.valueOf(windowInfo.type))
        table.set("layer", LuaValue.valueOf(windowInfo.layer))
        table.set("active", LuaValue.valueOf(windowInfo.isActive))
        table.set("focused", LuaValue.valueOf(windowInfo.isFocused))
        table.set("accessibilityFocused", LuaValue.valueOf(windowInfo.isAccessibilityFocused))
        table.set("title", toLuaValue(windowInfo.title?.toString()))
        table.set("packageName", toLuaValue(windowInfo.root?.packageName?.toString()))
        return table
    }

    companion object {
        private const val TAG = "LuaValueConverter"

        private val appListPrettyFunction: LuaValue = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1().checktable()
                val lines = mutableListOf<String>()
                for (i in 1..self.length()) {
                    val app = self.get(i)
                    if (app.istable()) {
                        lines += "[${i}] ${safeString(app.get("appName"))} (${safeString(app.get("packageName"))})"
                    }
                }
                return LuaValue.valueOf(lines.joinToString("\n"))
            }
        }

        private val treePrettyFunction: LuaValue = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1().checktable()
                val depth = if (args.narg() >= 2 && !args.arg(2).isnil()) args.arg(2).toint() else 0
                return LuaValue.valueOf(renderTree(self, depth))
            }
        }

        private val windowPrettyFunction: LuaValue = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1().checktable()
                val lines = mutableListOf<String>()
                lines += "UIWindow {package=${safeString(self.get("packageName"))}, activity=${safeString(self.get("activityName"))}}"

                val matchedNodes = self.get("matchedNodes")
                if (matchedNodes.istable() && matchedNodes.length() > 0) {
                    for (i in 1..matchedNodes.length()) {
                        val node = matchedNodes.get(i)
                        if (node.istable()) {
                            lines += renderTree(node.checktable(), 1)
                        }
                    }
                } else {
                    val root = self.get("root")
                    if (root.istable()) {
                        lines += renderTree(root.checktable(), 1)
                    }
                }
                return LuaValue.valueOf(lines.joinToString("\n"))
            }
        }

        private fun renderTree(node: LuaTable, depth: Int): String {
            val lines = mutableListOf<String>()
            renderTreeNode(node, depth, lines)
            return lines.joinToString("\n")
        }

        private fun renderTreeNode(node: LuaTable, depth: Int, lines: MutableList<String>) {
            val indent = "  ".repeat(depth.coerceAtLeast(0))
            val parts = mutableListOf("${indent}[${safeString(node.get("className"), "View")}]")

            if (!node.get("id").isnil()) {
                parts += "id=${safeString(node.get("id"))}"
                if (!node.get("isIdUnique").isnil()) {
                    parts += "isIdUnique=${safeString(node.get("isIdUnique"))}"
                }
            }
            if (!node.get("text").isnil()) {
                parts += "text=\"${safeString(node.get("text"))}\""
            }
            if (!node.get("desc").isnil()) {
                parts += "desc=\"${safeString(node.get("desc"))}\""
            }
            if (!node.get("hintText").isnil()) {
                parts += "hint=\"${safeString(node.get("hintText"))}\""
            }

            val bounds = node.get("bounds")
            if (bounds.istable()) {
                parts += "bounds=[${safeString(bounds.get("left"))},${safeString(bounds.get("top"))} ${safeString(bounds.get("right"))},${safeString(bounds.get("bottom"))}]"
            }

            lines += parts.joinToString(" ")

            val children = node.get("children")
            if (children.istable()) {
                for (i in 1..children.length()) {
                    val child = children.get(i)
                    if (child.istable()) {
                        renderTreeNode(child.checktable(), depth + 1, lines)
                    }
                }
            }
        }

        private fun safeString(value: LuaValue, defaultValue: String = "nil"): String {
            return if (value.isnil()) defaultValue else value.tojstring()
        }
    }
}

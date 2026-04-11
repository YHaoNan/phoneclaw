package top.yudoge.phoneclaw.emu.domain.objects

import android.graphics.Rect

data class UITree(
    var id: String? = null,
    var className: String? = null,
    var text: String? = null,
    var desc: String? = null,
    var hintText: String? = null,
    var bounds: Rect? = null,
    var clickable: Boolean = false,
    var longClickable: Boolean = false,
    var scrollable: Boolean = false,
    var editable: Boolean = false,
    var checkable: Boolean = false,
    var checked: Boolean = false,
    var children: List<UITree> = emptyList()
) {
    fun toStringTree(depth: Int = 0): String {
        val indent = "  ".repeat(depth)
        val sb = StringBuilder()
        sb.append(indent).append("[${className ?: "View"}]")
        
        id?.let { sb.append(" id=$it") }
        text?.let { sb.append(" text=\"$it\"") }
        desc?.let { sb.append(" desc=\"$it\"") }
        hintText?.let { sb.append(" hint=\"$it\"") }
        
        if (clickable) sb.append(" [clickable]")
        if (longClickable) sb.append(" [longClickable]")
        if (scrollable) sb.append(" [scrollable]")
        if (editable) sb.append(" [editable]")
        if (checkable) sb.append(" [checkable${if (checked) "=checked]" else "]"}")
        
        bounds?.let {
            sb.append(" bounds=[${it.left},${it.top} ${it.right},${it.bottom}]")
        }
        
        if (children.isNotEmpty()) {
            sb.append(" children=${children.size}")
        }
        
        if (children.isNotEmpty()) {
            children.forEach { child ->
                sb.append("\n").append(child.toStringTree(depth + 1))
            }
        }
        
        return sb.toString()
    }
}

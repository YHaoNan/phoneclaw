package top.yudoge.phoneclaw.emu.domain.objects

data class UIWindow(
    val packageName: String? = null,
    val activityName: String? = null,
    val root: UITree? = null,
    val matchedNodes: List<UITree> = emptyList()
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("UIWindow {")
        sb.append("package=${packageName ?: "null"}")
        activityName?.let { sb.append(", activity=$it") }
        
        if (matchedNodes.isNotEmpty()) {
            sb.append(", matched=${matchedNodes.size} nodes}\n")
            matchedNodes.forEachIndexed { i, node ->
                sb.append("  [${i + 1}] $node\n")
            }
        } else if (root != null) {
            sb.append("}\n")
            sb.append(root.toStringTree())
        } else {
            sb.append("}")
        }
        
        return sb.toString()
    }
}

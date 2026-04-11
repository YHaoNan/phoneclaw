package top.yudoge.phoneclaw.emu.domain.objects

data class AppInfo(
    var packageName: String = "",
    var appName: String = ""
) {
    override fun toString(): String = "$appName ($packageName)"
}

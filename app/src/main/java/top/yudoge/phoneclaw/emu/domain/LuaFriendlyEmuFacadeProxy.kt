package top.yudoge.phoneclaw.emu.domain

import org.luaj.vm2.LuaValue

class LuaFriendlyEmuFacadeProxy(
    private val emuFacade: EmuFacadeContract,
    private val converter: LuaValueConverter = LuaValueConverter()
) {
    var screenReadMode: ScreenReadMode
        get() = emuFacade.screenReadMode
        set(value) {
            emuFacade.screenReadMode = value
        }

    fun openApp(packageName: String): Boolean = emuFacade.openApp(packageName)

    fun waitWindowOpened(packageName: String?, activityName: String?, timeoutMs: Long): LuaValue {
        return converter.toLuaValue(emuFacade.waitWindowOpened(packageName, activityName, timeoutMs))
    }

    fun waitMS(milliseconds: Long) {
        emuFacade.waitMS(milliseconds)
    }

    fun back(): Boolean = emuFacade.back()

    fun home(): Boolean = emuFacade.home()

    fun getCurrentWindowByAccessibilityService(
        maxDepth: Int = 50,
        windowPackageName: String? = null,
        filterPattern: String? = null,
        requireClickable: Boolean = false,
        requireLongClickable: Boolean = false,
        requireScrollable: Boolean = false,
        requireEditable: Boolean = false,
        requireCheckable: Boolean = false
    ): LuaValue {
        val ori = emuFacade.getCurrentWindowByAccessibilityService(
            maxDepth = maxDepth,
            windowPackageName = windowPackageName,
            filterPattern = filterPattern,
            requireClickable = requireClickable,
            requireLongClickable = requireLongClickable,
            requireScrollable = requireScrollable,
            requireEditable = requireEditable,
            requireCheckable = requireCheckable
        )
        val converted = converter.toLuaValue(ori)
        return converted
    }

    fun clickById(id: String): Boolean = emuFacade.clickById(id)

    fun clickByPos(x: Double, y: Double): Boolean = emuFacade.clickByPos(x, y)

    fun longClickById(id: String, durationMs: Long = 500): Boolean = emuFacade.longClickById(id, durationMs)

    fun longClickByPos(x: Double, y: Double, durationMs: Long = 500): Boolean = emuFacade.longClickByPos(x, y, durationMs)

    fun swipe(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long): Boolean {
        return emuFacade.swipe(fromX, fromY, toX, toY, durationMs)
    }

    fun inputTextById(id: String, text: String): Boolean = emuFacade.inputTextById(id, text)

    fun inputTextByPos(x: Double, y: Double, text: String): Boolean = emuFacade.inputTextByPos(x, y, text)

    fun getInstalledApps(filterPattern: String?): LuaValue {
        val apps = emuFacade.getInstalledApps(filterPattern) ?: return LuaValue.NIL
        return converter.appInfoListToTable(apps)
    }
}

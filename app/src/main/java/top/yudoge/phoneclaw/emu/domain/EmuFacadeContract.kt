package top.yudoge.phoneclaw.emu.domain

import android.view.accessibility.AccessibilityWindowInfo
import top.yudoge.phoneclaw.emu.domain.objects.AppInfo
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow

interface EmuFacadeContract {
    var screenReadMode: ScreenReadMode

    fun openApp(packageName: String): Boolean

    fun waitWindowOpened(packageName: String?, activityName: String?, timeoutMs: Long): AccessibilityWindowInfo?

    fun waitMS(milliseconds: Long)

    fun back(): Boolean

    fun home(): Boolean

    fun getCurrentWindowByAccessibilityService(
        maxDepth: Int = 50,
        windowPackageName: String? = null,
        filterPattern: String? = null,
        requireClickable: Boolean = false,
        requireLongClickable: Boolean = false,
        requireScrollable: Boolean = false,
        requireEditable: Boolean = false,
        requireCheckable: Boolean = false
    ): UIWindow?

    fun clickById(id: String): Boolean

    fun clickByPos(x: Double, y: Double): Boolean

    fun longClickById(id: String, durationMs: Long = 500): Boolean

    fun longClickByPos(x: Double, y: Double, durationMs: Long = 500): Boolean

    fun swipe(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long): Boolean

    fun inputTextById(id: String, text: String): Boolean

    fun inputTextByPos(x: Double, y: Double, text: String): Boolean

    fun getInstalledApps(filterPattern: String?): List<AppInfo>?
}

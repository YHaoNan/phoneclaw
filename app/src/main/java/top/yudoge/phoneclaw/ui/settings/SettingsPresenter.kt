package top.yudoge.phoneclaw.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import top.yudoge.phoneclaw.emu.EmuAccessibilityService
import top.yudoge.phoneclaw.scripts.domain.ScriptServer
import top.yudoge.phoneclaw.app.service.KeepaliveService
import top.yudoge.phoneclaw.ui.base.BasePresenter
import top.yudoge.phoneclaw.ui.floating.FloatingWindowService

class SettingsPresenter(
    private val context: Context
) : BasePresenter<SettingsContract.View>(), SettingsContract.Presenter {
    
    private var scriptServer: ScriptServer? = null
    
    companion object {
        private const val PREFS_NAME = "phoneclaw"
        private const val PREFS_KEY_KEEPALIVE = "keepalive_enabled"
        private const val PREFS_KEY_FLOATING_WINDOW = "floating_window_enabled"
    }
    
    override fun checkAccessibilityStatus() {
        view?.showAccessibilityEnabled(isAccessibilityServiceEnabled())
    }
    
    override fun toggleAccessibility() {
        if (!isAccessibilityServiceEnabled()) {
            view?.openAccessibilitySettings()
        }
    }
    
    override fun checkFloatingWindowStatus() {
        val isEnabled = FloatingWindowService.isRunning || 
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREFS_KEY_FLOATING_WINDOW, false)
        view?.showFloatingWindowEnabled(isEnabled)
    }
    
    override fun toggleFloatingWindow() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREFS_KEY_FLOATING_WINDOW, false)
        
        if (!isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                view?.openOverlayPermissionSettings()
                return
            }
            prefs.edit().putBoolean(PREFS_KEY_FLOATING_WINDOW, true).apply()
            context.startService(Intent(context, FloatingWindowService::class.java))
            view?.showFloatingWindowEnabled(true)
            view?.showMessage("悬浮窗已启动")
        } else {
            prefs.edit().putBoolean(PREFS_KEY_FLOATING_WINDOW, false).apply()
            context.stopService(Intent(context, FloatingWindowService::class.java))
            view?.showFloatingWindowEnabled(false)
            view?.showMessage("悬浮窗已关闭")
        }
    }
    
    override fun checkKeepaliveStatus() {
        val isEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREFS_KEY_KEEPALIVE, false)
        view?.showKeepaliveEnabled(isEnabled)
    }
    
    override fun toggleKeepalive() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREFS_KEY_KEEPALIVE, false)
        
        prefs.edit().putBoolean(PREFS_KEY_KEEPALIVE, !isEnabled).apply()
        
        if (!isEnabled) {
            context.startForegroundService(Intent(context, KeepaliveService::class.java))
            view?.showKeepaliveEnabled(true)
        } else {
            context.stopService(Intent(context, KeepaliveService::class.java))
            view?.showKeepaliveEnabled(false)
        }
    }
    
    override fun checkServerStatus() {
        val isRunning = scriptServer?.isRunning == true
        val address = if (isRunning) "0.0.0.0:${scriptServer?.port}" else null
        view?.showServerRunning(isRunning, address)
    }
    
    override fun toggleServer() {
        if (scriptServer?.isRunning == true) {
            scriptServer?.stop()
            scriptServer = null
            view?.showServerRunning(false, null)
            view?.showMessage("脚本服务器已停止")
        } else {
            scriptServer = ScriptServer()
            scriptServer?.start(8765)
            view?.showServerRunning(true, "0.0.0.0:${scriptServer?.port}")
            view?.showMessage("脚本服务器已启动")
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${context.packageName}/${EmuAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver, 
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
}

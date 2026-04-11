package top.yudoge.phoneclaw.ui.settings

import top.yudoge.phoneclaw.ui.base.BaseContract

interface SettingsContract {
    
    interface View : BaseContract.View {
        fun showAccessibilityEnabled(enabled: Boolean)
        fun showFloatingWindowEnabled(enabled: Boolean)
        fun showKeepaliveEnabled(enabled: Boolean)
        fun showServerRunning(isRunning: Boolean, address: String?)
        fun showMessage(message: String)
        fun openAccessibilitySettings()
        fun openOverlayPermissionSettings()
    }
    
    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun checkAccessibilityStatus()
        fun toggleAccessibility()
        fun checkFloatingWindowStatus()
        fun toggleFloatingWindow()
        fun checkKeepaliveStatus()
        fun toggleKeepalive()
        fun checkServerStatus()
        fun toggleServer()
    }
}

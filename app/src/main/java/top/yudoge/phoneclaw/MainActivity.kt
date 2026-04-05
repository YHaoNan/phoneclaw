package top.yudoge.phoneclaw

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import top.yudoge.phoneclaw.script.ScriptServer
import top.yudoge.phoneclaw.script.LuaTest
import top.yudoge.phoneclaw.emu.EmuAccessibilityService
import top.yudoge.phoneclaw.emu.EmuApi

class MainActivity : AppCompatActivity() {

    private var scriptServer: ScriptServer? = null
    private var isKeepaliveEnabled: Boolean = false
    private var hasSecureSettingsPermission: Boolean = false

    private val requestOverlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateOverlayStatus()
    }

    private val requestAccessibilityPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateAccessibilityStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        isKeepaliveEnabled = getSharedPreferences("phoneclaw", MODE_PRIVATE)
            .getBoolean("keepalive_enabled", false)

        setupAccessibilityCard()
        setupKeepaliveCard()
        setupSecureSettingsCard()
        setupOverlayCard()
        setupScriptServerCard()
    }

    override fun onResume() {
        super.onResume()
        checkSecureSettingsPermission()
        updateAccessibilityStatus()
        updateOverlayStatus()
        
        if (isKeepaliveEnabled && hasSecureSettingsPermission && !isAccessibilityServiceEnabled()) {
            tryAutoEnableAccessibility()
        }
    }

    private fun setupAccessibilityCard() {
        findViewById<Button>(R.id.accessibility_button).setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                requestAccessibilityPermission.launch(intent)
            } else {
                Toast.makeText(this, "无障碍服务已开启", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupKeepaliveCard() {
        val keepaliveSwitch = findViewById<SwitchCompat>(R.id.keepalive_switch)
        keepaliveSwitch.isChecked = isKeepaliveEnabled

        keepaliveSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasSecureSettingsPermission) {
                Toast.makeText(this, "请先授予高级权限", Toast.LENGTH_SHORT).show()
                keepaliveSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            isKeepaliveEnabled = isChecked
            getSharedPreferences("phoneclaw", MODE_PRIVATE)
                .edit()
                .putBoolean("keepalive_enabled", isChecked)
                .apply()
            
            if (isChecked && !isAccessibilityServiceEnabled()) {
                tryAutoEnableAccessibility()
            }
        }
    }

    private fun setupSecureSettingsCard() {
        findViewById<Button>(R.id.copy_adb_command).setOnClickListener {
            val command = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", command))
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupOverlayCard() {
        findViewById<Button>(R.id.toggle_floating_window_button).setOnClickListener {
            toggleFloatingWindow()
        }
    }

    private fun setupScriptServerCard() {
        findViewById<Button>(R.id.toggle_script_server_button).setOnClickListener {
            toggleScriptServer()
        }
        findViewById<Button>(R.id.test_lua_button).setOnClickListener {
            Thread { LuaTest.runTests() }.start()
        }
    }

    private fun updateAccessibilityStatus() {
        val statusText = findViewById<TextView>(R.id.accessibility_status)
        val button = findViewById<Button>(R.id.accessibility_button)
        val keepaliveCard = findViewById<View>(R.id.keepalive_card)

        if (isAccessibilityServiceEnabled()) {
            statusText.text = "已开启"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            button.text = "已开启"
            button.isEnabled = false
            keepaliveCard.visibility = View.VISIBLE
        } else {
            statusText.text = "未开启"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            button.text = "开启无障碍服务"
            button.isEnabled = true
            keepaliveCard.visibility = View.GONE
        }
    }

    private fun updateOverlayStatus() {
        val statusText = findViewById<TextView>(R.id.overlay_status)
        val button = findViewById<Button>(R.id.toggle_floating_window_button)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            statusText.text = "已授权"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            button.text = if (isFloatingWindowRunning()) "关闭悬浮窗" else "启动悬浮窗"
        } else {
            statusText.text = "未授权"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            button.text = "授权并启动悬浮窗"
        }
    }

    private fun checkSecureSettingsPermission() {
        val statusText = findViewById<TextView>(R.id.secure_settings_status)
        
        hasSecureSettingsPermission = try {
            Settings.Secure.putString(contentResolver, "phoneclaw_permission_test", "test")
            Settings.Secure.putString(contentResolver, "phoneclaw_permission_test", null)
            true
        } catch (e: SecurityException) {
            false
        }

        if (hasSecureSettingsPermission) {
            statusText.text = "已授权"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            findViewById<View>(R.id.adb_command_hint).visibility = View.GONE
            findViewById<View>(R.id.adb_command).visibility = View.GONE
            findViewById<View>(R.id.copy_adb_command).visibility = View.GONE
        } else {
            statusText.text = "未授权"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${EmuAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }

    private fun isFloatingWindowRunning(): Boolean {
        return FloatingWindowService.isRunning
    }

    private fun toggleFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            requestOverlayPermission.launch(intent)
            return
        }

        if (isFloatingWindowRunning()) {
            stopService(Intent(this, FloatingWindowService::class.java))
            Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show()
        } else {
            startService(Intent(this, FloatingWindowService::class.java))
            Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
        }
        updateOverlayStatus()
    }

    private fun toggleScriptServer() {
        val button = findViewById<Button>(R.id.toggle_script_server_button)
        val statusText = findViewById<TextView>(R.id.script_server_status)
        val addressText = findViewById<TextView>(R.id.script_server_address)

        if (scriptServer?.isRunning == true) {
            scriptServer?.stop()
            scriptServer = null
            button.text = "启动服务器"
            statusText.text = "未运行"
            statusText.setTextColor(getColor(android.R.color.darker_gray))
            addressText.text = ""
            Toast.makeText(this, "脚本服务器已停止", Toast.LENGTH_SHORT).show()
        } else {
            scriptServer = ScriptServer()
            scriptServer!!.injectGlobal("emu", EmuApi())
            val port = scriptServer!!.start(8765)
            if (port > 0) {
                button.text = "停止服务器"
                val ip = getLocalIpAddress()
                if (ip != null) {
                    statusText.text = "运行中: $ip:$port"
                    statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    addressText.text = "curl -X POST http://$ip:$port/eval -d \"脚本内容\""
                } else {
                    statusText.text = "运行中 (端口: $port)"
                    statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    addressText.text = "未检测到网络，可使用 adb forward tcp:8765 tcp:8765"
                }
                Toast.makeText(this, "脚本服务器已启动", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.startsWith("wlan") ||
                    networkInterface.name.startsWith("rmnet") ||
                    networkInterface.name.startsWith("eth")
                ) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun tryAutoEnableAccessibility() {
        try {
            val service = "$packageName/${EmuAccessibilityService::class.java.name}"
            val currentServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val newServices = if (currentServices.isEmpty()) {
                service
            } else if (!currentServices.contains(service)) {
                "$currentServices:$service"
            } else {
                currentServices
            }

            Settings.Secure.putString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newServices
            )
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            )
            Toast.makeText(this, "无障碍服务已自动开启", Toast.LENGTH_SHORT).show()
            updateAccessibilityStatus()
        } catch (e: SecurityException) {
            hasSecureSettingsPermission = false
            checkSecureSettingsPermission()
            Toast.makeText(this, "自动开启失败，请手动开启", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scriptServer?.stop()
    }
}

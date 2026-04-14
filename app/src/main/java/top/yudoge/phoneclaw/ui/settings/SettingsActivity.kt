package top.yudoge.phoneclaw.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.databinding.ActivitySettingsBinding
import top.yudoge.phoneclaw.emu.EmuAccessibilityService
import top.yudoge.phoneclaw.scripts.domain.ScriptServer
import top.yudoge.phoneclaw.ui.floating.FloatingWindowService
import top.yudoge.phoneclaw.ui.settings.model.ProviderListActivity
import top.yudoge.phoneclaw.ui.settings.skill.SkillListActivity
import top.yudoge.phoneclaw.app.service.KeepaliveService

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var scriptServer: ScriptServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupToolbar()
        setupCardClicks()
        setupPermissionSettings()
        setupScriptServer()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCardClicks() {
        binding.modelSettingsCard.setOnClickListener {
            startActivity(Intent(this, ProviderListActivity::class.java))
        }

        binding.skillSettingsCard.setOnClickListener {
            startActivity(Intent(this, SkillListActivity::class.java))
        }
    }

    private fun setupPermissionSettings() {
        updateAccessibilityStatus()
        updateFloatingWindowStatus()

        binding.accessibilitySwitch.setOnCheckedChangeListener { _, _ ->
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }

        binding.floatingWindowSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    binding.floatingWindowSwitch.isChecked = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    return@setOnCheckedChangeListener
                }
                getSharedPreferences("phoneclaw", MODE_PRIVATE)
                    .edit()
                    .putBoolean("floating_window_enabled", true)
                    .apply()
                startService(Intent(this, FloatingWindowService::class.java))
                Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
            } else {
                getSharedPreferences("phoneclaw", MODE_PRIVATE)
                    .edit()
                    .putBoolean("floating_window_enabled", false)
                    .apply()
                stopService(Intent(this, FloatingWindowService::class.java))
                Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show()
            }
        }

        binding.keepaliveSwitch.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("phoneclaw", MODE_PRIVATE)
                .edit()
                .putBoolean("keepalive_enabled", isChecked)
                .apply()
            
            if (isChecked) {
                val intent = Intent(this, KeepaliveService::class.java)
                startForegroundService(intent)
            } else {
                val intent = Intent(this, KeepaliveService::class.java)
                stopService(intent)
            }
        }

        binding.keepaliveSwitch.isChecked = getSharedPreferences("phoneclaw", MODE_PRIVATE)
            .getBoolean("keepalive_enabled", false)
    }

    private fun setupScriptServer() {
        updateServerStatus()

        binding.serverSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startScriptServer()
            } else {
                stopScriptServer()
            }
        }
    }

    private fun startScriptServer() {
        scriptServer = ScriptServer()
        scriptServer!!.injectGlobal("emu", AppContainer.getInstance().luaFriendlyEmuFacadeProxy)
        scriptServer!!.start(8765)
        updateServerStatus()
        Toast.makeText(this, "脚本服务器已启动", Toast.LENGTH_SHORT).show()
    }

    private fun stopScriptServer() {
        scriptServer?.stop()
        scriptServer = null
        updateServerStatus()
        Toast.makeText(this, "脚本服务器已停止", Toast.LENGTH_SHORT).show()
    }

    private fun updateAccessibilityStatus() {
        binding.accessibilitySwitch.isChecked = isAccessibilityServiceEnabled()
    }

    private fun updateFloatingWindowStatus() {
        val isEnabled = FloatingWindowService.isRunning || 
            getSharedPreferences("phoneclaw", MODE_PRIVATE)
                .getBoolean("floating_window_enabled", false)
        binding.floatingWindowSwitch.isChecked = isEnabled
    }

    private fun updateServerStatus() {
        val isRunning = scriptServer?.isRunning == true
        binding.serverSwitch.isChecked = isRunning
        binding.serverStatusText.text = if (isRunning) {
            "运行中: 0.0.0.0:${scriptServer?.port}"
        } else {
            getString(R.string.status_stopped)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${EmuAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
        updateFloatingWindowStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop server when activity is destroyed
    }
}

package top.yudoge.phoneclaw.ui.floating

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.LayoutFloatingWindowBinding

class FloatingWindowService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var binding: LayoutFloatingWindowBinding
    private lateinit var floatingView: View
    private lateinit var prefs: SharedPreferences
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != FloatingWindowStatusNotifier.ACTION_STATUS_CHANGED) {
                return
            }
            val state = intent.getStringExtra(FloatingWindowStatusNotifier.EXTRA_STATE)
                ?: FloatingWindowStatusNotifier.STATE_IDLE
            val title = intent.getStringExtra(FloatingWindowStatusNotifier.EXTRA_TITLE)
            renderStatus(state, title)
        }
    }

    companion object {
        @JvmStatic
        var isRunning: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("phoneclaw", MODE_PRIVATE)

        if (!checkShouldRun()) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        registerStatusReceiver()
        isRunning = true
    }

    private fun checkShouldRun(): Boolean {
        val isEnabled = prefs.getBoolean("floating_window_enabled", false)
        if (!isEnabled) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            prefs.edit().putBoolean("floating_window_enabled", false).apply()
            return false
        }

        return true
    }

    private fun createFloatingWindow() {
        val context = android.view.ContextThemeWrapper(this, R.style.Theme_PhoneClaw)
        binding = LayoutFloatingWindowBinding.inflate(LayoutInflater.from(context))
        floatingView = binding.root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        windowManager.addView(floatingView, params)
        showIdle()
    }

    private fun showIdle() {
        binding.icon.setImageResource(R.drawable.ic_thinking)
        binding.title.text = getString(R.string.idle)
        applyStatusStyle(R.color.floating_state_idle)
    }

    private fun registerStatusReceiver() {
        val filter = IntentFilter(FloatingWindowStatusNotifier.ACTION_STATUS_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }
    }

    private fun renderStatus(state: String, title: String?) {
        when (state) {
            FloatingWindowStatusNotifier.STATE_REASONING -> {
                binding.icon.setImageResource(R.drawable.ic_thinking)
                binding.title.text = title ?: getString(R.string.thinking)
                applyStatusStyle(R.color.floating_state_reasoning)
            }

            FloatingWindowStatusNotifier.STATE_TOOL_RUNNING -> {
                binding.icon.setImageResource(R.drawable.ic_tool)
                binding.title.text = title ?: getString(R.string.running)
                applyStatusStyle(R.color.floating_state_tool_running)
            }

            FloatingWindowStatusNotifier.STATE_TOOL_SUCCESS -> {
                binding.icon.setImageResource(R.drawable.ic_check)
                binding.title.text = title ?: getString(R.string.success)
                applyStatusStyle(R.color.floating_state_tool_success)
            }

            FloatingWindowStatusNotifier.STATE_TOOL_FAILED -> {
                binding.icon.setImageResource(R.drawable.ic_close)
                binding.title.text = title ?: getString(R.string.failed)
                applyStatusStyle(R.color.floating_state_tool_failed)
            }

            FloatingWindowStatusNotifier.STATE_SKILL_RUNNING -> {
                binding.icon.setImageResource(R.drawable.ic_skill)
                binding.title.text = title ?: getString(R.string.running)
                applyStatusStyle(R.color.floating_state_skill_running)
            }

            FloatingWindowStatusNotifier.STATE_SKILL_SUCCESS -> {
                binding.icon.setImageResource(R.drawable.ic_check)
                binding.title.text = title ?: getString(R.string.success)
                applyStatusStyle(R.color.floating_state_skill_success)
            }

            FloatingWindowStatusNotifier.STATE_SKILL_FAILED -> {
                binding.icon.setImageResource(R.drawable.ic_close)
                binding.title.text = title ?: getString(R.string.failed)
                applyStatusStyle(R.color.floating_state_skill_failed)
            }

            FloatingWindowStatusNotifier.STATE_COMPLETED -> {
                binding.icon.setImageResource(R.drawable.ic_check)
                binding.title.text = title ?: getString(R.string.completed)
                applyStatusStyle(R.color.floating_state_completed)
            }

            FloatingWindowStatusNotifier.STATE_ERROR -> {
                binding.icon.setImageResource(R.drawable.ic_close)
                binding.title.text = title ?: getString(R.string.failed)
                applyStatusStyle(R.color.floating_state_error)
            }

            else -> showIdle()
        }
    }

    private fun applyStatusStyle(backgroundColorRes: Int) {
        val bgColor = ContextCompat.getColor(this, backgroundColorRes)
        binding.rootView.background?.mutate()?.setTint(bgColor)
        binding.icon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        binding.title.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(statusReceiver) }
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        isRunning = false
    }
}

package top.yudoge.phoneclaw.ui.floating

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.databinding.LayoutFloatingWindowBinding

class FloatingWindowService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var binding: LayoutFloatingWindowBinding
    private lateinit var floatingView: View
    private lateinit var prefs: SharedPreferences

    private var isVisible = false

    companion object {
        @JvmField
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
        observeAgentStatus()
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
        binding = LayoutFloatingWindowBinding.inflate(LayoutInflater.from(this))
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
        floatingView.visibility = View.GONE
    }

    private fun observeAgentStatus() {
        lifecycleScope.launch {
            AgentStatusManager.status.collect { status ->
                when (status) {
                    is AgentStatusManager.AgentStatus.Idle -> {
                        showIdle()
                    }
                    is AgentStatusManager.AgentStatus.Thinking -> {
                        showThinking()
                    }
                    is AgentStatusManager.AgentStatus.ToolCalling -> {
                        showToolCall(status.name, status.state)
                    }
                    is AgentStatusManager.AgentStatus.SkillCalling -> {
                        showSkillCall(status.name, status.state)
                    }
                    is AgentStatusManager.AgentStatus.Completed -> {
                        showCompleted(status.success)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        if (!isVisible) {
            showWithAnimation()
        }
        binding.icon.setImageResource(R.drawable.ic_thinking)
        binding.title.text = getString(R.string.idle)
        binding.status.text = ""
        binding.status.setTextColor(ContextCompat.getColor(this, R.color.floating_text_secondary))
        animateStatusChange()
    }

    private fun showThinking() {
        if (!isVisible) {
            showWithAnimation()
        }
        binding.icon.setImageResource(R.drawable.ic_thinking)
        binding.title.text = getString(R.string.thinking)
        binding.status.text = ""
        binding.status.setTextColor(ContextCompat.getColor(this, R.color.primary))
        animateStatusChange()
    }

    private fun showToolCall(name: String, state: AgentStatusManager.CallState) {
        if (!isVisible) {
            showWithAnimation()
        }

        binding.icon.setImageResource(R.drawable.ic_tool)
        binding.title.text = name

        when (state) {
            AgentStatusManager.CallState.RUNNING -> {
                binding.status.text = getString(R.string.running)
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.primary))
            }
            AgentStatusManager.CallState.SUCCESS -> {
                binding.status.text = getString(R.string.success)
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.success))
            }
            AgentStatusManager.CallState.FAILED -> {
                binding.status.text = getString(R.string.failed)
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.error))
            }
        }

        animateStatusChange()
    }

    private fun showSkillCall(name: String, state: AgentStatusManager.CallState) {
        if (!isVisible) {
            showWithAnimation()
        }

        binding.icon.setImageResource(R.drawable.ic_skill)
        binding.title.text = name

        when (state) {
            AgentStatusManager.CallState.RUNNING -> {
                binding.status.text = getString(R.string.running)
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.secondary))
            }
            AgentStatusManager.CallState.SUCCESS -> {
                binding.status.text = getString(R.string.success)
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.success))
            }
            AgentStatusManager.CallState.FAILED -> {
                binding.status.text = getString(R.string.failed)
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.error))
            }
        }

        animateStatusChange()
    }

    private fun showCompleted(success: Boolean) {
        if (!isVisible) {
            showWithAnimation()
        }

        binding.icon.setImageResource(if (success) R.drawable.ic_check else R.drawable.ic_stop)
        binding.title.text = if (success) getString(R.string.completed) else getString(R.string.failed)
        
        binding.status.setTextColor(
            if (success) ContextCompat.getColor(this, R.color.success)
            else ContextCompat.getColor(this, R.color.error)
        )
        
        animateStatusChange()
    }

    private fun showWithAnimation() {
        isVisible = true
        floatingView.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(floatingView, "scaleX", 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(floatingView, "scaleY", 0.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(floatingView, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 300
            interpolator = AnticipateOvershootInterpolator()
        }.start()
    }

    private fun hideFloatingWindow() {
        if (!isVisible) return

        val scaleX = ObjectAnimator.ofFloat(floatingView, "scaleX", 1f, 0.5f)
        val scaleY = ObjectAnimator.ofFloat(floatingView, "scaleY", 1f, 0.5f)
        val alpha = ObjectAnimator.ofFloat(floatingView, "alpha", 1f, 0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 200
            interpolator = DecelerateInterpolator()
        }.apply {
            doOnEnd {
                floatingView.visibility = View.GONE
                isVisible = false
            }
        }.start()
    }

    private fun animateStatusChange() {
        val scaleDown = ObjectAnimator.ofFloat(binding.cardView, "scaleX", 1f, 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(binding.cardView, "scaleX", 0.95f, 1f)

        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            duration = 150
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        isRunning = false
    }
}

private fun AnimatorSet.doOnEnd(action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            action()
        }
    })
}

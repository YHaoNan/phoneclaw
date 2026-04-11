package top.yudoge.phoneclaw.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import top.yudoge.phoneclaw.ui.chat.ChatActivity
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.emu.EmuAccessibilityService

class KeepaliveService : Service() {

    private lateinit var prefs: SharedPreferences

    companion object {
        const val ACTION_CHECK_KEEPALIVE = "top.yudoge.phoneclaw.CHECK_KEEPALIVE"
        const val CHANNEL_ID = "keepalive_channel"
        const val NOTIFICATION_ID = 1001
        const val CHECK_INTERVAL = 60 * 1000L // 1 minute
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("phoneclaw", MODE_PRIVATE)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkAndEnableAccessibility()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        cancelAlarm()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "保活服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持无障碍服务正常运行"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, ChatActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("PhoneClaw 保活中")
                .setContentText("无障碍服务保活中")
                .setSmallIcon(R.drawable.ic_tool)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("PhoneClaw 保活中")
                .setContentText("无障碍服务保活中")
                .setSmallIcon(R.drawable.ic_tool)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun scheduleNextCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, KeepaliveService::class.java).apply {
            action = ACTION_CHECK_KEEPALIVE
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = SystemClock.elapsedRealtime() + CHECK_INTERVAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, KeepaliveService::class.java).apply {
            action = ACTION_CHECK_KEEPALIVE
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun checkAndEnableAccessibility() {
        val isKeepaliveEnabled = prefs.getBoolean("keepalive_enabled", false)
        if (!isKeepaliveEnabled) {
            stopSelf()
            return
        }

        if (!hasSecureSettingsPermission()) {
            scheduleNextCheck()
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            tryAutoEnableAccessibility()
        }
        
        scheduleNextCheck()
    }

    private fun hasSecureSettingsPermission(): Boolean {
        return try {
            Settings.Secure.putString(contentResolver, "phoneclaw_permission_test", "test")
            Settings.Secure.putString(contentResolver, "phoneclaw_permission_test", null)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${EmuAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServices.contains(service)
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
        } catch (e: SecurityException) {
        }
    }
}

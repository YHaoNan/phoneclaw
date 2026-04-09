package top.yudoge.phoneclaw.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import top.yudoge.phoneclaw.service.KeepaliveService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("phoneclaw", Context.MODE_PRIVATE)
            val isKeepaliveEnabled = prefs.getBoolean("keepalive_enabled", false)
            
            if (isKeepaliveEnabled) {
                val serviceIntent = Intent(context, KeepaliveService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
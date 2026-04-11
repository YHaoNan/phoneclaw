package top.yudoge.phoneclaw.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import top.yudoge.phoneclaw.app.service.KeepaliveService

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

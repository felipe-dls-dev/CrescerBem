package br.com.crescerbem.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.crescerbem.CrescerBemApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarmes somem ao reiniciar o aparelho; aqui eles são agendados de novo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val container = (context.applicationContext as CrescerBemApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.reminderSync.sync()
            } finally {
                pending.finish()
            }
        }
    }
}

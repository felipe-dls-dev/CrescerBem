package br.com.crescerbem.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.domain.Reminder
import br.com.crescerbem.domain.ReminderPlanner
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Sincroniza os alarmes do sistema com o que está no banco: agenda o que falta e cancela o que
 * sobrou (registro excluído, concluído, bebê apagado ou lembretes desligados).
 * Usa alarmes inexatos: não precisa de permissão de alarme exato e basta para lembretes de saúde.
 */
class ReminderSync(
    private val context: Context,
    private val db: AppDatabase,
    private val store: ReminderStore,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    suspend fun sync() {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val wanted: List<Reminder> = if (store.enabled.value) {
            ReminderPlanner.plan(
                db.appointmentDao().getAllScheduled(),
                db.vaccineDao().getAllScheduled(),
                now(),
            )
        } else {
            emptyList()
        }
        val wantedKeys = wanted.map { it.key }.toSet()
        store.scheduledKeys.filter { it !in wantedKeys }.forEach { alarms.cancel(pendingIntent(it, null)) }
        wanted.forEach {
            val millis = it.triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(it.key, it.kind.name))
        }
        store.scheduledKeys = wantedKeys
    }

    private fun pendingIntent(key: String, kind: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setData(Uri.parse("crescerbem://lembrete/$key"))
            .putExtra(ReminderReceiver.EXTRA_KIND, kind)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

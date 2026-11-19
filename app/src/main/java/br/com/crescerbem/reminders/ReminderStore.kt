package br.com.crescerbem.reminders

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferência de lembretes (desligada por padrão) e as chaves dos alarmes já agendados. */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("lembretes", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = state.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        state.value = value
    }

    var scheduledKeys: Set<String>
        get() = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit { putStringSet(KEY_SCHEDULED, value) }

    private companion object {
        const val KEY_ENABLED = "ativos"
        const val KEY_SCHEDULED = "agendados"
    }
}

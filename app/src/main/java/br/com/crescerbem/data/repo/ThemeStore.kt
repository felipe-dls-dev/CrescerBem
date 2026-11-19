package br.com.crescerbem.data.repo

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Aparência escolhida pelo responsável. SYSTEM segue a configuração do Android. */
enum class ThemeMode(val label: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK("Escuro"),
}

interface ThemeStore {
    val mode: StateFlow<ThemeMode>
    fun set(mode: ThemeMode)
}

class SharedPrefsThemeStore(context: Context) : ThemeStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        prefs.getString(KEY, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.SYSTEM,
    )

    override val mode: StateFlow<ThemeMode> = state.asStateFlow()

    override fun set(mode: ThemeMode) {
        prefs.edit { putString(KEY, mode.name) }
        state.value = mode
    }

    private companion object {
        const val PREFS = "preferencias"
        const val KEY = "tema"
    }
}

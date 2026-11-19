package br.com.crescerbem.data.repo

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda qual bebê está selecionado no topo do app (apenas o id, nenhum dado de saúde). */
interface ActiveBabyStore {
    val activeBabyId: StateFlow<Long?>
    fun setActive(id: Long?)
}

class SharedPrefsActiveBabyStore(context: Context) : ActiveBabyStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getLong(KEY, NONE).takeIf { it != NONE })

    override val activeBabyId: StateFlow<Long?> = state.asStateFlow()

    override fun setActive(id: Long?) {
        prefs.edit { if (id == null) remove(KEY) else putLong(KEY, id) }
        state.value = id
    }

    private companion object {
        const val PREFS = "preferencias"
        const val KEY = "bebe_ativo"
        const val NONE = -1L
    }
}

class InMemoryActiveBabyStore(initial: Long? = null) : ActiveBabyStore {
    private val state = MutableStateFlow(initial)
    override val activeBabyId: StateFlow<Long?> = state.asStateFlow()
    override fun setActive(id: Long?) {
        state.value = id
    }
}

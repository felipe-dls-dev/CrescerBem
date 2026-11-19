package br.com.crescerbem

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CrescerBemApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Mantém os lembretes em dia (ex.: após atualizar o app ou virar o dia).
        CoroutineScope(Dispatchers.IO).launch { container.reminderSync.sync() }
    }
}

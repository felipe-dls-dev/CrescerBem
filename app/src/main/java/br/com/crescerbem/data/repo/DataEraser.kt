package br.com.crescerbem.data.repo

import br.com.crescerbem.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** "Apagar todos os dados" em Ajustes: banco, fotos, relatórios e preferência do bebê ativo. */
class DataEraser(
    private val db: AppDatabase,
    private val photos: PhotoRepository,
    private val activeStore: ActiveBabyStore,
    private val deleteReports: () -> Unit,
) {
    suspend fun eraseEverything() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        photos.deleteAllFiles()
        deleteReports()
        activeStore.setActive(null)
    }
}

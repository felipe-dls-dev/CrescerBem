package br.com.crescerbem.data.repo

import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.data.model.Baby
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class BabyRepository(
    private val db: AppDatabase,
    private val photos: PhotoRepository,
    private val activeStore: ActiveBabyStore,
) {
    val babies: Flow<List<Baby>> = db.babyDao().observeAll()

    /** Bebê selecionado; se nenhum foi escolhido (ou foi excluído), usa o primeiro da lista. */
    val activeBaby: Flow<Baby?> = combine(babies, activeStore.activeBabyId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.distinctUntilChanged()

    suspend fun get(id: Long): Baby? = db.babyDao().getById(id)

    /** Primeira cor da paleta ainda não usada por outro bebê; se todas estiverem em uso, recomeça pela ordem. */
    suspend fun nextColorIndex(paletteSize: Int): Int {
        val all = db.babyDao().getAll()
        val used = all.map { it.colorIndex }.toSet()
        return (0 until paletteSize).firstOrNull { it !in used } ?: (all.size % paletteSize)
    }

    /** Salva e, se for um bebê novo, já o deixa ativo. Retorna o id. */
    suspend fun save(baby: Baby): Long {
        if (baby.id != 0L) {
            db.babyDao().update(baby)
            return baby.id
        }
        val id = db.babyDao().insert(baby)
        activeStore.setActive(id)
        return id
    }

    fun setActive(id: Long) = activeStore.setActive(id)

    /** Exclui o bebê, todos os registros (cascata) e os arquivos de foto. */
    suspend fun delete(baby: Baby) {
        photos.deleteFilesForBaby(baby.id)
        db.babyDao().delete(baby)
        if (activeStore.activeBabyId.value == baby.id) activeStore.setActive(null)
    }
}

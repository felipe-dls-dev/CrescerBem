package br.com.crescerbem.data.repo

import br.com.crescerbem.data.db.PhotoDao
import br.com.crescerbem.data.model.Photo
import br.com.crescerbem.data.model.RecordType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/** Operações de arquivo de foto usadas pelo repositório (implementado em photos/PhotoStorage). */
interface PhotoFiles {
    fun delete(fileName: String)
    fun deleteAll()
}

/**
 * Mantém as linhas da tabela de fotos e os arquivos em sincronia.
 * Regra de privacidade: excluir um registro exclui também o arquivo da foto.
 */
class PhotoRepository(
    private val dao: PhotoDao,
    private val files: PhotoFiles,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    fun observeFor(type: RecordType, ownerId: Long): Flow<List<Photo>> = dao.observeForOwner(type, ownerId)

    suspend fun fileNamesFor(type: RecordType, ownerId: Long): List<String> =
        dao.getForOwner(type, ownerId).map { it.fileName }

    /** Deixa o registro com exatamente as fotos [desired]: insere as novas e apaga as removidas. */
    suspend fun sync(babyId: Long, type: RecordType, ownerId: Long, desired: List<String>) {
        val existing = dao.getForOwner(type, ownerId)
        existing.filter { it.fileName !in desired }.forEach { photo ->
            dao.deleteById(photo.id)
            files.delete(photo.fileName)
        }
        val existingNames = existing.map { it.fileName }.toSet()
        desired.distinct().filter { it !in existingNames }.forEach { fileName ->
            dao.insert(
                Photo(
                    babyId = babyId,
                    ownerType = type,
                    ownerId = ownerId,
                    fileName = fileName,
                    addedAt = now(),
                ),
            )
        }
    }

    suspend fun deleteFor(type: RecordType, ownerId: Long) {
        dao.getForOwner(type, ownerId).forEach { files.delete(it.fileName) }
        dao.deleteForOwner(type, ownerId)
    }

    /** Apaga os arquivos do bebê. As linhas somem pela exclusão em cascata do bebê. */
    suspend fun deleteFilesForBaby(babyId: Long) {
        dao.getForBaby(babyId).forEach { files.delete(it.fileName) }
    }

    suspend fun photosByOwner(babyId: Long): Map<Pair<RecordType, Long>, List<String>> =
        dao.getForBaby(babyId).groupBy({ it.ownerType to it.ownerId }, { it.fileName })

    /** Descarta um arquivo que nunca chegou a ser salvo num registro. */
    fun discardFile(fileName: String) = files.delete(fileName)

    fun deleteAllFiles() = files.deleteAll()
}

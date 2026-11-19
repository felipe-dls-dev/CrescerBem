package br.com.crescerbem.photos

import br.com.crescerbem.report.ReportFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileCleanupTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val now = 1_800_000_000_000L
    private val hour = 60L * 60 * 1000

    private fun file(name: String, ageMillis: Long): File =
        File(tmp.root, name).apply {
            writeText("x")
            setLastModified(now - ageMillis)
        }

    @Test
    fun `remove so arquivos de camera antigos`() {
        val stale = file("camera_velho.jpg", 25 * hour)
        val recent = file("camera_em_andamento.jpg", 1 * hour)
        val photo = file("foto-salva.jpg", 100 * hour)

        val removed = PhotoStorage.removeStaleCameraFiles(tmp.root, now)

        assertEquals(1, removed)
        assertFalse(stale.exists())
        assertTrue("captura recente pode estar em andamento", recent.exists())
        assertTrue("fotos salvas nunca são apagadas por esta limpeza", photo.exists())
    }

    @Test
    fun `mantem so o relatorio mais recente`() {
        val old1 = file("relatorio-a.pdf", 2 * hour)
        val old2 = file("relatorio-b.pdf", 1 * hour)
        val latest = file("relatorio-c.pdf", 0)

        assertEquals(2, ReportFiles.deleteOthers(tmp.root, latest))
        assertFalse(old1.exists())
        assertFalse(old2.exists())
        assertTrue(latest.exists())
    }

    @Test
    fun `pasta inexistente nao quebra`() {
        val missing = File(tmp.root, "nao-existe")
        assertEquals(0, PhotoStorage.removeStaleCameraFiles(missing, now))
        assertEquals(0, ReportFiles.deleteOthers(missing, File(missing, "x.pdf")))
    }
}

package br.com.crescerbem.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.RecordKey
import br.com.crescerbem.domain.ReportBuilder
import br.com.crescerbem.domain.ReportData
import br.com.crescerbem.domain.ReportRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** Desenha cada página num bitmap, já que o PdfDocument nativo não existe no Robolectric. */
private class BitmapSink : PageSink {
    val pages = mutableListOf<Bitmap>()
    var open = false
    override fun startPage(number: Int, width: Int, height: Int): Canvas {
        check(!open) { "página anterior não foi fechada" }
        open = true
        assertEquals(pages.size + 1, number)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        pages += bitmap
        return Canvas(bitmap)
    }

    override fun finishPage() {
        check(open) { "nenhuma página aberta" }
        open = false
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfReportWriterTest {
    private val baby = Baby(id = 1, name = "Bebê Teste", birthDate = LocalDate.of(2026, 9, 1))
    private val writer = PdfReportWriter { name -> File("inexistente", name) }

    private fun Bitmap.hasInkBetween(top: Int, bottom: Int): Boolean {
        for (y in top until bottom step 2) for (x in 0 until width step 3) {
            if (getPixel(x, y) != Color.WHITE) return true
        }
        return false
    }

    @Test
    fun `tabela longa quebra em varias paginas e todas tem rodape`() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 11, 30)
        // 61 dias de alimentação geram uma tabela longa, que precisa quebrar página.
        val feedings = (0L..60L).flatMap { offset ->
            val day = start.plusDays(offset)
            listOf(
                Feeding(babyId = 1, dateTime = day.atTime(6, 0), type = FeedingType.BREAST_MILK, durationMinutes = 15),
                Feeding(babyId = 1, dateTime = day.atTime(12, 0), type = FeedingType.FORMULA, amountMl = 90),
            )
        }
        val request = ReportRequest(baby, start, end, ReportBuilder.reportableTypes.toSet())
        val document = ReportBuilder.build(request, ReportData(feedings = feedings), LocalDateTime.of(2026, 12, 1, 9, 0))
        val sink = BitmapSink()

        val pageCount = writer.render(document, sink)

        assertTrue("esperava mais de 1 página, veio $pageCount", pageCount > 1)
        assertEquals(pageCount, sink.pages.size)
        assertTrue(!sink.open)
        // Rodapé de aviso desenhado na faixa inferior de todas as páginas.
        sink.pages.forEach { page -> assertTrue(page.hasInkBetween(page.height - 70, page.height - 30)) }
    }

    @Test
    fun `foto ausente no disco nao quebra a geracao`() {
        val request = ReportRequest(baby, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), ReportBuilder.reportableTypes.toSet(), includePhotos = true)
        val data = ReportData(
            moments = listOf(Moment(id = 9, babyId = 1, dateTime = LocalDateTime.of(2026, 11, 2, 15, 0), title = "Primeiro sorriso")),
            photos = mapOf(RecordKey(RecordType.MOMENT, 9) to listOf("sumiu.jpg")),
        )
        val sink = BitmapSink()
        val pages = writer.render(ReportBuilder.build(request, data, LocalDateTime.of(2026, 12, 1, 9, 0)), sink)
        assertTrue(pages in 1..2)
        assertTrue(!sink.open)
    }
}

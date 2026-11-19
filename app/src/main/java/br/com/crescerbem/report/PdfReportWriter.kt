package br.com.crescerbem.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import br.com.crescerbem.domain.ReportBlock
import br.com.crescerbem.domain.ReportDocument
import br.com.crescerbem.domain.ReportSection
import br.com.crescerbem.photos.ImageSizing
import java.io.File
import java.io.FileOutputStream

/** Destino das páginas desenhadas: o PDF no app, bitmaps nos testes. */
interface PageSink {
    fun startPage(number: Int, width: Int, height: Int): Canvas
    fun finishPage()
}

private class PdfDocumentSink(private val pdf: PdfDocument) : PageSink {
    private var page: PdfDocument.Page? = null

    override fun startPage(number: Int, width: Int, height: Int): Canvas {
        val started = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, number).create())
        page = started
        return started.canvas
    }

    override fun finishPage() {
        page?.let(pdf::finishPage)
        page = null
    }
}

/**
 * Desenha um [ReportDocument] em PDF (A4) com o PdfDocument do Android.
 * Tudo acontece no aparelho; nenhuma biblioteca externa ou rede é usada.
 */
class PdfReportWriter(private val photoFile: (String) -> File) {

    fun write(document: ReportDocument, output: File) {
        output.parentFile?.mkdirs()
        val pdf = PdfDocument()
        try {
            render(document, PdfDocumentSink(pdf))
            FileOutputStream(output).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    /** Desenha o documento página a página em [sink]. Retorna o número de páginas. */
    fun render(document: ReportDocument, sink: PageSink): Int {
        val renderer = Renderer(sink, document.footer, photoFile)
        renderer.header(document)
        document.sections.forEach { renderer.section(it) }
        renderer.finish()
        return renderer.pageCount
    }

    private class Renderer(
        private val sink: PageSink,
        private val footer: String,
        private val photoFile: (String) -> File,
    ) {
        private var currentCanvas: Canvas? = null
        private var pageNumber = 0
        private var y = 0f

        val pageCount: Int get() = pageNumber

        private val canvas: Canvas get() = currentCanvas!!
        private val contentWidth = PAGE_WIDTH - 2 * MARGIN
        private val contentBottom = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT

        private val brandPaint = textPaint(11f, bold = true, color = BRAND)
        private val titlePaint = textPaint(20f, bold = true)
        private val sectionPaint = textPaint(13f, bold = true, color = BRAND_DARK)
        private val headingPaint = textPaint(10.5f, bold = true)
        private val bodyPaint = textPaint(10f)
        private val cellPaint = textPaint(9f)
        private val cellHeaderPaint = textPaint(9f, bold = true)
        private val smallPaint = textPaint(8f, color = MUTED)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RULE
            strokeWidth = 0.6f
        }

        fun header(document: ReportDocument) {
            newPage()
            textBlock("Crescer Bem", brandPaint, MARGIN, contentWidth, after = 2f)
            textBlock(document.title, titlePaint, MARGIN, contentWidth, after = 8f)
            document.headerLines.forEach { textBlock(it, bodyPaint, MARGIN, contentWidth, after = 1f) }
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 12f
        }

        fun section(section: ReportSection) {
            // Evita título de seção sozinho no pé da página.
            ensureSpace(SECTION_BAND + 40f)
            fillPaint.color = BAND
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + SECTION_BAND), 4f, 4f, fillPaint)
            val layout = layout(section.title, sectionPaint, contentWidth - 16f)
            drawLayoutAt(layout, MARGIN + 8f, y + (SECTION_BAND - layout.height) / 2f)
            y += SECTION_BAND + 8f
            section.blocks.forEach { block ->
                when (block) {
                    is ReportBlock.Paragraph -> textBlock(block.text, bodyPaint, MARGIN, contentWidth, after = 6f)
                    is ReportBlock.Table -> table(block)
                    is ReportBlock.Entry -> entry(block)
                }
            }
            y += 10f
        }

        private fun entry(entry: ReportBlock.Entry) {
            ensureSpace(30f)
            textBlock(entry.heading, headingPaint, MARGIN, contentWidth, after = 2f)
            entry.lines.forEach { textBlock(it, bodyPaint, MARGIN + 10f, contentWidth - 10f, after = 1f) }
            if (entry.photos.isNotEmpty()) photos(entry.photos)
            y += 4f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 8f
        }

        private fun photos(names: List<String>) {
            y += 4f
            names.chunked(PHOTOS_PER_ROW).forEach { row ->
                ensureSpace(THUMB_SIZE + 6f)
                var x = MARGIN + 10f
                row.forEach { name ->
                    loadThumbnail(photoFile(name))?.let { bitmap ->
                        val scale = minOf(THUMB_SIZE / bitmap.width, THUMB_SIZE / bitmap.height)
                        val w = bitmap.width * scale
                        val h = bitmap.height * scale
                        canvas.drawBitmap(bitmap, null, RectF(x, y, x + w, y + h), null)
                        bitmap.recycle()
                    }
                    x += THUMB_SIZE + 8f
                }
                y += THUMB_SIZE + 6f
            }
        }

        private fun table(table: ReportBlock.Table) {
            val totalWeight = table.weights.sum()
            val widths = table.weights.map { contentWidth * it / totalWeight }
            ensureSpace(rowHeight(table.headers, widths, cellHeaderPaint) * 2)
            drawRow(table.headers, widths, cellHeaderPaint, HEADER_FILL)
            table.rows.forEachIndexed { index, row ->
                val height = rowHeight(row, widths, cellPaint)
                if (y + height > contentBottom) {
                    newPage()
                    drawRow(table.headers, widths, cellHeaderPaint, HEADER_FILL)
                }
                drawRow(row, widths, cellPaint, if (index % 2 == 1) ZEBRA else null)
            }
            y += 8f
        }

        private fun rowHeight(cells: List<String>, widths: List<Float>, paint: TextPaint): Float =
            cells.mapIndexed { i, text -> layout(text, paint, widths[i] - 2 * CELL_PAD).height.toFloat() }
                .maxOrNull().orZero() + 2 * CELL_PAD

        private fun drawRow(cells: List<String>, widths: List<Float>, paint: TextPaint, fill: Int?) {
            val height = rowHeight(cells, widths, paint)
            if (fill != null) {
                fillPaint.color = fill
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + height, fillPaint)
            }
            var x = MARGIN
            cells.forEachIndexed { i, text ->
                drawLayoutAt(layout(text, paint, widths[i] - 2 * CELL_PAD), x + CELL_PAD, y + CELL_PAD)
                x += widths[i]
            }
            y += height
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        }

        /** Texto quebrado em linhas, com troca de página entre linhas quando preciso. */
        private fun textBlock(text: String, paint: TextPaint, x: Float, width: Float, after: Float) {
            val layout = layout(text, paint, width)
            for (line in 0 until layout.lineCount) {
                val lineTop = layout.getLineTop(line)
                val lineHeight = (layout.getLineBottom(line) - lineTop).toFloat()
                ensureSpace(lineHeight)
                val baseline = y + (layout.getLineBaseline(line) - lineTop)
                canvas.drawText(text, layout.getLineStart(line), layout.getLineEnd(line), x, baseline, paint)
                y += lineHeight
            }
            y += after
        }

        private fun drawLayoutAt(layout: StaticLayout, x: Float, top: Float) {
            canvas.withTranslation(x, top) { layout.draw(this) }
        }

        private fun layout(text: String, paint: TextPaint, width: Float): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

        private fun ensureSpace(height: Float) {
            if (currentCanvas == null || y + height > contentBottom) newPage()
        }

        private fun newPage() {
            closePage()
            pageNumber += 1
            currentCanvas = sink.startPage(pageNumber, PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt())
            y = MARGIN
            if (pageNumber > 1) {
                textBlock("Crescer Bem · Relatório de acompanhamento", smallPaint, MARGIN, contentWidth, after = 8f)
            }
        }

        private fun closePage() {
            if (currentCanvas == null) return
            val footerTop = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT + 10f
            canvas.drawLine(MARGIN, footerTop, PAGE_WIDTH - MARGIN, footerTop, linePaint)
            val pageLabel = "Página $pageNumber"
            val labelWidth = smallPaint.measureText(pageLabel)
            drawLayoutAt(layout(footer, smallPaint, contentWidth - labelWidth - 16f), MARGIN, footerTop + 6f)
            canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN - labelWidth, footerTop + 6f - smallPaint.ascent(), smallPaint)
            sink.finishPage()
            currentCanvas = null
        }

        fun finish() = closePage()

        private fun loadThumbnail(file: File): Bitmap? {
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, THUMB_DECODE_PX)
            }
            return BitmapFactory.decodeFile(file.path, options)
        }

        private fun textPaint(size: Float, bold: Boolean = false, color: Int = INK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        private fun Float?.orZero(): Float = this ?: 0f
    }

    private companion object {
        // A4 em pontos (1/72 pol.).
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val MARGIN = 40f
        const val FOOTER_HEIGHT = 34f
        const val SECTION_BAND = 24f
        const val CELL_PAD = 4f
        const val THUMB_SIZE = 110f
        const val THUMB_DECODE_PX = 330
        const val PHOTOS_PER_ROW = 4

        val INK = Color.rgb(27, 28, 26)
        val MUTED = Color.rgb(100, 104, 102)
        val BRAND = Color.rgb(47, 111, 106)
        val BRAND_DARK = Color.rgb(30, 80, 76)
        val BAND = Color.rgb(219, 238, 234)
        val HEADER_FILL = Color.rgb(234, 240, 238)
        val ZEBRA = Color.rgb(248, 248, 246)
        val RULE = Color.rgb(205, 208, 205)
    }
}

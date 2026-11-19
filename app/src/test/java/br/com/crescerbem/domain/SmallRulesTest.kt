package br.com.crescerbem.domain

import br.com.crescerbem.data.db.Converters
import br.com.crescerbem.data.model.Acceptance
import br.com.crescerbem.data.model.BreastSide
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Symptom
import br.com.crescerbem.photos.ImageSizing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class FeedingSummaryTest {
    private val day = LocalDate.of(2026, 11, 17)
    private fun at(hour: Int, d: LocalDate = day) = d.atTime(hour, 0)

    @Test
    fun `resume mamadas formula agua e refeicoes`() {
        val feedings = listOf(
            Feeding(babyId = 1, dateTime = at(6), type = FeedingType.BREAST_MILK, breastSide = BreastSide.LEFT, durationMinutes = 15),
            Feeding(babyId = 1, dateTime = at(9), type = FeedingType.BREAST_MILK, durationMinutes = 10),
            Feeding(babyId = 1, dateTime = at(12), type = FeedingType.FORMULA, amountMl = 90),
            Feeding(babyId = 1, dateTime = at(15), type = FeedingType.FORMULA, amountMl = 120),
            Feeding(babyId = 1, dateTime = at(16), type = FeedingType.WATER, amountMl = 30),
            Feeding(babyId = 1, dateTime = at(17), type = FeedingType.FRUIT, food = " Banana ", firstTime = true, acceptance = Acceptance.GOOD),
            Feeding(babyId = 1, dateTime = at(18), type = FeedingType.SOLID_MEAL, food = "banana", firstTime = true),
        )
        val s = FeedingSummaries.summarize(feedings)
        assertEquals(7, s.total)
        assertEquals(2, s.breastfeedings)
        assertEquals(25, s.breastMinutes)
        assertEquals(2, s.formulaCount)
        assertEquals(210, s.formulaMl)
        assertEquals(30, s.waterMl)
        assertEquals(2, s.solidMeals)
        assertEquals(listOf("Banana"), s.newFoods)
    }

    @Test
    fun `dia vazio`() {
        assertTrue(FeedingSummaries.summarize(emptyList()).isEmpty)
    }

    @Test
    fun `agrupa por dia em ordem`() {
        val feedings = listOf(
            Feeding(babyId = 1, dateTime = at(8, day.plusDays(1)), type = FeedingType.FORMULA, amountMl = 100),
            Feeding(babyId = 1, dateTime = at(8), type = FeedingType.FORMULA, amountMl = 50),
        )
        val days = FeedingSummaries.byDay(feedings)
        assertEquals(listOf(day, day.plusDays(1)), days.map { it.first })
        assertEquals(50, days.first().second.formulaMl)
    }
}

class PlausibilityTest {
    @Test
    fun `peso em gramas pede confirmacao`() {
        assertNotNull(Plausibility.measurementWarning(MeasurementType.WEIGHT, 3450.0))
        assertNull(Plausibility.measurementWarning(MeasurementType.WEIGHT, 3.45))
    }

    @Test
    fun `comprimentos fora da faixa`() {
        assertNotNull(Plausibility.measurementWarning(MeasurementType.HEIGHT, 5.0))
        assertNull(Plausibility.measurementWarning(MeasurementType.HEIGHT, 52.0))
        assertNotNull(Plausibility.measurementWarning(MeasurementType.HEAD_CIRCUMFERENCE, 80.0))
        assertNull(Plausibility.measurementWarning(MeasurementType.HEAD_CIRCUMFERENCE, 35.0))
    }

    @Test
    fun `temperatura incomum`() {
        assertNotNull(Plausibility.temperatureWarning(378.0))
        assertNull(Plausibility.temperatureWarning(37.8))
    }
}

class CalendarGridTest {
    @Test
    fun `novembro de 2026 comeca no domingo`() {
        val cells = CalendarGrid.cells(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 11, 1), cells.first())
        assertEquals(0, cells.size % 7)
        assertEquals(30, cells.count { it != null })
    }

    @Test
    fun `preenche o inicio ate o dia da semana certo`() {
        // 1º de outubro de 2026 é quinta-feira: 4 células vazias antes (D, S, T, Q).
        val cells = CalendarGrid.cells(YearMonth.of(2026, 10))
        assertEquals(4, cells.takeWhile { it == null }.size)
        assertEquals(DayOfWeek.THURSDAY, cells[4]!!.dayOfWeek)
        assertEquals(35, cells.size)
    }

    @Test
    fun `iniciais dos dias`() {
        assertEquals(listOf("D", "S", "T", "Q", "Q", "S", "S"), CalendarGrid.weekdayInitials())
        assertEquals("segunda-feira", CalendarGrid.weekdayNames(DayOfWeek.MONDAY).first())
    }

    @Test
    fun `marcadores por dia sem repetir tipo`() {
        val day = LocalDate.of(2026, 11, 17)
        val items = listOf(
            TimelineItem(RecordType.FEEDING, 1, day, LocalTime.NOON, "a"),
            TimelineItem(RecordType.FEEDING, 2, day, LocalTime.MIDNIGHT, "b"),
            TimelineItem(RecordType.VACCINE, 3, day, null, "c"),
            TimelineItem(RecordType.MOMENT, 4, day.plusDays(1), null, "d"),
        )
        val markers = CalendarGrid.markersByDay(items)
        assertEquals(setOf(RecordType.FEEDING, RecordType.VACCINE), markers[day])
        assertEquals(setOf(RecordType.MOMENT), markers[day.plusDays(1)])
    }
}

class FormatsTest {
    @Test
    fun `datas no padrao brasileiro`() {
        val d = LocalDate.of(2026, 11, 17)
        assertEquals("17/11/2026", Formats.date(d))
        assertEquals("17/11", Formats.shortDate(d))
        assertEquals("Terça-feira, 17 de novembro", Formats.longDate(d))
        assertEquals("Novembro de 2026", Formats.monthYear(YearMonth.of(2026, 11)))
        assertEquals("17/11/2026 08:05", Formats.dateTime(LocalDateTime.of(2026, 11, 17, 8, 5)))
    }

    @Test
    fun `numeros com virgula`() {
        assertEquals("3,450 kg", Formats.measurement(MeasurementType.WEIGHT, 3.45))
        assertEquals("52,0 cm", Formats.measurement(MeasurementType.HEIGHT, 52.0))
        assertEquals("37,8 °C", Formats.temperature(37.8))
    }

    @Test
    fun `leitura de numeros digitados`() {
        assertEquals(3.45, Formats.parseDecimal("3,45")!!, 0.0001)
        assertEquals(3.45, Formats.parseDecimal(" 3.45 ")!!, 0.0001)
        assertNull(Formats.parseDecimal(""))
        assertNull(Formats.parseDecimal("abc"))
        assertEquals(120, Formats.parseInt("120"))
        assertNull(Formats.parseInt(""))
    }

    @Test
    fun `plural simples`() {
        assertEquals("1 dia", plural(1, "dia", "dias"))
        assertEquals("2 dias", plural(2, "dia", "dias"))
    }
}

class ConvertersTest {
    private val c = Converters()

    @Test
    fun `datas fazem ida e volta`() {
        val date = LocalDate.of(2026, 2, 28)
        assertEquals(date, c.toLocalDate(c.fromLocalDate(date)))
        val dateTime = LocalDateTime.of(2026, 2, 28, 23, 59)
        assertEquals(dateTime, c.toLocalDateTime(c.fromLocalDateTime(dateTime)))
        val time = LocalTime.of(6, 30)
        assertEquals(time, c.toLocalTime(c.fromLocalTime(time)))
        assertNull(c.toLocalDate(null))
    }

    @Test
    fun `ordem das datas e hora preservada como numero`() {
        val earlier = c.fromLocalDateTime(LocalDateTime.of(2026, 1, 1, 23, 59))!!
        val later = c.fromLocalDateTime(LocalDateTime.of(2026, 1, 2, 0, 0))!!
        assertTrue(earlier < later)
    }

    @Test
    fun `sintomas fazem ida e volta e ignoram valores desconhecidos`() {
        val list = listOf(Symptom.FEVER, Symptom.COUGH)
        assertEquals(list, c.toSymptoms(c.fromSymptoms(list)))
        assertEquals(listOf(Symptom.FEVER), c.toSymptoms("FEVER,INEXISTENTE"))
        assertEquals(emptyList<Symptom>(), c.toSymptoms(""))
    }
}

class ImageSizingTest {
    @Test
    fun `amostragem em potencia de 2 sem ficar menor que o limite`() {
        assertEquals(1, ImageSizing.sampleSize(1600, 1200, 1600))
        assertEquals(2, ImageSizing.sampleSize(4000, 3000, 1600))
        assertEquals(4, ImageSizing.sampleSize(8000, 6000, 1600))
        assertEquals(1, ImageSizing.sampleSize(0, 0, 1600))
    }

    @Test
    fun `fator de escala`() {
        assertEquals(1f, ImageSizing.scaleFactor(800, 600, 1600))
        assertEquals(0.8f, ImageSizing.scaleFactor(2000, 1500, 1600), 0.0001f)
    }
}

class TimelineTest {
    @Test
    fun `descricao da alimentacao`() {
        val f = Feeding(
            babyId = 1,
            dateTime = LocalDateTime.of(2026, 11, 17, 9, 0),
            type = FeedingType.BREAST_MILK,
            breastSide = BreastSide.RIGHT,
            durationMinutes = 12,
        )
        assertEquals("lado direito · 12 min", f.describe())
        assertEquals(LocalTime.of(9, 0), f.toTimelineItem().time)
    }

    @Test
    fun `ordem cronologica com registros so de data primeiro`() {
        val day = LocalDate.of(2026, 11, 17)
        val items = listOf(
            TimelineItem(RecordType.FEEDING, 1, day, LocalTime.of(10, 0), "b"),
            TimelineItem(RecordType.GROWTH, 2, day, null, "a"),
            TimelineItem(RecordType.MOMENT, 3, day.minusDays(1), LocalTime.of(23, 0), "z"),
        ).sortedChronologically()
        assertEquals(listOf(3L, 2L, 1L), items.map { it.id })
    }
}

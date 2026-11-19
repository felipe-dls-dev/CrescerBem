package br.com.crescerbem.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BabyAgeTest {
    private val birth = LocalDate.of(2026, 1, 1)

    @Test
    fun `dia do nascimento`() {
        assertEquals("Nasceu hoje", BabyAge.describe(birth, birth))
    }

    @Test
    fun `primeiras duas semanas em dias`() {
        assertEquals("1 dia", BabyAge.describe(birth, birth.plusDays(1)))
        assertEquals("13 dias", BabyAge.describe(birth, birth.plusDays(13)))
    }

    @Test
    fun `ate 59 dias em semanas e dias`() {
        assertEquals("2 semanas", BabyAge.describe(birth, birth.plusDays(14)))
        assertEquals("2 semanas e 2 dias", BabyAge.describe(birth, birth.plusDays(16)))
        assertEquals("8 semanas e 3 dias", BabyAge.describe(birth, birth.plusDays(59)))
    }

    @Test
    fun `primeiro ano em meses e dias`() {
        assertEquals("2 meses e 4 dias", BabyAge.describe(birth, LocalDate.of(2026, 3, 5)))
        assertEquals("6 meses", BabyAge.describe(birth, LocalDate.of(2026, 7, 1)))
        // Fim de mês: 31/01 a 01/04 são 60 dias, contados como 2 meses e 1 dia.
        assertEquals("2 meses e 1 dia", BabyAge.describe(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun `depois de um ano em anos e meses`() {
        assertEquals("1 ano", BabyAge.describe(birth, LocalDate.of(2027, 1, 1)))
        assertEquals("1 ano e 2 meses", BabyAge.describe(birth, LocalDate.of(2027, 3, 10)))
        assertEquals("2 anos e 1 mês", BabyAge.describe(birth, LocalDate.of(2028, 2, 1)))
    }

    @Test
    fun `data futura nao quebra`() {
        assertEquals("Nascimento previsto para 01/01/2026", BabyAge.describe(birth, birth.minusDays(3)))
        assertEquals(0, BabyAge.months(birth, birth.minusDays(3)))
    }

    @Test
    fun `frase do cabecalho sem erro gramatical`() {
        assertEquals("Ana Teste nasceu hoje", BabyAge.sentence("Ana Teste", birth, birth))
        assertEquals("Ana Teste tem 5 dias", BabyAge.sentence("Ana Teste", birth, birth.plusDays(5)))
        assertEquals("Ana Teste tem 1 ano e 2 meses", BabyAge.sentence("Ana Teste", birth, LocalDate.of(2027, 3, 10)))
        assertEquals("Nascimento previsto para 01/01/2026", BabyAge.sentence("Ana Teste", birth, birth.minusDays(1)))
    }

    @Test
    fun `meses completos`() {
        assertEquals(0, BabyAge.months(birth, LocalDate.of(2026, 1, 31)))
        assertEquals(1, BabyAge.months(birth, LocalDate.of(2026, 2, 1)))
        assertEquals(14, BabyAge.months(birth, LocalDate.of(2027, 3, 1)))
    }
}

package br.com.crescerbem.data.repo

import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.RecordKey
import br.com.crescerbem.domain.ReportData
import br.com.crescerbem.domain.TimelineItem
import br.com.crescerbem.domain.sortedChronologically
import br.com.crescerbem.domain.toTimelineItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Junta todos os tipos de registro numa linha do tempo (calendário, Hoje) e carrega dados de relatório. */
class TimelineRepository(
    private val db: AppDatabase,
    private val photos: PhotoRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    fun observeRange(babyId: Long, start: LocalDate, end: LocalDate): Flow<List<TimelineItem>> {
        val from = start.atStartOfDay()
        val to = end.plusDays(1).atStartOfDay()
        val sources: List<Flow<List<TimelineItem>>> = listOf(
            db.growthDao().observeBetween(babyId, start, end).map { list -> list.map { it.toTimelineItem() } },
            db.vaccineDao().observeBetween(babyId, start, end)
                .map { list -> list.mapNotNull { it.toTimelineItem(today()) } },
            db.appointmentDao().observeBetween(babyId, from, to).map { list -> list.map { it.toTimelineItem() } },
            db.healthEventDao().observeBetween(babyId, from, to).map { list -> list.map { it.toTimelineItem() } },
            db.feedingDao().observeBetween(babyId, from, to).map { list -> list.map { it.toTimelineItem() } },
            db.momentDao().observeBetween(babyId, from, to).map { list -> list.map { it.toTimelineItem() } },
            db.medicationDao().observeBetween(babyId, from, to).map { list -> list.map { it.toTimelineItem() } },
        )
        return combine(sources) { lists -> lists.flatMap { it }.sortedChronologically() }
    }

    /** Carrega os registros do período para o relatório (as fotos só se pedidas). */
    suspend fun loadReportData(
        babyId: Long,
        start: LocalDate,
        end: LocalDate,
        includePhotos: Boolean,
    ): ReportData {
        val from = start.atStartOfDay()
        val to = end.plusDays(1).atStartOfDay()
        val photoMap = if (includePhotos) {
            photos.photosByOwner(babyId).mapKeys { (key, _) -> RecordKey(key.first, key.second) }
                .filterKeys { it.type != RecordType.BABY }
        } else {
            emptyMap()
        }
        return ReportData(
            measurements = db.growthDao().getBetween(babyId, start, end),
            vaccines = db.vaccineDao().getBetween(babyId, start, end),
            appointments = db.appointmentDao().getBetween(babyId, from, to),
            healthEvents = db.healthEventDao().getBetween(babyId, from, to),
            feedings = db.feedingDao().getBetween(babyId, from, to),
            moments = db.momentDao().getBetween(babyId, from, to),
            medications = db.medicationDao().getBetween(babyId, from, to),
            photos = photoMap,
        )
    }
}

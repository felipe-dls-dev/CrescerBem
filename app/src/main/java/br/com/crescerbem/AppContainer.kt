package br.com.crescerbem

import android.content.Context
import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.data.repo.BabyRepository
import br.com.crescerbem.data.repo.DataEraser
import br.com.crescerbem.data.repo.PhotoRepository
import br.com.crescerbem.data.repo.RecordRepository
import br.com.crescerbem.data.repo.SharedPrefsActiveBabyStore
import br.com.crescerbem.data.repo.SharedPrefsThemeStore
import br.com.crescerbem.data.repo.ThemeStore
import br.com.crescerbem.data.repo.TimelineRepository
import br.com.crescerbem.photos.PhotoStorage
import br.com.crescerbem.report.PdfReportWriter
import br.com.crescerbem.report.ReportFiles
import br.com.crescerbem.reminders.ReminderStore
import br.com.crescerbem.reminders.ReminderSync

/** Injeção de dependências manual: um único lugar cria e entrega as dependências do app. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val photoStorage: PhotoStorage by lazy { PhotoStorage(appContext) }
    val reportFiles: ReportFiles by lazy { ReportFiles(appContext) }
    private val activeBabyStore by lazy { SharedPrefsActiveBabyStore(appContext) }
    val themeStore: ThemeStore by lazy { SharedPrefsThemeStore(appContext) }

    val photoRepository: PhotoRepository by lazy { PhotoRepository(database.photoDao(), photoStorage) }
    val babyRepository: BabyRepository by lazy { BabyRepository(database, photoRepository, activeBabyStore) }
    val reminderStore: ReminderStore by lazy { ReminderStore(appContext) }
    val reminderSync: ReminderSync by lazy { ReminderSync(appContext, database, reminderStore) }
    val recordRepository: RecordRepository by lazy {
        RecordRepository(database, photoRepository, onRemindersChanged = reminderSync::sync)
    }
    val timelineRepository: TimelineRepository by lazy { TimelineRepository(database, photoRepository) }
    val dataEraser: DataEraser by lazy {
        DataEraser(database, photoRepository, activeBabyStore, reportFiles::deleteAll)
    }
    val pdfWriter: PdfReportWriter by lazy { PdfReportWriter(photoStorage::fileFor) }
}

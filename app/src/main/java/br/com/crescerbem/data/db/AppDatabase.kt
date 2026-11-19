package br.com.crescerbem.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.Photo
import br.com.crescerbem.data.model.Vaccine

@Database(
    entities = [
        Baby::class,
        GrowthMeasurement::class,
        Vaccine::class,
        Appointment::class,
        HealthEvent::class,
        Feeding::class,
        Moment::class,
        Photo::class,
        MedicationDose::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun growthDao(): GrowthDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun healthEventDao(): HealthEventDao
    abstract fun feedingDao(): FeedingDao
    abstract fun momentDao(): MomentDao
    abstract fun photoDao(): PhotoDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        const val NAME = "crescer_bem.db"

        /** v1 → v2: tabela de doses de medicamentos. Não mexe nos dados existentes. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medication_doses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `babyId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `dosage` TEXT, `scheduledAt` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `givenAt` INTEGER, `notes` TEXT, " +
                        "FOREIGN KEY(`babyId`) REFERENCES `babies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_medication_doses_babyId_scheduledAt` " +
                        "ON `medication_doses` (`babyId`, `scheduledAt`)",
                )
            }
        }

        /** v2 → v3: liga as doses criadas por uma mesma repetição. Doses antigas ficam sem série. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `medication_doses` ADD COLUMN `seriesId` INTEGER")
            }
        }

        /** v3 → v4: cor de tema por bebê. Bebês existentes ficam com verde-menta (índice 0). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `babies` ADD COLUMN `colorIndex` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}

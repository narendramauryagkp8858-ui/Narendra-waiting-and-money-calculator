package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Persistent local Room database for calculation history.
 *
 * Update & data-safety guarantee (v1.0 -> v1.1 -> v1.2 -> v1.3 -> v1.4+):
 * - Schema export is enabled so every version's schema is tracked under app/schemas/.
 * - Never call fallbackToDestructiveMigration() here; always supply non-destructive
 *   Migration / @AutoMigration definitions when incrementing the database version.
 */
@Database(entities = [CalculationEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_calculation_history_timestamp` ON `calculation_history` (`timestamp`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_calculation_history_calculationType` ON `calculation_history` (`calculationType`)"
                )
            }
        }

        // Register all schema migrations here so user history is never wiped across updates.
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "narendra_calculator_db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

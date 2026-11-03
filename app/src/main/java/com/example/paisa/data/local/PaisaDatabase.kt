package com.example.paisa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.paisa.BuildConfig
import com.example.paisa.data.local.dao.BankAccountDao
import com.example.paisa.data.local.dao.CategoryDao
import com.example.paisa.data.local.dao.TransactionDao
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity

@Database(
    entities = [
        BankAccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PaisaDatabase : RoomDatabase() {

    abstract fun bankAccountDao(): BankAccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "paisa_database.db"

        /**
         * Safe migration from schema version 1 to 2:
         * Adds stable bankIdentifier and themeId columns to bank_accounts table
         * preserving all existing financial accounts and balances.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN bankIdentifier TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN themeId TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2
        )

        @Volatile
        private var INSTANCE: PaisaDatabase? = null

        fun getInstance(context: Context): PaisaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(appContext: Context): PaisaDatabase {
            val builder = Room.databaseBuilder(
                appContext,
                PaisaDatabase::class.java,
                DATABASE_NAME
            )
            // Apply explicit migrations strategy
            .addMigrations(*ALL_MIGRATIONS)

            // MIGRATION STRATEGY:
            // Destructive migration is strictly forbidden in Release builds.
            // If an unhandled migration occurs in release, Room will throw an IllegalStateException
            // rather than wiping user financial records.
            if (BuildConfig.DEBUG) {
                builder.fallbackToDestructiveMigration(dropAllTables = true)
            }

            return builder.build()
        }
    }
}

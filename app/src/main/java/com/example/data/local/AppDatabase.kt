package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WallpaperEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "pastelka_wallpapers.db",
                            )
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration(dropAllTables = true)
                            .build()
                            .also { instance = it }
                }
        }

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_wallpapers_isFavorite_createdAt " +
                            "ON wallpapers (isFavorite, createdAt)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_wallpapers_category_createdAt " +
                            "ON wallpapers (category, createdAt)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_wallpapers_isUserUploaded_createdAt " +
                            "ON wallpapers (isUserUploaded, createdAt)",
                    )
                }
            }
    }
}

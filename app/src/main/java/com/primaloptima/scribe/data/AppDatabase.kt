package com.primaloptima.scribe.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Folder::class, WorldEntry::class, Book::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun worldEntryDao(): WorldEntryDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 → v2:
         *  - Create `books` table; insert "My Notes" default book for existing data.
         *  - Add `book_id` column to `notes` (DEFAULT 'default').
         *  - Recreate `folders` with composite PK (book_id, path) and copy existing rows.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                // 1. Books table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS books (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        cover_uri TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0,
                        sort_order INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 2. Default book for pre-existing notes
                database.execSQL("""
                    INSERT OR IGNORE INTO books (id, title, cover_uri, created_at, updated_at, sort_order)
                    VALUES ('default', 'My Notes', NULL, $now, $now, 0)
                """.trimIndent())

                // 3. Add book_id to notes
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN book_id TEXT NOT NULL DEFAULT 'default'"
                )

                // 4. Recreate folders with composite PK (book_id, path)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders_new (
                        book_id TEXT NOT NULL,
                        path TEXT NOT NULL,
                        external_uri TEXT,
                        PRIMARY KEY (book_id, path)
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT OR IGNORE INTO folders_new (book_id, path, external_uri)
                    SELECT 'default', path, external_uri FROM folders
                """.trimIndent())
                database.execSQL("DROP TABLE folders")
                database.execSQL("ALTER TABLE folders_new RENAME TO folders")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scribe.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val now = System.currentTimeMillis()
                            // Insert default book
                            db.execSQL("""
                                INSERT OR IGNORE INTO books (id, title, cover_uri, created_at, updated_at, sort_order)
                                VALUES ('default', 'My Notes', NULL, $now, $now, 0)
                            """.trimIndent())
                            // Insert root folder for default book
                            db.execSQL(
                                "INSERT OR IGNORE INTO folders (book_id, path) VALUES ('default', '/')"
                            )
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}

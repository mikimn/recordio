package com.mikimn.recordio.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mikimn.recordio.RegisteredCall
import com.mikimn.recordio.RegisteredCallDao
import com.mikimn.recordio.dummyCalls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Database(version = 1, entities = [RegisteredCall::class], exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callRecordingsDao(): RegisteredCallDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        fun instance(applicationContext: Context, scope: CoroutineScope): AppDatabase {
            Log.d("AppDatabase", "getInstance")
            return synchronized(this) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java, "default-db"
                    ).addCallback(DatabaseCallback(scope))
                        .fallbackToDestructiveMigration()
                        .build()
                }
                instance!!
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("AppDatabase", "onCreate")
            instance?.let { database ->
                scope.launch {
                    populateDatabase(database.callRecordingsDao())
                }
            }
        }

        suspend fun populateDatabase(dao: RegisteredCallDao) {
            Log.d("AppDatabase", "Populating database")
            // Delete all content here.
            dao.deleteAll()

            // Add sample recordings.
            for (call in dummyCalls(50)) {
                dao.insert(call)
            }
        }
    }
}
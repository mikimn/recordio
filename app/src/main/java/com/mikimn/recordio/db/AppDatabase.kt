package com.mikimn.recordio.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mikimn.recordio.model.CallType
import com.mikimn.recordio.model.RegisteredCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.abs


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

        private fun callFromId(id: Int): RegisteredCall {
            return RegisteredCall(
                id,
                "+1-202-555-0108",
                CallType.values()[abs(id) % CallType.values().size],
                Duration.ofMinutes(5).plus(Duration.ofSeconds(23))
            )
        }

        private fun dummyCalls(size: Int): List<RegisteredCall> {
            return (0 until size).map { callFromId(0) }
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
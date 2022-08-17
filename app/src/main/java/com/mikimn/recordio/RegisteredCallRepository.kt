package com.mikimn.recordio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.temporal.Temporal

@Dao
interface RegisteredCallDao {
    @Query("SELECT * FROM registered_calls")
    fun list(): Flow<List<RegisteredCall>>

    @Query("SELECT * FROM registered_calls WHERE id == :id")
    suspend fun get(id: Int): List<RegisteredCall>

    @Insert
    suspend fun insert(registeredCall: RegisteredCall)

    @Delete
    suspend fun delete(registeredCall: RegisteredCall)

    @Query("DELETE FROM registered_calls")
    suspend fun deleteAll()
}

/**
 * Manages saving/retrieving of [RegisteredCall]s to a
 */
class RegisteredCallsRepository(private val dao: RegisteredCallDao) {
    val calls: Flow<List<RegisteredCall>> = dao.list()

    suspend fun findById(id: Int): RegisteredCall =
        dao.get(id)
            .getOrNull(0) ?: throw IllegalArgumentException("No recording with id $id found")

    suspend fun delete(callRecording: RegisteredCall) = dao.delete(callRecording)

    suspend fun insert(number: String, type: CallType, startTime: Temporal, endTime: Temporal) {
        val recording = RegisteredCall(
            id = 0, // Auto-generate
            source = number,
            callType = type,
            duration = Duration.between(startTime, endTime)
        )
        dao.insert(recording)
    }
}
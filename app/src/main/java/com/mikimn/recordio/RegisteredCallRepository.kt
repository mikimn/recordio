package com.mikimn.recordio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Duration

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

    // TODO Add duration support
    suspend fun insert(number: String, type: CallType) {
        val recording = RegisteredCall(
            id = 0, // Auto-generate
            source = number,
            callType = type,
            // TODO Replace with real implementation
            duration = Duration.ofSeconds(10)
        )
        dao.insert(recording)
    }
}
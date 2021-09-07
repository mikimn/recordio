package com.mikimn.recordio
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.time.Duration
import kotlin.math.abs
import androidx.lifecycle.*
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED;

    class Converter {
        @TypeConverter
        fun toCallType(type: Int) = values()[type]

        @TypeConverter
        fun fromCallType(type: CallType): Int = type.ordinal
    }
}

fun callRecordingFromId(id: Int): CallRecording {
    return CallRecording(
        id,
        "+1-202-555-0108",
        CallType.values()[abs(id) % CallType.values().size],
        Duration.ofMinutes(5).plus(Duration.ofSeconds(23)),
        "some-file.mp4"
    )
}

fun dummyCallRecordings(size: Int): List<CallRecording> {
    return (0 until size).map { callRecordingFromId(it) }
}

class DurationConverter {
    @TypeConverter
    fun toDuration(durationSeconds: Long): Duration = Duration.ofSeconds(durationSeconds)

    @TypeConverter
    fun fromDuration(duration: Duration) = duration.seconds
}


@Entity(tableName = "recordings")
@TypeConverters(CallType.Converter::class, DurationConverter::class)
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "call_type") val callType: CallType,
    @ColumnInfo(name = "duration") val duration: Duration,
    @ColumnInfo(name = "file_path") val filePath: String
)


@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM recordings")
    fun list(): Flow<List<CallRecording>>

    @Query("SELECT * FROM recordings WHERE id == :id")
    suspend fun get(id: Int): List<CallRecording>

    @Insert
    suspend fun insert(callRecording: CallRecording)

    @Delete
    suspend fun delete(callRecording: CallRecording)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()
}


class CallRecordingsRepository(private val dao: CallRecordingDao) {
    val recordings: Flow<List<CallRecording>> = dao.list()

    suspend fun findById(id: Int): CallRecording =
        dao.get(id)
            .getOrNull(0) ?: throw IllegalArgumentException("No recording with id $id found")

    suspend fun delete(callRecording: CallRecording) = dao.delete(callRecording)
}


class CallRecordingsViewModel(
    private val repository: CallRecordingsRepository
) : ViewModel() {

    val recordings: LiveData<List<CallRecording>> = repository.recordings.asLiveData()

    suspend fun findById(id: Int) = repository.findById(id)

    fun delete(callRecording: CallRecording, context: Context? = null) = viewModelScope.launch {
        context?.let { callRecording.documentFile(it) }?.delete()
        repository.delete(callRecording)
    }
}


class CallRecordingsViewModelFactory(
    private val repository: CallRecordingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallRecordingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallRecordingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


fun CallRecording.documentFile(context: Context): DocumentFile? {
    return DocumentFile.fromSingleUri(context, Uri.parse(filePath))
}
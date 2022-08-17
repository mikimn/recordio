package com.mikimn.recordio.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mikimn.recordio.db.Converters
import java.time.Duration


enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}


@Entity(tableName = "registered_calls")
@TypeConverters(Converters.CallTypeConverter::class, Converters.DurationConverter::class)
data class RegisteredCall(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "call_type") val callType: CallType,
    @ColumnInfo(name = "duration") val duration: Duration,
)




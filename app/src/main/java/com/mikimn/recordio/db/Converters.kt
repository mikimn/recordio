package com.mikimn.recordio.db

import androidx.room.TypeConverter
import com.mikimn.recordio.model.CallType
import java.time.Duration


object Converters {
    /**
     * Room converter for [Duration] objects.
     */
    class DurationConverter {
        @TypeConverter
        fun toDuration(durationSeconds: Long): Duration = Duration.ofSeconds(durationSeconds)

        @TypeConverter
        fun fromDuration(duration: Duration) = duration.seconds
    }


    /**
     * Room converter for [CallType] enum values.
     */
    class CallTypeConverter {
        @TypeConverter
        fun toCallType(type: Int) = CallType.values()[type]

        @TypeConverter
        fun fromCallType(type: CallType): Int = type.ordinal
    }
}

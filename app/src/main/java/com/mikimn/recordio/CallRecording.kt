package com.mikimn.recordio

import java.time.Duration
import kotlin.math.abs


enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}


data class CallRecording(
    val id: Int,
    val source: String,
    val callType: CallType,
    val duration: Duration,
    val filePath: String
)


fun dummyCallRecordings(size: Int): List<CallRecording> {
    return (0 until size).map { callRecordingFromId(it) }
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
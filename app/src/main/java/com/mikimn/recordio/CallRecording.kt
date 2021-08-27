package com.mikimn.recordio

import java.time.Duration

data class CallRecording(
    val source: String,
    val duration: Duration,
    val filePath: String
)


fun dummyCallRecordings(size: Int): List<CallRecording> {
    return generateSequence {
        CallRecording(
            "+1-202-555-0108",
            Duration.ofMinutes(5).plus(Duration.ofSeconds(23)),
            "some-file.mp4"
        )
    }.take(size).toList()
}
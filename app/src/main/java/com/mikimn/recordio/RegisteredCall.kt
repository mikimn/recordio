package com.mikimn.recordio

import java.time.Duration
import kotlin.math.abs

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

data class RegisteredCall(
    val id: Int,
    val source: String,
    val callType: CallType,
    val duration: Duration
)

fun callFromId(id: Int): RegisteredCall {
    return RegisteredCall(
        id,
        "+1-202-555-0108",
        CallType.values()[abs(id) % CallType.values().size],
        Duration.ofMinutes(5).plus(Duration.ofSeconds(23))
    )
}

fun dummyCalls(size: Int): List<RegisteredCall> {
    return (0 until size).map { callFromId(it) }
}
package com.mikimn.recordio.phone

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.widget.Toast
import com.mikimn.recordio.CallType
import com.mikimn.recordio.recording.RecordingService


sealed class PhoneCall {
    data class Incoming(
        val state: String,
        val number: String?
    ): PhoneCall()

    data class Outgoing(
        val number: String?
    ): PhoneCall()

    object Unknown : PhoneCall()
}


fun Intent.phoneCallInformation(): PhoneCall {
    val action = action
    val extras = extras
    if (extras != null) {
        if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            // Incoming Call
            val state = getStringExtra(TelephonyManager.EXTRA_STATE)!!
            if (hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                val number = getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)!!

                return PhoneCall.Incoming(state, number)
                // handleIncomingCall(context, state, number)
            }
        } else if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            val number = getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            return PhoneCall.Outgoing(number)
            // handleOutgoingCall(context, number)
        }
    }
    return PhoneCall.Unknown
}

fun handleIncomingCall(context: Context, phoneCall: PhoneCall.Incoming) {
    val state = phoneCall.state
    val number = phoneCall.number
    if (state == TelephonyManager.EXTRA_STATE_RINGING) {
        Toast.makeText(context, "Incoming: $number", Toast.LENGTH_SHORT).show()
    } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
        Toast.makeText(context, "Answered", Toast.LENGTH_SHORT).show()
        // Start Recording
        val intent = RecordingService.newRecording(
            context,
            number,
            CallType.INCOMING
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
        Toast.makeText(context, "Reject || Disconnected", Toast.LENGTH_SHORT).show()
        // Stop Recording
        context.stopService(RecordingService.stopRecording(context))
    }
}

fun handleOutgoingCall(context: Context, phoneCall: PhoneCall.Outgoing) {
    val number = phoneCall.number
    Toast.makeText(context, "Outgoing: $number", Toast.LENGTH_SHORT).show()
    // Start Recording
}
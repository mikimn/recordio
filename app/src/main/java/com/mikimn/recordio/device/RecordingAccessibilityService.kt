package com.mikimn.recordio.device

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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

class RecordingAccessibilityService : AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        bindCallBroadcastReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindCallBroadcastReceiver()
    }

    private fun bindCallBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL)
        registerReceiver(CallBroadcastReceiver(), filter)
        Log.d("RecordingService", "bindCallBroadcastReceiver")
    }

    private fun unbindCallBroadcastReceiver() {
        unregisterReceiver(CallBroadcastReceiver())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // TODO Stub
    }

    override fun onInterrupt() {
        // TODO Stub
    }

    inner class CallBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(_c: Context, intent: Intent) {
            val context = this@RecordingAccessibilityService
            when (val phoneCall = intent.phoneCallInformation()) {
                is PhoneCall.Incoming -> handleIncomingCall(context, phoneCall)
                is PhoneCall.Outgoing -> handleOutgoingCall(context, phoneCall)
            }
        }
    }
}



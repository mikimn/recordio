package com.mikimn.recordio.calls

import android.Manifest
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission

/**
 * Algebraic data type that describes all possible states of a phone call.
 * A phone call is either [Incoming], [Outgoing], or [Unknown] in any other undefined case.
 */
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


/**
 * Try to extract phone call information from an intent. Should be used when an intent is
 * passed to a broadcast receiver that is registered to receive call system events.
 *
 * @return A [PhoneCall] object representing the type of call, or [PhoneCall.Unknown] if a
 * phone call could not be recognized.
 */
@RequiresPermission(allOf = [
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.PROCESS_OUTGOING_CALLS
])
fun Intent.phoneCallInformation(): PhoneCall {
    val action = action
    val extras = extras
    if (extras != null) {
        if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            // Incoming Call
            val state = getStringExtra(TelephonyManager.EXTRA_STATE)!!
            if (hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) && state == TelephonyManager.EXTRA_STATE_RINGING) {
                val number = getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)!!

                return PhoneCall.Incoming(state, number)
            }
        } else if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            val number = getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            return PhoneCall.Outgoing(number)
        }
    }
    return PhoneCall.Unknown
}

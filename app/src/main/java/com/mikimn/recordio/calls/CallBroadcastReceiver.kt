package com.mikimn.recordio.calls

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.mikimn.recordio.RegisteredCallsRepository
import com.mikimn.recordio.db.AppDatabase
import com.mikimn.recordio.system.checkPermissions

class CallBroadcastReceiver : BroadcastReceiver() {
    private fun handleIncomingCall(context: Context, phoneCall: PhoneCall.Incoming) {
        Toast.makeText(
            context,
            "Incoming: ${phoneCall.number}, state=${phoneCall.state}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleOutgoingCall(context: Context, phoneCall: PhoneCall.Outgoing) {
        Toast.makeText(
            context,
            "Outgoing: ${phoneCall.number}",
            Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.checkPermissions(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.READ_PHONE_STATE
            )
        ) {
            return
        }
        when (val phoneCall = intent.phoneCallInformation()) {
            is PhoneCall.Incoming -> handleIncomingCall(context, phoneCall)
            is PhoneCall.Outgoing -> handleOutgoingCall(context, phoneCall)
            else -> {}
        }
    }
}
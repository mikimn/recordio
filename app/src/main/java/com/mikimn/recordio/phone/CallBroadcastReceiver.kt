package com.mikimn.recordio.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber: String =
            resultData ?: intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return


    }
}
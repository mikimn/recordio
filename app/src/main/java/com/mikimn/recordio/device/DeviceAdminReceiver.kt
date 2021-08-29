package com.mikimn.recordio.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mikimn.recordio.recording.RecordingService

class DeviceAdminReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recordIntent = Intent(context, RecordingService::class.java)
        context.stopService(recordIntent)
        context.startService(recordIntent)
    }
}
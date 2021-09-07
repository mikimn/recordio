package com.mikimn.recordio.recording

import android.app.Service
import android.content.Context
import com.mikimn.recordio.CallType

interface CallRecordingService {
    fun beginCallRecording(context: Context, phoneNumber: String?, callType: CallType): Boolean
    fun endCallRecording(attachedService: Service? = null)
}
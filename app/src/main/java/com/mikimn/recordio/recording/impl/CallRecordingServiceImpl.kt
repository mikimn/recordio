package com.mikimn.recordio.recording.impl

import android.app.Service
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.documentfile.provider.DocumentFile
import com.mikimn.recordio.CallRecording
import com.mikimn.recordio.CallType
import com.mikimn.recordio.db.AppDatabase
import com.mikimn.recordio.recording.CallRecordingService
import com.mikimn.recordio.recording.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

class CallRecordingServiceImpl(
    context: Context
) : CallRecordingService {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val dao = AppDatabase.instance(context, scope).callRecordingsDao()
    private val recordingRepository = RecordingRepository()

    private var phoneNumber: String? = null
    private var callType: CallType? = null

    override fun beginCallRecording(context: Context, phoneNumber: String?, callType: CallType): Boolean {
        val sharedPrefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        val uriString = sharedPrefs.getString("saveFolder", null)

        return if (uriString == null) {
            false
        } else {
            Log.d("Service", uriString)
            val directory = DocumentFile.fromTreeUri(context, Uri.parse(uriString))!!
            Log.d("Service", "canWrite = ${directory.canWrite()}")
            Log.d("Service", "name = ${directory.name}")
            try {
                recordingRepository.startRecording(
                    context,
                    directory,
                    RecordingRepository.MicrophoneType.CALL
                )
                this.phoneNumber = phoneNumber
                this.callType = callType
                true
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
                insertCallRecording(phoneNumber ?: "Unknown Caller", callType, "Could not record")
                false
            }
        }
    }

    override fun endCallRecording(attachedService: Service?) {
        val type = callType
            ?: throw IllegalStateException(
                "Call recording not in progress. Call beginCallRecording() first"
            )
        val number = phoneNumber ?: "Unknown Caller"
        if (recordingRepository.isRecording) {
            val file = recordingRepository.stopRecording()
            attachedService?.let { ServiceCompat.stopForeground(it, ServiceCompat.STOP_FOREGROUND_REMOVE) }
            val filePath = file.uri.toString()
            insertCallRecording(number, type, filePath)
        }
    }

    private fun insertCallRecording(number: String, type: CallType, filePath: String) {
        val recording = CallRecording(
            id = 0, // Auto-generate
            source = number,
            callType = type,
            duration = Duration.ofSeconds(10), // FIXME
            filePath = filePath
        )
        scope.launch {
            dao.insert(recording)
        }
    }
}
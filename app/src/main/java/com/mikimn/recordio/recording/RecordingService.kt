package com.mikimn.recordio.recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.mikimn.recordio.CallType
import com.mikimn.recordio.MainActivity
import com.mikimn.recordio.R
import com.mikimn.recordio.recording.impl.CallRecordingServiceImpl


class RecordingService : Service() {
    companion object {
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val CHANNEL_DEFAULT_IMPORTANCE = "general"

        private const val EXTRA_PHONE_NUMBER = "phoneNumber"
        private const val EXTRA_CALL_TYPE = "callType"

        fun newRecording(context: Context, phoneNumber: String?, callType: CallType): Intent {
            return Intent(context, RecordingService::class.java).apply {
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber ?: "Unknown")
                putExtra(EXTRA_CALL_TYPE, callType.ordinal)
            }
        }

        fun stopRecording(context: Context): Intent {
            return Intent(context, RecordingService::class.java)
        }
    }

    private lateinit var callRecordingService: CallRecordingService

    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        callRecordingService.endCallRecording(this)
    }

    override fun onCreate() {
        super.onCreate()
        callRecordingService = CallRecordingServiceImpl(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)!!
            val callType = CallType.values()[intent.getIntExtra(EXTRA_CALL_TYPE, 0)]

            startForegroundWithNotification()

            if (!callRecordingService.beginCallRecording(this, phoneNumber, callType)) {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_LONG).show()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                val intentFlags =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else 0
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    intentFlags
                )
            }

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, "Recording Notification")
            } else ""
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getText(R.string.recording_notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_baseline_record_voice_over_24)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        channel.lightColor = Color.BLUE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }
}
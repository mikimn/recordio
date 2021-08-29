package com.mikimn.recordio.recording

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class RecordingService : Service() {
    var recorder: MediaRecorder? = null
    var audiofile: File? = null
    var name: String? = null
    private var recordstarted = false
    private var br_call: CallBr? = null

    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("service", "destroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // final String terminate =(String)
        // intent.getExtras().get("terminate");//
        // intent.getStringExtra("terminate");
        // Log.d("TAG", "service started");
        //
        // TelephonyManager telephony = (TelephonyManager)
        // getSystemService(Context.TELEPHONY_SERVICE); // TelephonyManager
        // // object
        // CustomPhoneStateListener customPhoneListener = new
        // CustomPhoneStateListener();
        // telephony.listen(customPhoneListener,
        // PhoneStateListener.LISTEN_CALL_STATE);
        // context = getApplicationContext();
        val filter = IntentFilter()
        filter.addAction(ACTION_OUT)
        filter.addAction(ACTION_IN)
        br_call = CallBr()
        this.registerReceiver(br_call, filter)

        // if(terminate != null) {
        // stopSelf();
        // }
        return START_NOT_STICKY
    }

    inner class CallBr : BroadcastReceiver() {
        var bundle: Bundle? = null
        var state: String? = null
        var inCall: String? = null
        var outCall: String? = null
        var wasRinging = false
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == ACTION_IN) {
                if (intent.extras.also { bundle = it } != null) {
                    state = bundle!!.getString(TelephonyManager.EXTRA_STATE)
                    if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                        inCall = bundle!!.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        wasRinging = true
                        Toast.makeText(context, "IN : $inCall", Toast.LENGTH_LONG).show()
                    } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        if (wasRinging == true) {
                            Toast.makeText(context, "ANSWERED", Toast.LENGTH_LONG).show()
                            val out: String = SimpleDateFormat("dd-MM-yyyy hh-mm-ss").format(Date())
                            val sampleDir = File(
                                Environment.getExternalStorageDirectory(),
                                "/TestRecordingDasa1"
                            )
                            if (!sampleDir.exists()) {
                                sampleDir.mkdirs()
                            }
                            val file_name = "Record"
                            try {
                                audiofile = File.createTempFile(file_name, ".amr", sampleDir)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            val path: String =
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                            recorder = MediaRecorder()
                            //                          recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                            recorder!!.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                            recorder!!.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                            recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                            recorder!!.setOutputFile(audiofile!!.getAbsolutePath())
                            try {
                                recorder!!.prepare()
                            } catch (e: IllegalStateException) {
                                e.printStackTrace()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            recorder!!.start()
                            recordstarted = true
                        }
                    } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                        wasRinging = false
                        Toast.makeText(context, "REJECT || DISCO", Toast.LENGTH_LONG).show()
                        if (recordstarted) {
                            recorder!!.stop()
                            recordstarted = false
                        }
                    }
                }
            } else if (intent.action == ACTION_OUT) {
                if (intent.extras.also { bundle = it } != null) {
                    outCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                    Toast.makeText(context, "OUT : $outCall", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        private const val ACTION_IN = TelephonyManager.ACTION_PHONE_STATE_CHANGED
        private const val ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL"
    }
}
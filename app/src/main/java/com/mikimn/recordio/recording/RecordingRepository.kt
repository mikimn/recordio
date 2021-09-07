package com.mikimn.recordio.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread


class RecordingRepository {
    companion object {
        private const val SAMPLE_RATE_HZ = 44100
    }

    enum class MicrophoneType(val micType: Int) {
        MIC(MediaRecorder.AudioSource.MIC),
        // VOICE_UPLINK => AudioRecord initialization error
        // VOICE_DOWNLINK => AudioRecord initialization error
        // VOICE_COMMUNICATION => No sound
        // MIC => Sound without call, No sound in call
        // CAMCORDER => No sound
        CALL(
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                    MediaRecorder.AudioSource.VOICE_CALL
                }
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N -> {
                    MediaRecorder.AudioSource.MIC
                }
                else -> {
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
                }
            }
        );
    }

    private val recordState = RecordState()

    private class RecordState {
        var audioRecord: AudioRecord? = null
        var isRecording = false
            private set
        var file: DocumentFile? = null
            private set
        private var totalSize = 0
        private var outputStream: OutputStream? = null

        fun recording(outputStream: OutputStream, file: DocumentFile) {
            this.isRecording = true
            this.totalSize = 0
            this.outputStream = outputStream
            this.file = file
        }

        fun accumulate(size: Int) {
            totalSize += size
        }

        fun stop(): Int {
            audioRecord = audioRecord?.let {
                try {
                    it.stop()
                    it.release()
                } catch (ignored: IllegalStateException) {
                }
                null
            }
            isRecording = false
            file = null
            try {
                Log.d("RecordState", "totalSize = $totalSize")
                outputStream = outputStream?.let {
                    (it as FileOutputStream).apply {
                        writeWaveInputSize(totalSize)
                        close()
                    }
                    null
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            return totalSize
        }
    }

    private fun buildAudioRecorder(
        context: Context,
        audioSource: Int = MediaRecorder.AudioSource.VOICE_CALL
    ): Pair<AudioRecord, Int> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            throw IllegalStateException("No permission to record audio.")
        }

        try {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val sampleRate = SAMPLE_RATE_HZ
            val bufferSize = 10 * AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat
            )
            val audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            when (audioRecord.state) {
                AudioRecord.STATE_INITIALIZED -> return audioRecord to bufferSize
                else -> throw IllegalStateException("Error initializing AudioRecord")
            }
        } catch (ex: SecurityException) {
            throw IllegalStateException("Must have the ${Manifest.permission.RECORD_AUDIO} permission!")
        }
    }

    private fun doRecord(
        recorder: AudioRecord,
        bufferSize: Int,
        outputStream: FileOutputStream,
    ) {
        thread {
            outputStream.appendWaveHeader(recorder.channelCount, recorder.sampleRate)
            recorder.startRecording()
            val buffer = ByteArray(bufferSize)
            while (recordState.isRecording) {
                val result = recorder.read(buffer, 0, buffer.size)
                try {
                    if (result == AudioRecord.ERROR ||
                        result == AudioRecord.ERROR_BAD_VALUE ||
                        result == AudioRecord.ERROR_INVALID_OPERATION
                    ) {
                        stopRecording()
                        break
                    }
                    val bytes = ByteArray(buffer.size)
                    ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).get(bytes)
                    recordState.accumulate(result)
                    outputStream.write(bytes)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Start recording the given [source], and save the recording file in [directory]. The file
     * name is determined by the call time, and is an implementation detail. The resulting file
     * is returned by calling [stopRecording].
     * @throws IllegalStateException If the application does not have the
     * [Manifest.permission.RECORD_AUDIO] permission, or the [AudioRecord] device failed to
     * initialize.
     */
    fun startRecording(
        context: Context,
        directory: DocumentFile,
        source: MicrophoneType
    ) {
        val firstApiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
        val date = firstApiFormat.format(LocalDateTime.now())
        val file = directory.createFile("audio/wav", "${date}.wav")
        return file?.uri?.let { uri ->
            context.contentResolver.openFileDescriptor(
                uri, "w"
            )?.let {
                val (recorder, bufferSize) = buildAudioRecorder(context, source.micType)
                recordState.audioRecord = recorder
                val os = FileOutputStream(it.fileDescriptor)
                recordState.recording(os, file)
                doRecord(recorder, bufferSize, os)
            }
        } ?: throw IOException("Could not open file")
    }

    val isRecording: Boolean
        get() = recordState.isRecording

    // TODO @mikimn: Why is this called twice?
    /**
     * Stops the recording (if [startRecording] was previously called) and returns the
     * [DocumentFile] where the recording was saved.
     */
    fun stopRecording(): DocumentFile {
        val file = recordState.file!!
        recordState.stop()
        return file
    }
}


/**
 * Write the inputSize parameters to the Wave header of a FileOutputStream
 */
private fun FileOutputStream.writeWaveInputSize(inputSize: Int) {
    val position = channel.position()
    channel.position(4)
    writeInt(36 + inputSize)
    channel.position(40)
    writeInt(inputSize)
    channel.position(position)
}


/**
 * Append a WAV (Waveform) header at the current position of the [OutputStream].
 * The WAV header has the following structure:
 *
 * | Name              | Size (Bytes) | Type   | Value                           |
 * |-------------------|--------------|--------|---------------------------------|
 * | RIFF Chunk ID     | 4            | String | "RIFF"                          |
 * | RIFF Chunk Size   | 4            | Int    | 36 + inputSize                  |
 * | Format            | 4            | String | "WAVE"                          |
 * | Sub-chunk #1 ID   | 4            | String | "fmt "                          |
 * | Sub-chunk #1 Size | 4            | Int    | 16                              |
 * | Audio Format      | 2            | Short  | 1 (PCM)                         |
 * | Channel Count     | 2            | Short  | [channelCount]                  |
 * | Sample Rate       | 4            | Int    | [sampleRate]                    |
 * | Byte Rate         | 4            | Int    |                                 |
 * | Block Align       | 2            | Short  |                                 |
 * | Bits Per Sample   | 2            | Short  | [bitsPerSample] (16 for PCM-16) |
 * | Sub-chunk #2 ID   | 4            | String | "data"                          |
 * | Sub-chunk #2 Size | 4            | Int    | inputSize                       |
 */
private fun OutputStream.appendWaveHeader(
    channelCount: Int,
    sampleRate: Int,
    bitsPerSample: Int = 16, // PCM 16BIT
) {
    // WAVE RIFF header
    writeString("RIFF") // chunk id (4)
    writeInt(0) // chunk size (4)
    writeString("WAVE") // format (4)
    // SUB CHUNK 1 (FORMAT)
    writeString("fmt ") // subchunk 1 id (4)
    writeInt(16) // subchunk 1 size (4)
    writeShort(1.toShort()) // audio format [1 = PCM] (2)
    writeShort(channelCount.toShort()) // number of channelCount (2)
    writeInt(sampleRate) // sample rate (4)
    writeInt(sampleRate * channelCount * bitsPerSample / 8) // byte rate (4)
    writeShort((channelCount * bitsPerSample / 8).toShort()) // block align (2)
    writeShort(bitsPerSample.toShort()) // bits per sample (2)
    // SUB CHUNK 2 (AUDIO DATA)
    writeString("data") // subchunk 2 id (4)
    writeInt(0) // subchunk 2 size (40)
}

private fun OutputStream.writeString(data: String) {
    for (element in data) write(element.code)
}

private fun OutputStream.writeShort(data: Short) {
    write((data.toInt() and 0xFF))
    write((data.toInt() shr 8) and 0xFF)
}

private fun OutputStream.writeInt(data: Int) {
    var i = data
    write((i and 0xFF))
    i = i shr 8
    write((i and 0xFF))
    i = i shr 8
    write((i and 0xFF))
    i = i shr 8
    write((i and 0xFF))
}

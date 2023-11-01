package com.example.tfliteaudio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class Recorder {
    private val TAG = "RecordingThread"
    private val isRecording = AtomicBoolean(false)

    suspend fun record(filesDir: String) = suspendCancellableCoroutine<String> {
        try {
            isRecording.set(true)
            val channels = 1
            val bytesPerSample = 2
            val sampleRateInHz = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO // as per channels
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT // as per bytesPerSample
            val audioSource = MediaRecorder.AudioSource.MIC
            val bufferSize =
                AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
            val audioRecord =
                AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSize)
            audioRecord.startRecording()
            val durationInSeconds = 30
            val bufferSize30Sec = durationInSeconds * sampleRateInHz * bytesPerSample * channels
            val byteBuffer = ByteBuffer.allocateDirect(bufferSize30Sec)
            var totalBytesRead = 0
            val buffer = ByteArray(bufferSize)
            while (isRecording.get() && totalBytesRead < bufferSize30Sec) {
                val bytesRead = audioRecord.read(buffer, 0, bufferSize)
                if (bytesRead > 0) {
                    byteBuffer.put(buffer, 0, bytesRead)
                } else {
                    Log.d(TAG, "AudioRecord error, bytes read: $bytesRead")
                }
                totalBytesRead = totalBytesRead + bytesRead
            }
            audioRecord.stop()
            audioRecord.release()
            isRecording.set(false)
            val wavePath = filesDir + File.separator + WaveUtil.RECORDING_FILE
            WaveUtil.createWaveFile(
                wavePath,
                byteBuffer.array(),
                sampleRateInHz,
                channels,
                bytesPerSample
            )
            Log.d(TAG, "Recorded file: $wavePath")
            it.resumeWith(Result.success(wavePath))
//            mUpdateListener.updateStatus(context.getString(R.string.recording_is_completed))
        } catch (e: SecurityException) {
            it.resumeWith(Result.failure(RuntimeException("Writing of recorded audio failed", e)))
        } catch (e: Exception) {
            it.resumeWith(Result.failure(RuntimeException("Writing of recorded audio failed", e)))
        }
    }

    fun stop() {
        isRecording.set(false)
    }
}

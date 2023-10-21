package com.example.tfliteaudio

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.suspendCoroutine

class TranscriptionEngine {
    private val TAG = "TranscriptionCoroutine"

    //    private val updateListener: UpdateListener
    private val tfLiteEngine = TFLiteEngine()

    suspend fun transcribeInput(filesDir: String): String = suspendCoroutine {
        val TAG = "TranscriptionThread"
        try {
            // Initialize TFLiteEngine
            if (!tfLiteEngine.isInitialized) {
                // Update progress to UI thread
//                updateListener.updateStatus(context.getString(R.string.loading_model_and_vocab))

                // set true for multilingual support
                // whisper.tflite => not multilingual
                // whisper-small.tflite => multilingual
                // whisper-tiny.tflite => multilingual
                val isMultilingual = true

                // Get Model and vocab file paths
                val modelPath: String
                val vocabPath: String
                if (isMultilingual) {
                    modelPath = getFilePath(filesDir, "whisper-tiny.tflite")
                    vocabPath = getFilePath(filesDir, "filters_vocab_multilingual.bin")
                } else {
                    modelPath = getFilePath(filesDir, "whisper-tiny-en.tflite")
                    vocabPath = getFilePath(filesDir, "filters_vocab_gen.bin")
                }
                tfLiteEngine.initialize(isMultilingual, vocabPath, modelPath)
            }

            // Get Transcription
            if (tfLiteEngine.isInitialized) {
                val wavePath = getFilePath(filesDir, WaveUtil.RECORDING_FILE)
                Log.d(TAG, "WaveFile: $wavePath")
                if (File(wavePath).exists()) {
                    // Update progress to UI thread
                    val startTime = System.currentTimeMillis()

                    // Get transcription from wav file
                    val result = tfLiteEngine.getTranscription(wavePath)

                    // Display output result

                    val endTime = System.currentTimeMillis()
                    val timeTaken = endTime - startTime
                    Log.d(TAG, "Time Taken for transcription: " + timeTaken + "ms")
                    Log.d(TAG, "Result len: " + result.length + ", Result: " + result)
                    it.resumeWith(Result.success(result))
                } else {
                    it.resumeWith(Result.failure(FileNotFoundException(wavePath)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error..", e)
            it.resumeWith(Result.failure(e))
        }
    }

    // Copies specified asset to app's files directory and returns its absolute path.
    private fun getFilePath(filesDir: String, assetName: String): String {
        val outfile = File(filesDir, assetName)
        if (!outfile.exists()) {
            Log.d(TAG, "File not found - " + outfile.absolutePath)
        }
        Log.d(TAG, "Returned asset path: " + outfile.absolutePath)
        return outfile.absolutePath
    }

    companion object {
        fun initializeModels(context: ComponentActivity) {
            context.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // Call the method to copy specific file types from assets to data folder
                    val extensionsToCopy = arrayOf("pcm", "bin", "wav", "tflite")
                    copyAssetsWithExtensionsToDataFolder(context, extensionsToCopy)
                }
            }
        }

        private fun copyAssetsWithExtensionsToDataFolder(
            context: Context,
            extensions: Array<String>
        ) {
            val assetManager = context.assets
            try {
                // Specify the destination directory in the app's data folder
                val destFolder = context.filesDir.absolutePath
                for (extension in extensions) {
                    // List all files in the assets folder with the specified extension
                    val assetFiles = assetManager.list("")
                    for (assetFileName in assetFiles!!) {
                        if (assetFileName.endsWith(".$extension")) {
                            val inputStream = assetManager.open(assetFileName)
                            val outFile = File(destFolder, assetFileName)
                            val outputStream: OutputStream = FileOutputStream(outFile)

                            // Copy the file from assets to the data folder
                            val buffer = ByteArray(1024)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                            }
                            inputStream.close()
                            outputStream.flush()
                            outputStream.close()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

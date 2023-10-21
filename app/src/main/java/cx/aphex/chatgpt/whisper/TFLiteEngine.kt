package com.example.tfliteaudio

import android.util.Log
import cx.aphex.chatgpt.whisper.WhisperUtil
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.min

class TFLiteEngine {
    private val TAG = "TFLiteEngine"
    var isInitialized = false
        private set
    private val mWhisper = WhisperUtil()
    private var mInterpreter: Interpreter? = null

    @Throws(IOException::class)
    fun initialize(multilingual: Boolean, vocabPath: String, modelPath: String): Boolean {

        // Load model
        loadModel(modelPath)
        Log.d(TAG, "Model is loaded...!$modelPath")

        // Load filters and vocab
        loadFiltersAndVocab(multilingual, vocabPath)
        Log.d(TAG, "Filters and Vocab are loaded...!")
        this.isInitialized = true
        return true
    }

    fun getTranscription(wavePath: String): String {

        // Calculate Mel spectrogram
        Log.d(TAG, "Calculating Mel spectrogram...")
        val melSpectrogram = getMelSpectrogram(wavePath)
        Log.d(TAG, "Mel spectrogram is calculated...!")

        // Perform inference
        val result = runInference(melSpectrogram)
        Log.d(TAG, "Inference is executed...!")
        return result
    }

    // Load TFLite model
    @Throws(IOException::class)
    private fun loadModel(modelPath: String) {
        val fileInputStream = FileInputStream(modelPath)
        val fileChannel = fileInputStream.channel
        val startOffset: Long = 0
        val declaredLength = fileChannel.size()
        val tfliteModel: ByteBuffer =
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        // Set the number of threads for inference
        val options = Interpreter.Options()
        options.numThreads = Runtime.getRuntime().availableProcessors()
        mInterpreter = Interpreter(tfliteModel, options)
    }

    // Load filters and vocab data from pre-generated filters_vocab_gen.bin file
    @Throws(IOException::class)
    private fun loadFiltersAndVocab(multilingual: Boolean, vocabPath: String) {

        // Read vocab file
        val bytes = Files.readAllBytes(Paths.get(vocabPath))
        val vocabBuf = ByteBuffer.wrap(bytes)
        vocabBuf.order(ByteOrder.nativeOrder())
        Log.d(TAG, "Vocab file size: " + vocabBuf.limit())

        // @magic:USEN
        val magic = vocabBuf.int
        if (magic == 0x5553454e) {
            Log.d(TAG, "Magic number: $magic")
        } else {
            Log.d(TAG, "Invalid vocab file (bad magic: $magic), $vocabPath")
            return
        }

        // Load mel filters
        mWhisper.filters.nMel = vocabBuf.int
        mWhisper.filters.nFft = vocabBuf.int
        Log.d(TAG, "n_mel:" + mWhisper.filters.nMel + ", n_fft:" + mWhisper.filters.nFft)
        val filterData =
            ByteArray(mWhisper.filters.nMel * mWhisper.filters.nFft * java.lang.Float.BYTES)
        vocabBuf[filterData, 0, filterData.size]
        val filterBuf = ByteBuffer.wrap(filterData)
        filterBuf.order(ByteOrder.nativeOrder())
        mWhisper.filters.data = FloatArray(mWhisper.filters.nMel * mWhisper.filters.nFft)
        run {
            var i = 0
            while (filterBuf.hasRemaining()) {
                mWhisper.filters.data[i] = filterBuf.float
                i++
            }
        }

        // Load vocabulary
        val nVocab = vocabBuf.int
        Log.d(TAG, "nVocab: $nVocab")
        for (i in 0 until nVocab) {
            val len = vocabBuf.int
            val wordBytes = ByteArray(len)
            vocabBuf[wordBytes, 0, wordBytes.size]
            val word = String(wordBytes)
            mWhisper.vocab.tokenToWord[i] = word
        }

        // Add additional vocab ids
        val mVocabAdditional: Int
        if (!multilingual) {
            mVocabAdditional = WhisperUtil.N_VOCAB_ENGLISH
        } else {
            mVocabAdditional = WhisperUtil.N_VOCAB_MULTILINGUAL
            mWhisper.vocab.tokenEot++
            mWhisper.vocab.tokenSot++
            mWhisper.vocab.tokenPrev++
            mWhisper.vocab.tokenSolm++
            mWhisper.vocab.tokenNot++
            mWhisper.vocab.tokenBeg++
        }
        for (i in nVocab until mVocabAdditional) {
            var word: String
            word = if (i > mWhisper.vocab.tokenBeg) {
                "[_TT_" + (i - mWhisper.vocab.tokenBeg) + "]"
            } else if (i == mWhisper.vocab.tokenEot) {
                "[_EOT_]"
            } else if (i == mWhisper.vocab.tokenSot) {
                "[_SOT_]"
            } else if (i == mWhisper.vocab.tokenPrev) {
                "[_PREV_]"
            } else if (i == mWhisper.vocab.tokenNot) {
                "[_NOT_]"
            } else if (i == mWhisper.vocab.tokenBeg) {
                "[_BEG_]"
            } else {
                "[_extra_token_$i]"
            }
            mWhisper.vocab.tokenToWord[i] = word
            //Log.d(TAG, "i= " + i + ", word= " + word);
        }
    }

    private fun getMelSpectrogram(wavePath: String): FloatArray? {
        // Get samples in PCM_FLOAT format
        val samples = WaveUtil.getSamples(wavePath)
        val fixedInputSize: Int =
            WhisperUtil.WHISPER_SAMPLE_RATE * WhisperUtil.WHISPER_CHUNK_SIZE
        val inputSamples = FloatArray(fixedInputSize)
        val copyLength = min(samples.size, fixedInputSize)
        System.arraycopy(samples, 0, inputSamples, 0, copyLength)
        val cores = Runtime.getRuntime().availableProcessors()
        if (!WhisperUtil.getMelSpectrogram(
                inputSamples,
                inputSamples.size,
                WhisperUtil.WHISPER_SAMPLE_RATE,
                WhisperUtil.WHISPER_N_FFT,
                WhisperUtil.WHISPER_HOP_LENGTH,
                WhisperUtil.WHISPER_N_MEL,
                cores,
                mWhisper.filters,
                mWhisper.mel
            )
        ) {
            Log.d(TAG, "%s: failed to compute mel spectrogram")
            return null
        }
        return mWhisper.mel.data
    }

    private fun runInference(inputData: FloatArray?): String {
        // Create input tensor
        val inputTensor = mInterpreter!!.getInputTensor(0)
        val inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())
        Log.d(TAG, "Input Tensor Dump ===>")
        printTensorDump(inputTensor)

        // Create output tensor
        val outputTensor = mInterpreter!!.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)
        Log.d(TAG, "Output Tensor Dump ===>")
        printTensorDump(outputTensor)

        // Load input data
        val inputSize =
            inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * java.lang.Float.BYTES
        val inputBuf = ByteBuffer.allocateDirect(inputSize)
        inputBuf.order(ByteOrder.nativeOrder())
        for (input in inputData!!) {
            inputBuf.putFloat(input)
        }

        // To test mel data as a input directly
//        try {
//            byte[] bytes = Files.readAllBytes(Paths.get("/data/user/0/com.example.tfliteaudio/files/mel_spectrogram.bin"));
//            inputBuf = ByteBuffer.wrap(bytes);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        inputBuffer.loadBuffer(inputBuf)

        // Run inference
        mInterpreter!!.run(inputBuffer.buffer, outputBuffer.buffer)

        // Retrieve the results
        val outputLen = outputBuffer.intArray.size
        Log.d(TAG, "output_len: $outputLen")
        val result = StringBuilder()
        for (i in 0 until outputLen) {
            val token = outputBuffer.buffer.int
            if (token == mWhisper.vocab.tokenEot) break

            // Get word for token and Skip additional token
            if (token < mWhisper.vocab.tokenEot) {
                val word = mWhisper.getWordFromToken(token)
                Log.d(TAG, "Adding token: $token, word: $word")
                result.append(word)
            } else {
                if (token == mWhisper.vocab.tokenTranscribe) Log.d(TAG, "It is Transcription...")
                if (token == mWhisper.vocab.tokenTranslate) Log.d(TAG, "It is Translation...")
                val word = mWhisper.getWordFromToken(token)
                Log.d(TAG, "Skipping token: $token, word: $word")
            }
        }
        return result.toString()
    }

    private fun printTensorDump(tensor: Tensor) {
        Log.d(TAG, "  shape.length: " + tensor.shape().size)
        for (i in tensor.shape().indices) Log.d(TAG, "    shape[" + i + "]: " + tensor.shape()[i])
        Log.d(TAG, "  dataType: " + tensor.dataType())
        Log.d(TAG, "  name: " + tensor.name())
        Log.d(TAG, "  numBytes: " + tensor.numBytes())
        Log.d(TAG, "  index: " + tensor.index())
        Log.d(TAG, "  numDimensions: " + tensor.numDimensions())
        Log.d(TAG, "  numElements: " + tensor.numElements())
        Log.d(TAG, "  shapeSignature.length: " + tensor.shapeSignature().size)
        Log.d(TAG, "  quantizationParams.getScale: " + tensor.quantizationParams().scale)
        Log.d(TAG, "  quantizationParams.getZeroPoint: " + tensor.quantizationParams().zeroPoint)
        Log.d(TAG, "==================================================================")
    }
}
package cx.aphex.chatgpt.whisper

import java.util.Arrays

class WhisperUtil {
    var vocab = WhisperVocab()
    var filters = WhisperFilter()
    var mel = WhisperMel()

    // Helper class definitions
    class WhisperVocab {
        // Token types
        var tokenEot = 50256 // end of transcript
        var tokenSot = 50257 // start of transcript
        var tokenPrev = 50360
        var tokenSolm = 50361 // ??
        var tokenNot = 50362 // no timestamps
        var tokenBeg = 50363

        // Available tasks
        var tokenTranslate = 50358
        var tokenTranscribe = 50359
        var tokenToWord: MutableMap<Int?, String?> = HashMap()
    }

    class WhisperFilter {
        var nMel = 0
        var nFft = 0
        lateinit var data: FloatArray
    }

    class WhisperMel {
        var nLen = 0
        var nMel = 0
        lateinit var data: FloatArray
    }

    // Helper functions definitions
    fun getWordFromToken(token: Int): String? {
        return vocab.tokenToWord[token]
    }

    companion object {
        private const val TAG = "WhisperUtil"

        // Vocab types
        var N_VOCAB_ENGLISH = 51864 // for english only vocab
        var N_VOCAB_MULTILINGUAL = 51865 // for multilingual vocab
        const val WHISPER_SAMPLE_RATE = 16000
        const val WHISPER_N_FFT = 400
        const val WHISPER_N_MEL = 80
        const val WHISPER_HOP_LENGTH = 160
        const val WHISPER_CHUNK_SIZE = 30
        const val WHISPER_MEL_LEN = 3000
        val golden_generated_ids = intArrayOf(
            50257, 50362, 1770, 13, 2264, 346, 353, 318,
            262, 46329, 286, 262, 3504, 6097, 11, 290, 356, 389, 9675, 284, 7062
        )

        // nSamples size => WHISPER_SAMPLE_RATE * WHISPER_CHUNK_SIZE => 480000
        fun getMelSpectrogram(
            samples: FloatArray, nSamples: Int, sampleRate: Int,
            fftSize: Int, fftStep: Int, nMel: Int, nThreads: Int,
            filters: WhisperFilter?, mel: WhisperMel?
        ): Boolean {
            mel!!.nMel = nMel
            mel.nLen = nSamples / fftStep
            mel.data = FloatArray(mel.nMel * mel.nLen)
            val hann = FloatArray(fftSize)
            for (i in 0 until fftSize) {
                hann[i] = (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / fftSize))).toFloat()
            }
            val nFft = 1 + fftSize / 2

/////////////// UNCOMMENT below block to use multithreaded mel calculation /////////////////////////
//        // Calculate mel values using multiple threads
//        List<Thread> workers = new ArrayList<>();
//        for (int iw = 0; iw < nThreads; iw++) {
//            final int ith = iw;  // Capture iw in a final variable for use in the lambda
//            Thread thread = new Thread(() -> {
//                // Inside the thread, ith will have the same value as iw (first value is 0)
//                Log.d(TAG, "Thread " + ith + " started.");
//
//                float[] fftIn = new float[fftSize];
//                Arrays.fill(fftIn, 0.0f);
//                float[] fftOut = new float[fftSize * 2];
//
//                for (int i = ith; i < mel.nLen; i += nThreads) {
/////////////// END of Block ///////////////////////////////////////////////////////////////////////

/////////////// COMMENT below block to use multithreaded mel calculation ///////////////////////////
            val fftIn = FloatArray(fftSize)
            Arrays.fill(fftIn, 0.0f)
            val fftOut = FloatArray(fftSize * 2)
            for (i in 0 until mel.nLen) {
/////////////// END of Block ///////////////////////////////////////////////////////////////////////
                val offset = i * fftStep

                // apply Hanning window
                for (j in 0 until fftSize) {
                    if (offset + j < nSamples) {
                        fftIn[j] = hann[j] * samples[offset + j]
                    } else {
                        fftIn[j] = 0.0f
                    }
                }

                // FFT -> mag^2
                fft(fftIn, fftOut)
                for (j in 0 until fftSize) {
                    fftOut[j] =
                        fftOut[2 * j] * fftOut[2 * j] + fftOut[2 * j + 1] * fftOut[2 * j + 1]
                }
                for (j in 0 until fftSize / 2) {
                    fftOut[j] += fftOut[fftSize - j - 1]
                }

                // mel spectrogram
                for (j in 0 until mel.nMel) {
                    var sum = 0.0
                    for (k in 0 until nFft) {
                        sum += (fftOut[k] * filters!!.data[j * nFft + k]).toDouble()
                    }
                    if (sum < 1e-10) {
                        sum = 1e-10
                    }
                    sum = Math.log10(sum)
                    mel.data[j * mel.nLen + i] = sum.toFloat()
                }
            }

/////////////// UNCOMMENT below block to use multithreaded mel calculation /////////////////////////
//            });
//            workers.add(thread);
//            thread.start();
//        }
//
//        // Wait for all threads to finish
//        for (Thread worker : workers) {
//            try {
//                worker.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
/////////////// END of Block ///////////////////////////////////////////////////////////////////////

            // clamping and normalization
            var mmax = -1e20
            for (i in 0 until mel.nMel * mel.nLen) {
                if (mel.data[i] > mmax) {
                    mmax = mel.data[i].toDouble()
                }
            }
            mmax -= 8.0
            for (i in 0 until mel.nMel * mel.nLen) {
                if (mel.data[i] < mmax) {
                    mel.data[i] = mmax.toFloat()
                }
                mel.data[i] = ((mel.data[i] + 4.0) / 4.0).toFloat()
            }
            return true
        }

        private fun dft(input: FloatArray, output: FloatArray) {
            val inSize = input.size
            for (k in 0 until inSize) {
                var re = 0.0f
                var im = 0.0f
                for (n in 0 until inSize) {
                    val angle = (2 * Math.PI * k * n / inSize).toFloat()
                    re += (input[n] * Math.cos(angle.toDouble())).toFloat()
                    im -= (input[n] * Math.sin(angle.toDouble())).toFloat()
                }
                output[k * 2 + 0] = re
                output[k * 2 + 1] = im
            }
        }

        private fun fft(input: FloatArray, output: FloatArray) {
            val inSize = input.size
            if (inSize == 1) {
                output[0] = input[0]
                output[1] = 0.0f
                return
            }
            if (inSize % 2 == 1) {
                dft(input, output)
                return
            }
            val even = FloatArray(inSize / 2)
            val odd = FloatArray(inSize / 2)
            var indxEven = 0
            var indxOdd = 0
            for (i in 0 until inSize) {
                if (i % 2 == 0) {
                    even[indxEven] = input[i]
                    indxEven++
                } else {
                    odd[indxOdd] = input[i]
                    indxOdd++
                }
            }
            val evenFft = FloatArray(inSize)
            val oddFft = FloatArray(inSize)
            fft(even, evenFft)
            fft(odd, oddFft)
            for (k in 0 until inSize / 2) {
                val theta = (2 * Math.PI * k / inSize).toFloat()
                val re = Math.cos(theta.toDouble()).toFloat()
                val im = -Math.sin(theta.toDouble()).toFloat()
                val reOdd = oddFft[2 * k + 0]
                val imOdd = oddFft[2 * k + 1]
                output[2 * k + 0] = evenFft[2 * k + 0] + re * reOdd - im * imOdd
                output[2 * k + 1] = evenFft[2 * k + 1] + re * imOdd + im * reOdd
                output[2 * (k + inSize / 2) + 0] = evenFft[2 * k + 0] - re * reOdd + im * imOdd
                output[2 * (k + inSize / 2) + 1] = evenFft[2 * k + 1] - re * imOdd - im * reOdd
            }
        }
    }
}
package cx.aphex.chatgpt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.example.tfliteaudio.Recorder
import com.example.tfliteaudio.TranscriptionEngine
import cx.aphex.chatgpt.api.OpenAIClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

enum class VoiceInputState {
    RECORDING,
    TRANSCRIBING,
    IDLE
}

@OptIn(ExperimentalCoroutinesApi::class)
@BetaOpenAI
class MainViewModel(
    private val defaultDispatcher: CoroutineContext = Dispatchers.IO.limitedParallelism(1)
) : ViewModel() {

    private val _useGPT4 = MutableStateFlow(false)
    val useGPT4: StateFlow<Boolean>
        get() = _useGPT4

    private val _useVoice = MutableStateFlow(false)
    val useVoice: StateFlow<Boolean>
        get() = _useVoice

    private val _voiceInputState = MutableStateFlow(VoiceInputState.IDLE)
    val voiceInputState: StateFlow<VoiceInputState>
        get() = _voiceInputState

    private val _newRecordedText = MutableStateFlow("")
    val newRecordedText: StateFlow<String>
        get() = _newRecordedText

    private val _chatLog = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatLog: StateFlow<List<ChatMessage>> = _chatLog

    private val _isFetchingAnswer = MutableStateFlow(false)
    val isFetchingAnswer: StateFlow<Boolean>
        get() = _isFetchingAnswer

    private var fetchAnswerJob: Job? = null
    private var recordJob: Job? = null
    private var recorder: Recorder = Recorder()

    fun setUseGPT4(useGPT4: Boolean) {
        _useGPT4.value = useGPT4
    }

    fun sendMessage(content: String) {
        if (content.isNotBlank()) {
            // Add the user message to the chat log
            _chatLog.value = _chatLog.value + ChatMessage(
                ChatRole.User,
                content
            )

            // Add a loading bot message to the chat log
            _chatLog.value = _chatLog.value + ChatMessage(ChatRole.Assistant, "\u2588")
            generateAnswer(content)
        }
    }

    private fun generateAnswer(content: String) {
        Log.d("generateAnswer", "submitQuery called!!!!")

        viewModelScope.launch(defaultDispatcher) {
            _isFetchingAnswer.emit(true)

            val currentAnswerChunks = mutableListOf<String>()

            fetchAnswerJob =
                OpenAIClient.generateAnswer(content, chatLog.value, useGPT4.value, useVoice.value)
                    .onStart {
                        currentAnswerChunks.clear()
                    }
                    .mapNotNull { chunk ->
                        chunk.choices.firstOrNull()?.delta?.content
                    }
                    .buffer()
                    .flowOn(defaultDispatcher)
                    .onEach { content ->
                        Log.d(
                            "generateAnswer",
                            "got $content, currentAnswerChunks= ${currentAnswerChunks}"
                        )
                        delay(24)
                        currentAnswerChunks.add(content)
                        Log.d(
                            "generateAnswer",
                            "added to currentAnswerChunks= ${currentAnswerChunks}"
                        )
                        updateLastChatMessage(currentAnswerChunks.joinToString("") + "\u2588")
                    }
                    .onCompletion { cause ->
                        Log.e("search", "Cancelled: $cause")
                    _isFetchingAnswer.emit(false)
                    updateLastChatMessage(currentAnswerChunks.joinToString(""))
                }
                .catch { cause ->
                    Log.e("search", "Exception: $cause")
                    updateLastChatMessage(cause.message ?: "Error")
                }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun updateLastChatMessage(content: String) {
        withContext(Dispatchers.Main) {
            _chatLog.value = _chatLog.value.dropLast(1) + ChatMessage(
                ChatRole.Assistant,
                content,
            )
        }
    }

    fun cancelFetchingAnswer() {
        fetchAnswerJob?.cancel(CancellationException("Answer fetching canceled by user"))
    }

    fun recordMessage(filesDir: String) {
        if (voiceInputState.value == VoiceInputState.IDLE) {
            _useVoice.value = true
            _voiceInputState.value = VoiceInputState.RECORDING
            recordJob = viewModelScope.launch(Dispatchers.IO) {
                recorder.record(filesDir)
                _voiceInputState.value = VoiceInputState.TRANSCRIBING
                val result = TranscriptionEngine().transcribeInput(filesDir)
                _newRecordedText.value = result
                _voiceInputState.value = VoiceInputState.IDLE
                return@launch
            }
        }
    }

    fun stopRecording() {
        if (_voiceInputState.value == VoiceInputState.RECORDING) {
            recorder.stop()
//            recordJob?.cancel()
        }
    }
}

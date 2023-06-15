package cx.aphex.chatgpt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import cx.aphex.chatgpt.api.OpenAIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
@BetaOpenAI
class MainViewModel(
    private val defaultDispatcher: CoroutineContext = Dispatchers.Main
//        Dispatchers.IO.limitedParallelism(1)
) : ViewModel() {
    private var job: Job? = null
    private val currentAnswerChunk: MutableStateFlow<String> = MutableStateFlow<String>("")

    private val currentAnswerChunks = mutableListOf<String>()

    // Intermediate flow that buffers the chunks
    private val bufferedAnswerChunks: Flow<String> =
        currentAnswerChunk
            .buffer()
            .onEach { content ->
                Log.d(
                    "search",
                    "buffer collect latest: got $content, currentAnswerChunks= ${currentAnswerChunks}"
                )
                delay(48)
                currentAnswerChunks.add(content)
                Log.d("search", "added to currentAnswerChunks= ${currentAnswerChunks}")
                withContext(Dispatchers.Main) {
                    _chatLog.value = _chatLog.value.dropLast(1) + ChatMessage(
                        ChatRole.Assistant,
                        currentAnswerChunks.joinToString(""),
                    )
                }
            }
            .flowOn(defaultDispatcher)

    private val _chatLog = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatLog: StateFlow<List<ChatMessage>> = _chatLog

    private val _isFetchingAnswer = MutableStateFlow(false)
    val isFetchingAnswer: StateFlow<Boolean>
        get() = _isFetchingAnswer

    fun sendMessage(query: String) {
        if (query.isNotBlank()) {
            // Add the user message to the chat log
            _chatLog.value = _chatLog.value + ChatMessage(
                ChatRole.User,
                query
            )

            // Add a loading bot message to the chat log
            _chatLog.value =
                _chatLog.value + ChatMessage(ChatRole.Assistant, "Loading...")
            search(query)
        }
    }


    fun search(query: String) {
        Log.d("search", "search called!!!!")

        viewModelScope.launch(Dispatchers.Main) {
            _isFetchingAnswer.emit(true)
            val answer: Flow<ChatCompletionChunk> =
                OpenAIClient.generateAnswer(query, _chatLog.value)

            answer
                .onStart {
                    job?.cancel()
                    currentAnswerChunks.clear()
                    currentAnswerChunk.value = ""
                    job = bufferedAnswerChunks
                        .launchIn(viewModelScope)
                }
                .onEach { chunk ->
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        Log.d("search", "${chunk} content: $content")
//                        withContext(Dispatchers.Main) {
                        currentAnswerChunk.value = content
                        // Update the last bot message in the chat log with the response
//                            _chatLog.value = _chatLog.value.dropLast(1) + ChatMessage(
//                                ChatRole.Assistant,
//                                allChunks.value.joinToString(""),
//                            )

                        Log.d("search", "chunks so far: ${currentAnswerChunks}")
//                        }
                    }
                }
                .flowOn(defaultDispatcher)
                .onCompletion { cause ->
                    _isFetchingAnswer.emit(false)
//                    _answerChunks.emit(chunks)
                }
                .catch { cause -> Log.e("search", "Exception: $cause") }
                .launchIn(viewModelScope)
        }
    }

}

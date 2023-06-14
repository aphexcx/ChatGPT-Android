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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@BetaOpenAI
class MainViewModel : ViewModel() {
    private val _answerChunks = MutableStateFlow<String>("")
    val answerChunks: StateFlow<String>
        get() = _answerChunks

    // Intermediate flow that buffers the chunks
    private val _bufferedAnswerChunks = MutableSharedFlow<String>()
    val bufferedAnswerChunks: SharedFlow<String> = _bufferedAnswerChunks.buffer()

    private val _isFetchingAnswer = MutableStateFlow(false)
    val isFetchingAnswer: StateFlow<Boolean>
        get() = _isFetchingAnswer

    val allChunks = MutableStateFlow<List<String>>(listOf())

    private val _chatLog = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatLog: StateFlow<List<ChatMessage>> = _chatLog

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

            // Start the search
            viewModelScope.launch {
                bufferedAnswerChunks.collectLatest { content ->
                    delay(10)
                    _chatLog.value = _chatLog.value.dropLast(1) + ChatMessage(
                        ChatRole.Assistant,
                        content,
                    )
                }
            }
            search(query)
        }
    }


    fun search(query: String) {
        Log.d("search", "search called!!!!")
        allChunks.value = listOf()
        viewModelScope.launch(Dispatchers.Main) {
            _isFetchingAnswer.emit(true)
            val answer: Flow<ChatCompletionChunk> =
                OpenAIClient.generateAnswer(query, _chatLog.value)

            answer
                .onEach { chunk ->
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        Log.d("search", "${chunk} content: $content")
                        allChunks.update { it + content }
                        withContext(Dispatchers.Main) {
                            _bufferedAnswerChunks.emit(content)
                            // Update the last bot message in the chat log with the response
                            _chatLog.value = _chatLog.value.dropLast(1) + ChatMessage(
                                ChatRole.Assistant,
                                allChunks.value.joinToString(""),
                            )

                            Log.d("search", "chunks so far: ${allChunks}")
                        }
                    }
                }
                .onCompletion { cause ->
                    _isFetchingAnswer.emit(false)
//                    _answerChunks.emit(chunks)
                }
                .catch { cause -> Log.e("search", "Exception: $cause") }
                .launchIn(viewModelScope)
        }
    }

}

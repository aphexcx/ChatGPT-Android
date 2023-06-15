package cx.aphex.chatgpt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import cx.aphex.chatgpt.api.OpenAIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@BetaOpenAI
class MainViewModel(
    private val defaultDispatcher: CoroutineContext = Dispatchers.IO.limitedParallelism(1)
) : ViewModel() {

    private val _chatLog = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatLog: StateFlow<List<ChatMessage>> = _chatLog

    private val _isFetchingAnswer = MutableStateFlow(false)
    val isFetchingAnswer: StateFlow<Boolean>
        get() = _isFetchingAnswer

    fun sendMessage(content: String, useGPT4: Boolean) {
        if (content.isNotBlank()) {
            // Add the user message to the chat log
            _chatLog.value = _chatLog.value + ChatMessage(
                ChatRole.User,
                content
            )

            // Add a loading bot message to the chat log
            _chatLog.value = _chatLog.value + ChatMessage(ChatRole.Assistant, "\u2588")
            generateAnswer(content, useGPT4)
        }
    }

    private fun generateAnswer(content: String, useGPT4: Boolean) {
        Log.d("generateAnswer", "submitQuery called!!!!")

        viewModelScope.launch(defaultDispatcher) {
            _isFetchingAnswer.emit(true)

            val currentAnswerChunks = mutableListOf<String>()

            OpenAIClient.generateAnswer(content, chatLog.value, useGPT4)
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
                    delay(16)
                    currentAnswerChunks.add(content)
                    Log.d("generateAnswer", "added to currentAnswerChunks= ${currentAnswerChunks}")
                    updateLastChatMessage(currentAnswerChunks.joinToString("") + "\u2588")
                }
                .onCompletion { cause ->
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
}

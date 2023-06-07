package cx.aphex.chatgpt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import cx.aphex.chatgpt.api.OpenAIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val bufferedAnswerChunks: StateFlow<String> = _answerChunks

    private val _isFetchingAnswer = MutableStateFlow(false)
    val isFetchingAnswer: StateFlow<Boolean>
        get() = _isFetchingAnswer

    val allChunks = MutableStateFlow<List<String>>(listOf())
    fun search(query: String) {
        Log.d("search", "search called!!!!")
        viewModelScope.launch(Dispatchers.Main) {
            _isFetchingAnswer.emit(true)
            val answer: Flow<ChatCompletionChunk> = OpenAIClient.generateAnswer(query)

            answer
                .onEach { chunk ->
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        Log.d("search", "${chunk} content: $content")
                        allChunks.update { it + content }
                        withContext(Dispatchers.Main) {
                            _answerChunks.emit(content)
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
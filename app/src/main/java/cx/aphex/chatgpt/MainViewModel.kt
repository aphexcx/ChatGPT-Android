package cx.aphex.chatgpt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import cx.aphex.chatgpt.api.OpenAIClient
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@BetaOpenAI
class MainViewModel : ViewModel() {
    private val _answerChunks = MutableSharedFlow<String>(replay = 0)
    val answerChunks: SharedFlow<String>
        get() = _answerChunks

    // Intermediate flow that buffers the chunks
    val bufferedAnswerChunks: Flow<String> = _answerChunks.buffer()

    private val _isFetchingAnswer = MutableStateFlow(false)
    val isFetchingAnswer: StateFlow<Boolean>
        get() = _isFetchingAnswer

    private val chunks: MutableList<String> = mutableListOf()
    fun search(query: String) {
        Log.d("search", "search called!!!!")
        viewModelScope.launch(Dispatchers.Main) {
            _isFetchingAnswer.emit(true)
            val answer: Flow<ChatCompletionChunk> = OpenAIClient.generateAnswer(query)

            answer
                .onEach { chunk ->
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        Log.d("search", "${chunk} content: $content")
                        chunks.add(content)
                        withContext(Dispatchers.Main) {
                            _answerChunks.emit(content)
                            Log.d("search", "chunks so far: ${chunks}")
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
package cx.aphex.chatgpt.api

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import cx.aphex.chatgpt.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@BetaOpenAI
object OpenAIClient {
    private val config = OpenAIConfig(
        token = BuildConfig.OPENAI_API_KEY,
        timeout = Timeout(socket = 60.seconds),
    )

    private val openAI = OpenAI(config)

    fun generateAnswer(
        prompt: String,
        history: List<ChatMessage>,
        useGPT4: Boolean
    ): Flow<ChatCompletionChunk> {
        val chatCompletionRequest = ChatCompletionRequest(
            model = if (useGPT4) ModelId("gpt-4") else ModelId("gpt-3.5-turbo"),
            messages = listOf(
                ChatMessage( //system prompt from https://news.ycombinator.com/item?id=37879077
                    role = ChatRole.System,
                    content = "You are ChatGPT, a large language model trained by OpenAI, based on the GPT-4 architecture.\n" +
                            "You are chatting with the user via the ChatGPT Android app. This means most of the time your lines should be a sentence or two, unless the user's request requires reasoning or long-form outputs. Never use emojis, unless explicitly asked to.\n" +
                            "Knowledge cutoff: 2022-01" +
                            "Current date: ${
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(Date())
                            }"
                )
            )
                    + history
                    + listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = prompt
                )
            ),
            maxTokens = 2048,
            temperature = 0.0
        )

        return openAI.chatCompletions(chatCompletionRequest)
            .flowOn(Dispatchers.IO)
    }
}

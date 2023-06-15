package cx.aphex.chatgpt

import android.animation.AnimatorSet
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aallam.openai.api.BetaOpenAI
import io.noties.markwon.Markwon

@OptIn(
    ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@BetaOpenAI
class MainActivity : ComponentActivity() {

    private lateinit var pulsingAnimation: AnimatorSet

    private val viewModel: MainViewModel by viewModels()

    private val markwon: Markwon by lazy { Markwon.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF4A148C)) { // gpt4purple
                    var useGPT4 by remember { mutableStateOf(false) }
                    Scaffold(
                        bottomBar = {
                            var query by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = query,
                                onValueChange = { newValue: String -> query = newValue },
                                label = { Text("Message") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onKeyEvent {
                                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                            viewModel.sendMessage(query, useGPT4)
                                            query = ""
                                        }
                                        true
                                    },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        viewModel.sendMessage(query, useGPT4)
                                        query = ""
                                    }
                                )
                            )
                        }
                    ) {
                        val chatLog = viewModel.chatLog.collectAsStateWithLifecycle()

                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                                .consumeWindowInsets(it),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (chatLog.value.isEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("GPT-3.5", textAlign = TextAlign.End)
                                        Switch(
                                            checked = useGPT4,
                                            onCheckedChange = { useGPT4 = !useGPT4 },
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Text("GPT-4", textAlign = TextAlign.Start)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            } else {
                                // Collect chat log and update the UI
                                items(chatLog.value) { message ->
                                    ChatMessage(message, useGPT4)
                                }
                            }
                        }

                        // Scroll to the last message when a new one is received
                        LaunchedEffect(chatLog.value) {
                            listState.animateScrollToItem(
                                chatLog.value.lastIndex.coerceAtLeast(
                                    0
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


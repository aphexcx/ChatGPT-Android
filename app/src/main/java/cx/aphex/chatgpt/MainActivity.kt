package cx.aphex.chatgpt

import android.animation.AnimatorSet
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.elevatedButtonElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors
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
import cx.aphex.chatgpt.ui.DotsLoadingIndicator
import cx.aphex.chatgpt.ui.GPT4Switch
import cx.aphex.chatgpt.ui.appTypography
import io.noties.markwon.Markwon

@ExperimentalAnimationApi
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
            MaterialTheme(typography = appTypography) {
                Surface(color = Color(0xFF4A148C)) { // gpt4purple
                    val useGPT4 = viewModel.useGPT4.collectAsStateWithLifecycle().value

                    val gptColor by animateColorAsState(
                        targetValue = if (useGPT4) Color(0xFF4A148C) else Color(
                            0xFF4CAF50
                        ),
                        animationSpec = tween(durationMillis = 200)
                    )
                    val isFetchingAnswer =
                        viewModel.isFetchingAnswer.collectAsStateWithLifecycle().value
                    Scaffold(
                        bottomBar = {
                            var query by remember { mutableStateOf("") }
                            Column {
                                AnimatedVisibility(
                                    visible = isFetchingAnswer, Modifier
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Button(
                                        onClick = { viewModel.cancelFetchingAnswer() },
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .wrapContentSize(),
                                        elevation = elevatedButtonElevation(),
                                        shape = RoundedCornerShape(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = gptColor)
                                    ) {
                                        Icon(
                                            Icons.Filled.Stop,
                                            contentDescription = "Stop Icon",
                                            tint = Color.White
                                        )
                                        Text(
                                            text = "Stop generating",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .wrapContentHeight()
                                                .align(Alignment.CenterVertically),
                                            style = typography.labelMedium
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = query,
                                    textStyle = typography.bodyMedium,
                                    onValueChange = { newValue: String -> query = newValue },
                                    label = { Text("Message") },
                                    enabled = !isFetchingAnswer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .onKeyEvent {
                                            if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                                viewModel.sendMessage(query)
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
                                            viewModel.sendMessage(query)
                                            query = ""
                                        }
                                    ),
                                    colors = outlinedTextFieldColors(
                                        focusedBorderColor = gptColor,
                                        focusedLabelColor = gptColor,
                                        cursorColor = gptColor,
                                        selectionColors = TextSelectionColors(gptColor, gptColor)
                                    ),
                                    trailingIcon = {
                                        if (isFetchingAnswer) {
                                            DotsLoadingIndicator(color = gptColor)
                                        }
                                    }
                                )
                            }
                        }
                    ) { paddingValues ->
                        val chatLog = viewModel.chatLog.collectAsStateWithLifecycle()

                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .consumeWindowInsets(paddingValues),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (chatLog.value.isEmpty()) {
                                item {
                                    GPT4Switch(useGPT4, gptColor)
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
                                ),
                                scrollOffset = 100
                            )
                        }
                    }
                }
            }
        }
    }
}
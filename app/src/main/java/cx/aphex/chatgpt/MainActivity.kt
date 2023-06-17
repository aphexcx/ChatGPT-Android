package cx.aphex.chatgpt

import android.animation.AnimatorSet
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aallam.openai.api.BetaOpenAI
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
                    var useGPT4 by remember { mutableStateOf(false) }
                    Scaffold(
                        bottomBar = {
                            var query by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = query,
                                textStyle = typography.bodyMedium,
                                onValueChange = { newValue: String -> query = newValue },
                                label = { Text("Message") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Gray)
                                    ) {
                                        val selectorWidth =
                                            with(LocalDensity.current) { 64.dp.toPx() }
                                        val selectorOffset = remember { mutableStateOf(0f) }
                                        val selectorTransition = updateTransition(useGPT4)

                                        selectorTransition.animateFloat(
                                            transitionSpec = { spring(stiffness = 200f) },
                                            label = ""
                                        ) { if (it) 1f else 0f }.apply {
                                            selectorOffset.value = selectorWidth * value
                                        }

//                                        val value by animateFloatAsState(
//                                            targetValue = 100f,
//                                            animationSpec = spring(
//                                                dampingRatio = Spring.DampingRatioHighBouncy,
//                                                stiffness = Spring.StiffnessMedium
//                                            )
//                                        )
//                                        val alignment = remember { Alignment.CenterStart }
                                        AnimatedContent(
                                            useGPT4,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp),
                                            transitionSpec = {
                                                slideInHorizontally(
                                                    initialOffsetX = { it }, // it == fullWidth
                                                    animationSpec = tween(
                                                        durationMillis = 200,
                                                        easing = LinearEasing
                                                    )
                                                ) with
                                                        slideOutHorizontally(
                                                            animationSpec = tween(
                                                                durationMillis = 200,
                                                                easing = LinearEasing
                                                            )
                                                        )
                                            }
//                                            transitionSpec = ContentTransform { spring(stiffness = 200f) },
//                                            contentAlignment = if (useGPT4) Alignment.CenterEnd else Alignment.CenterStart
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(64.dp),
                                                horizontalArrangement = if (useGPT4) Arrangement.End else Arrangement.Start
                                            ) {

                                                Box(
                                                    modifier = Modifier
//                                                .offset(x = selectorOffset.value.dp)
//                                                    .fillMaxWidth(0.5f).height(64.dp)
//                                                        .fillMaxSize()
                                                        .fillMaxWidth(0.5f)
                                                        .height(64.dp)

//                                                        .size(width = 64.dp, height = 64.dp)
                                                        .padding(4.dp)
//                                                .fillMaxHeight()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (useGPT4) Color(0xFF4A148C) else Color(
                                                                0xFF4CAF50
                                                            )
                                                        )
//                                                .offset(x = 40.dp) //value.dp) // selectorOffset.value.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Transparent),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(4.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { useGPT4 = false },
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        painterResource(R.drawable.bolt),
                                                        contentDescription = "GPT-3.5 Icon",
                                                        modifier = Modifier
                                                            .padding(start = 8.dp, end = 2.dp)
                                                            .size(32.dp),
                                                        tint = Color.White
                                                    )
                                                    Text(
                                                        "GPT-3.5",
                                                        textAlign = TextAlign.Start,
                                                        color = Color.White,
                                                        style = typography.labelLarge,
                                                    )
                                                }
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(4.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { useGPT4 = true },
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        painterResource(R.drawable.awesome),
                                                        contentDescription = "GPT-4 Icon",
                                                        modifier = Modifier
                                                            .padding(8.dp)
                                                            .size(32.dp),
                                                        tint = Color.White
                                                    )
                                                    Text(
                                                        "GPT-4",
                                                        textAlign = TextAlign.Start,
                                                        color = Color.White,
                                                        style = typography.labelLarge,
                                                    )
                                                }
                                            }
                                        }
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


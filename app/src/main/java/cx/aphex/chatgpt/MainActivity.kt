package cx.aphex.chatgpt

import BlinkingCaretSpan
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aallam.openai.api.BetaOpenAI
import io.noties.markwon.Markwon

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@BetaOpenAI
class MainActivity : ComponentActivity() {

    private lateinit var pulsingAnimation: AnimatorSet

    //    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val markwon: Markwon by lazy { Markwon.create(this) }

    lateinit var blinkingCaretAnimator: ObjectAnimator

    private val answerChunks = SpannableStringBuilder()

    private lateinit var blinkingCaretSpan: BlinkingCaretSpan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    color = Color(
                        ContextCompat.getColor(
                            this,
                            R.color.gpt4purple
                        )
                    ),
                    modifier = Modifier.fillMaxHeight()
                ) { // gpt4purple
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_chatgpt),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(top = 24.dp)
                        )

                        var query by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = query,
                            onValueChange = { newValue: String -> query = newValue },
                            label = { Text("Search") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send,
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    performSearch(query)
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
//                            elevation = CardElevation() 4.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            FlowRow {
                                // Collect answer chunks and update the UI
                                val answerChunks =
                                    viewModel.allChunks.collectAsStateWithLifecycle()
//                                LaunchedEffect(answerChunks) {
//                                    answerChunks.value.forEach { newChunk ->
//                                        delay(48)
                                Text(answerChunks.value.joinToString(""))
//                                    }
                            }

                        }
                    }
                }
            }
        }
    }


//     fun oldonCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//
//        binding.searchQuery.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
//                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
//            ) {
//                performSearch(query)
//                true
//            } else {
//                false
//            }
//        }
//
//        blinkingCaretSpan = BlinkingCaretSpan(ContextCompat.getColor(this, R.color.white))
//
//        pulsingAnimation = Animations.createPulsingAnimation(binding.logo)
//
//        // Collect answer chunks and update the UI
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//
//                viewModel.bufferedAnswerChunks.collect { newChunk ->
////                    answerChunks.clear()
//                    Log.d("mainactivity", "MAINACTIVITY got chunk: $newChunk")
//                    Log.d("mainactivity", "MAINACTIVITY CHUNKS: $answerChunks")
//                    binding.answerCard.visibility = View.VISIBLE
//
//                    // Remove the previous caret if it exists
//                    removeCaret()
//                    delay(48)
//                    answerChunks.append(newChunk)
//
//                    // Append the blinking caret and apply the BlinkingCaretSpan
//                    val caretPosition = answerChunks.length
//                    answerChunks.append("\u2588") // Unicode character for a block caret
////                                    answerChunks.setSpan(
////                                        blinkingCaretSpan,
////                                        caretPosition,
////                                        caretPosition + 1,
////                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
////                                    )
//
////                                    blinkingCaretAnimator = createCaretAnimator(blinkingCaretSpan)
//
//                    // Update the Markdown view
//                    updateMarkdownView()
//                }
//            }
//        }
//
//
//        lifecycleScope.launch {
//            viewModel.isFetchingAnswer.collect { isFetching ->
//                if (isFetching) {
//                    pulsingAnimation.start()
//                } else {
//                    pulsingAnimation.cancel()
//                    binding.logo.scaleX = 1f
//                    binding.logo.scaleY = 1f
//                    lifecycleScope.launch {
//                        delay(2000)
//                        removeCaret()
//                        updateMarkdownView()
//                    }
//                }
//            }
//        }
//    }

//    private fun updateMarkdownView() {
//        markwon.setMarkdown(
//            binding.markdownView,
//            answerChunks.toString()
//        )
//    }

    private fun removeCaret() {
        val previousCaretPosition = answerChunks.lastIndex
        if (previousCaretPosition >= 0) {
            answerChunks.removeSpan(blinkingCaretSpan)
            answerChunks.delete(previousCaretPosition, previousCaretPosition + 1)
        }
    }

    private fun performSearch(query: String) {
//        answerChunks.clear()
        if (query.isNotBlank()) {
            viewModel.search(query)
        }
    }
}
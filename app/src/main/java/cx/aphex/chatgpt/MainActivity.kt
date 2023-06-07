package cx.aphex.chatgpt

import BlinkingCaretSpan
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aallam.openai.api.BetaOpenAI
import cx.aphex.chatgpt.databinding.ActivityMainBinding
import io.noties.markwon.Markwon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@BetaOpenAI
class MainActivity : AppCompatActivity() {

    private lateinit var pulsingAnimation: AnimatorSet
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val markwon: Markwon by lazy { Markwon.create(this) }

    lateinit var blinkingCaretAnimator: ObjectAnimator

    private val answerChunks = SpannableStringBuilder()

    private lateinit var blinkingCaretSpan: BlinkingCaretSpan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.searchQuery.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch()
                true
            } else {
                false
            }
        }

        blinkingCaretSpan = BlinkingCaretSpan(ContextCompat.getColor(this, R.color.white))

        pulsingAnimation = Animations.createPulsingAnimation(binding.logo)

        // Collect answer chunks and update the UI
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.bufferedAnswerChunks.collect { newChunk ->
//                    answerChunks.clear()
                    Log.d("mainactivity", "MAINACTIVITY got chunk: $newChunk")
                    Log.d("mainactivity", "MAINACTIVITY CHUNKS: $answerChunks")
                    binding.answerCard.visibility = View.VISIBLE

                    // Remove the previous caret if it exists
                    removeCaret()
                    delay(48)
                    answerChunks.append(newChunk)

                    // Append the blinking caret and apply the BlinkingCaretSpan
                    val caretPosition = answerChunks.length
                    answerChunks.append("\u2588") // Unicode character for a block caret
//                                    answerChunks.setSpan(
//                                        blinkingCaretSpan,
//                                        caretPosition,
//                                        caretPosition + 1,
//                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                                    )

//                                    blinkingCaretAnimator = createCaretAnimator(blinkingCaretSpan)

                    // Update the Markdown view
                    updateMarkdownView()
                }
            }
        }


        lifecycleScope.launch {
            viewModel.isFetchingAnswer.collect { isFetching ->
                if (isFetching) {
                    pulsingAnimation.start()
                } else {
                    pulsingAnimation.cancel()
                    binding.logo.scaleX = 1f
                    binding.logo.scaleY = 1f
                    lifecycleScope.launch {
                        delay(2000)
                        removeCaret()
                        updateMarkdownView()
                    }
                }
            }
        }
    }

    private fun updateMarkdownView() {
        markwon.setMarkdown(
            binding.markdownView,
            answerChunks.toString()
        )
    }

    private fun removeCaret() {
        val previousCaretPosition = answerChunks.lastIndex
        if (previousCaretPosition >= 0) {
            answerChunks.removeSpan(blinkingCaretSpan)
            answerChunks.delete(previousCaretPosition, previousCaretPosition + 1)
        }
    }

    private fun performSearch() {
        val query = binding.searchQuery.text.toString()
        binding.answerCard.visibility = View.GONE
        binding.sourcesView.visibility = View.GONE
        answerChunks.clear()
        if (query.isNotBlank()) {
            viewModel.search(query)
        }
    }
}
package cx.aphex.chatgpt.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aallam.openai.api.BetaOpenAI
import cx.aphex.chatgpt.MainViewModel
import cx.aphex.chatgpt.R

@OptIn(BetaOpenAI::class)
@Composable
fun GPT4Switch(
    useGPT4: Boolean,
    gptColor: Color
) {
    val viewModel: MainViewModel = viewModel()
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Gray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = if (useGPT4) Arrangement.End else Arrangement.Start
        ) {
            AnimatedVisibility(
                visible = !useGPT4,
                enter = slideInHorizontally(
                    animationSpec = tween(
                        durationMillis = 200,
                    )
                ) { it },
                exit = slideOutHorizontally(
                    animationSpec = tween(
                        durationMillis = 200,
                    )
                ) { 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(64.dp)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(gptColor)
                )
            }
            AnimatedVisibility(visible = useGPT4,
                enter = slideInHorizontally(
                    tween(
                        durationMillis = 200,
                    )
                ) { -it },
                exit = slideOutHorizontally(
                    animationSpec = tween(
                        durationMillis = 200,
                    )
                ) { -it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(64.dp)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(gptColor)
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
                    .clickable { viewModel.setUseGPT4(false) },
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
                        style = if (useGPT4) MaterialTheme.typography.displayLarge else MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { viewModel.setUseGPT4(true) },
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
                        style = if (useGPT4) MaterialTheme.typography.labelLarge else MaterialTheme.typography.displayLarge,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

}
@file:OptIn(BetaOpenAI::class)

package cx.aphex.chatgpt

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import dev.jeziellago.compose.markdowntext.MarkdownText

private fun ChatMessage.getNameOrFromRole(useGPT4: Boolean): String? = when (role) {
    ChatRole.User -> "YOU"
    ChatRole.Assistant -> if (useGPT4) "GPT-4" else "GPT-3.5"
    else -> name
}


private val ChatMessage.profileImage: Int?
    get() = when (role) {
        ChatRole.User -> android.R.drawable.stat_sys_headset
        ChatRole.Assistant -> R.drawable.logo_chatgpt
        else -> null
    }


@Composable
fun ChatMessage(message: ChatMessage, useGPT4: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column() {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    message.profileImage?.let {
                        val profileTint = if (useGPT4) Color(0xFF4A148C) else Color(0xFF4CAF50)
                        Image(
                            painter = painterResource(id = it),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                                .clip(CircleShape),
                            colorFilter = ColorFilter.tint(profileTint)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    message.getNameOrFromRole(useGPT4)?.let {
                        Text(
                            text = it,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 36.dp, top = 0.dp, end = 16.dp)) {
                MarkdownText(markdown = message.content)
            }
        }
    }
}

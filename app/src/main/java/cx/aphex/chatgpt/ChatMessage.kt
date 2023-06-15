@file:OptIn(BetaOpenAI::class)

package cx.aphex.chatgpt

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.transferParameters
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import dev.jeziellago.compose.markdowntext.MarkdownText

private val ChatMessage.profileImage: Int?
    get() = when (role) {
        ChatRole.User -> {
            android.R.drawable.stat_sys_headset
        }

        ChatRole.Assistant -> {
            R.drawable.logo_chatgpt
        }

        else -> null
    }


@Composable
fun ChatMessage(message: ChatMessage, useGPT4: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            message.profileImage?.let {
                val colorMatrix = ColorMatrix().apply {
                    val color = if (useGPT4) Color(0xFF4A148C) else Color(0xFF4CAF50)
                    setColorFilter(color.transferParameters(ColorSpaces.Srgb))
                }
                Image(
                    painter = painterResource(id = it),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column() {
                message.name?.let { Text(it) }
                MarkdownText(markdown = message.content)
            }
        }
    }
}

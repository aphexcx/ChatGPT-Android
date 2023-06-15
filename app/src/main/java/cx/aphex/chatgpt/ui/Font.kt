package cx.aphex.chatgpt.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import cx.aphex.chatgpt.R

val appFontFamily = FontFamily(
    fonts = listOf(
        Font(
            resId = R.font.soehne_buch,
            weight = FontWeight.W400,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.soehne_kraftig,
            weight = FontWeight.W500,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.soehne_fett,
            weight = FontWeight.W800,
            style = FontStyle.Normal
        ),
    )
)
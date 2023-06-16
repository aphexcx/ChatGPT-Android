package cx.aphex.chatgpt.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cx.aphex.chatgpt.R


val appFontFamily = FontFamily(
    Font(
        resId = R.font.soehne_buch,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ),
    Font(
        resId = R.font.soehne_buchkursiv,
        weight = FontWeight.Normal, //400
        style = FontStyle.Italic
    ),
    Font(
        resId = R.font.soehne_kraftig, //500
        weight = FontWeight.Medium,
        style = FontStyle.Normal
    ),
    Font(
        resId = R.font.soehne_fett, //800
        weight = FontWeight.Bold,
        style = FontStyle.Normal
    )    ,
    Font(
        resId = R.font.soehne_fettkursiv,
        weight = FontWeight.Bold,
        style = FontStyle.Italic
    )
)

val appTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
)
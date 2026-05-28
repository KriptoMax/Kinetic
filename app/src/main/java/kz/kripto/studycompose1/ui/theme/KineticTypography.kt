package kz.kripto.studycompose1.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R

val MinecraftFontFamily = FontFamily(
    Font(resId = R.font.minecraft, weight = FontWeight.Normal)
)
val NunitoFontFamily = FontFamily(
    Font(resId = R.font.nunito_bold, weight = FontWeight.Bold),
    Font(resId = R.font.nunito_regular, weight = FontWeight.Normal),
    Font(resId = R.font.nunito_semibold, weight = FontWeight.SemiBold)
)
val RubikFontFamily = FontFamily(
    Font(resId = R.font.rubik_regular, weight = FontWeight.Normal)
)

object KineticStyle{
    val minecraftStyle = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )

    val nunitoBoldStyle = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
    val nunitoNormalStyle = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
    val nunitoSemiBoldStyle = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )

    val rubikNormalStyle = TextStyle(
        fontFamily = RubikFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
}
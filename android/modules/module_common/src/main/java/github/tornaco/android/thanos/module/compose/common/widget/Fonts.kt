package github.tornaco.android.thanos.module.compose.common.widget

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun fontFamilyProductSans() = FontFamily(
    Font(assetManager = LocalContext.current.assets, path = "fonts/google/ProductSansBold.ttf")
)

@Composable
fun fontFamilyJetBrainsMono() = FontFamily(
    Font(
        assetManager = LocalContext.current.assets,
        path = "fonts/google/jetbrains/JetBrainsMonoRegular.ttf"
    )
)

@Composable
fun productSansBoldTypography() = Typography(
    h5 = TextStyle(
        fontFamily = fontFamilyProductSans(),
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    caption = TextStyle(
        fontFamily = fontFamilyProductSans(),
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun jetbrainMonoTypography() = Typography(
    body1 = TextStyle(
        fontFamily = fontFamilyJetBrainsMono(),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = fontFamilyJetBrainsMono(),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
)
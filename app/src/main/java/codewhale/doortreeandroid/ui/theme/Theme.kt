package codewhale.doortreeandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DoorTreeTheme {
    val backgroundPrimary = Color(0xFFF4EEE6)
    val backgroundSecondary = Color(0xFFE8DDD0)
    val cardSurface = Color(0xD1FFF9F2)
    val cardBorder = Color(0x1A3E3227)
    val rowSurface = Color(0xEBFFFBF6)
    val rowBorder = Color(0x143E3227)
    val textPrimary = Color(0xFF1E1813)
    val textSecondary = Color(0xFF74675B)
    val gradientStart = Color(0xFF2ECC8A)
    val gradientEnd = Color(0xFF1ABCB0)
    val accentForeground = Color(0xFF07131B)
    val dueBackground = Color(0xFFFCE7CC)
    val dueText = Color(0xFF9E5E00)
    val paidBackground = Color(0xFFDDF3E6)
    val paidText = Color(0xFF157752)
    val pendingBackground = Color(0xFFE9DED2)
    val pendingText = Color(0xFF70665D)
    val chatAccent = Color(0xFF42D1D8)
    val chatAccentBackground = Color(0xFFDAF1EE)
    val leaseAccent = Color(0xFFB07CFF)
    val leaseAccentBackground = Color(0xFFF0E4FA)
    val destructive = Color(0xFFA92B2B)
    val divider = Color(0x1A3E3227)
    val tabBarOverlay = Color(0xDBF7F1E8)
    val cardShadow = Color(0x1F2E2419)
    val glassStroke = Color(0x1A3E3227)
    val overlayScrim = Color(0x8FF5EFE7)
    val barGlassTint = Color(0x3DFFF8F0)
    val barHighlightStrong = Color(0x57FFFFFF)
    val barHighlightSoft = Color(0x29FFF8F0)
    val inputGlassTint = Color(0x47FFF8F0)
    val screenHorizontalPadding = 18.dp
    val cardPadding = 14.dp
    val cardCornerRadius = 14.dp
    val buttonCornerRadius = 12.dp
    val cardSpacing = 10.dp

    val primaryGradient: Brush
        get() = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))

    val avatarGradient: Brush
        get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

private val lightScheme = lightColorScheme(
    primary = DoorTreeTheme.gradientStart,
    onPrimary = DoorTreeTheme.accentForeground,
    surface = DoorTreeTheme.backgroundPrimary,
    onSurface = DoorTreeTheme.textPrimary,
    background = DoorTreeTheme.backgroundPrimary,
    onBackground = DoorTreeTheme.textPrimary
)

private val darkScheme = darkColorScheme(
    primary = DoorTreeTheme.gradientStart,
    onPrimary = DoorTreeTheme.accentForeground,
    surface = DoorTreeTheme.backgroundPrimary,
    onSurface = DoorTreeTheme.textPrimary,
    background = DoorTreeTheme.backgroundPrimary,
    onBackground = DoorTreeTheme.textPrimary
)

@Composable
fun DoorTreeAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
        content = content
    )
}

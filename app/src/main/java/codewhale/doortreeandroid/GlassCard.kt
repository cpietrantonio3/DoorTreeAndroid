package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

fun Modifier.liquidGlassSurface(
    cornerRadius: Dp = DoorTreeTheme.cardCornerRadius,
    interactive: Boolean = false,
    tint: Color = Color.Unspecified
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val baseColor = if (tint != Color.Unspecified) tint.copy(alpha = 0.22f) else DoorTreeTheme.cardSurface
    val bottomColor = if (interactive) DoorTreeTheme.rowSurface.copy(alpha = 0.96f) else DoorTreeTheme.rowSurface.copy(alpha = 0.84f)
    return this
        .shadow(12.dp, shape, clip = false, ambientColor = DoorTreeTheme.cardShadow, spotColor = DoorTreeTheme.cardShadow)
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(baseColor, bottomColor)
            ),
            shape = shape
        )
        .border(1.dp, DoorTreeTheme.glassStroke, shape)
}

fun Modifier.glassCard(cornerRadius: Dp = DoorTreeTheme.cardCornerRadius): Modifier {
    return liquidGlassSurface(cornerRadius = cornerRadius)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DoorTreeTheme.cardCornerRadius,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = cornerRadius),
        content = content
    )
}

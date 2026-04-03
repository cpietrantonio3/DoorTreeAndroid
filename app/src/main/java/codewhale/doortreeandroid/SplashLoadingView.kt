package codewhale.doortreeandroid

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun SplashLoadingView(onFinish: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splash")
    val glowScale by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(animation = tween(1600), repeatMode = RepeatMode.Reverse),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        delay(1900)
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .topSafeAreaPadding()
                        .padding(horizontal = DoorTreeTheme.screenHorizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
            Spacer(modifier = Modifier.height(1.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(176.dp)
                            .scale(glowScale)
                            .blur(18.dp)
                            .background(DoorTreeTheme.gradientStart.copy(alpha = 0.16f), CircleShape)
                    )
                    DoorTreeLogoLockup(width = 220.dp)
                }
                Text(text = L("splash.subtitle"), color = DoorTreeTheme.textSecondary)
                CircularProgressIndicator(color = DoorTreeTheme.gradientStart)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 34.dp)) {
                PoweredByCodeWhaleFooter()
            }
        }
    }
}

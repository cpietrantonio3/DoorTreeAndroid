package codewhale.doortreeandroid

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import kotlinx.coroutines.launch

@Composable
fun AuthBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF9F0),
                        Color(0xFFF0E4D5),
                        Color(0xFFE8DDD0)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .blur(74.dp)
                .background(DoorTreeTheme.gradientStart.copy(alpha = 0.18f), CircleShape)
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .size(310.dp)
                .blur(68.dp)
                .background(Color(0xFFF6DDB8).copy(alpha = 0.88f), CircleShape)
                .align(Alignment.TopEnd)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .blur(62.dp)
                .background(DoorTreeTheme.paidBackground.copy(alpha = 0.92f), CircleShape)
                .align(Alignment.BottomStart)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHeightModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DoorTreeTheme.backgroundPrimary,
        scrimColor = DoorTreeTheme.overlayScrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(DoorTreeTheme.pendingText.copy(alpha = 0.35f))
                )
            }
        },
        content = content
    )
}

@Composable
fun DoorTreeLogoLockup(width: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.doortree_logo_black),
        contentDescription = L("brand.doortree"),
        modifier = modifier.width(width),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun PoweredByCodeWhaleFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = L("brand.powered_by"), color = DoorTreeTheme.textSecondary)
        Spacer(modifier = Modifier.width(10.dp))
        Image(
            painter = painterResource(id = R.drawable.codewhale_logo),
            contentDescription = null,
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
fun HeaderIconButton(systemName: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .liquidGlassSurface(cornerRadius = 14.dp, interactive = true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = systemIcon(systemName),
            contentDescription = null,
            tint = DoorTreeTheme.textPrimary
        )
    }
}

@Composable
fun AvatarCircle(initials: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(DoorTreeTheme.primaryGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = DoorTreeTheme.accentForeground)
    }
}

@Composable
fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: String? = null,
    trailingContent: @Composable (BoxScope.() -> Unit)? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 18.dp, interactive = true)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (leadingIcon != null) {
                Icon(systemIcon(leadingIcon), contentDescription = null, tint = DoorTreeTheme.textSecondary)
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isBlank()) {
                    Text(text = placeholder, color = DoorTreeTheme.textSecondary)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = TextStyle(color = DoorTreeTheme.textPrimary),
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingContent != null) {
                Box(content = trailingContent)
            }
        }
    }
}

@Composable
fun LiquidBarBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DoorTreeTheme.barGlassTint)
    )
}

@Composable
fun Modifier.topSafeAreaPadding(): Modifier {
    return windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
}

@Composable
fun Modifier.bottomSafeAreaPadding(): Modifier {
    return windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableScreen(
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (isRefreshing) {
                return@PullToRefreshBox
            }
            coroutineScope.launch {
                isRefreshing = true
                try {
                    onRefresh()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = modifier,
        content = content
    )
}

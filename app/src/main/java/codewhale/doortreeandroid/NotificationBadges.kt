package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun NotificationCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    val displayCount = if (count > 99) "99+" else count.toString()
    Text(
        text = displayCount,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(10.dp), ambientColor = DoorTreeTheme.destructive.copy(alpha = 0.22f))
            .background(DoorTreeTheme.destructive, RoundedCornerShape(10.dp))
            .border(2.dp, DoorTreeTheme.backgroundPrimary, RoundedCornerShape(10.dp))
            .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
            .padding(horizontal = if (count > 9) 6.dp else 5.dp)
    )
}

@Composable
fun NotificationDotBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(3.dp, CircleShape, ambientColor = DoorTreeTheme.destructive.copy(alpha = 0.22f))
            .size(11.dp)
            .background(DoorTreeTheme.destructive, CircleShape)
            .border(2.dp, DoorTreeTheme.backgroundPrimary, CircleShape)
    )
}

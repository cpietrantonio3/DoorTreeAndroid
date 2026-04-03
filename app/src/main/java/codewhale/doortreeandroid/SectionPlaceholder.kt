package codewhale.doortreeandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun SectionPlaceholder(
    systemName: String,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 16.dp, tint = DoorTreeTheme.backgroundSecondary.copy(alpha = 0.18f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(systemIcon(systemName), contentDescription = null, tint = DoorTreeTheme.textSecondary)
        Text(text = title, color = DoorTreeTheme.textPrimary, textAlign = TextAlign.Center)
        Text(text = message, color = DoorTreeTheme.textSecondary, textAlign = TextAlign.Center)
    }
}

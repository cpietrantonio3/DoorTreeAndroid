package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun AuthLoadingOverlay(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.overlayScrim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .glassCard(cornerRadius = 18.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = DoorTreeTheme.textPrimary)
            Text(text = title, color = DoorTreeTheme.textPrimary)
            Text(text = subtitle, color = DoorTreeTheme.textSecondary)
        }
    }
}

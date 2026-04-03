package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

enum class RentScheduleRowStyle {
    Status,
    LeaseDisplay
}

@Composable
fun RentScheduleRow(
    entry: RentScheduleEntry,
    style: RentScheduleRowStyle = RentScheduleRowStyle.Status
) {
    val surfaceTint = when (style) {
        RentScheduleRowStyle.Status -> entry.accentBackground.copy(alpha = 0.28f)
        RentScheduleRowStyle.LeaseDisplay -> DoorTreeTheme.barGlassTint
    }
    val borderColor = when (style) {
        RentScheduleRowStyle.Status -> entry.accentColor.copy(alpha = 0.22f)
        RentScheduleRowStyle.LeaseDisplay -> DoorTreeTheme.cardBorder
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 16.dp, tint = surfaceTint)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (style == RentScheduleRowStyle.Status) 4.dp else 0.dp)) {
            Text(text = entry.formattedDueDate, color = DoorTreeTheme.textPrimary)
            if (style == RentScheduleRowStyle.Status) {
                Text(text = entry.statusLabel, color = DoorTreeTheme.textSecondary)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(text = entry.amount, color = DoorTreeTheme.textPrimary)
            if (style == RentScheduleRowStyle.Status) {
                Text(
                    text = entry.monthLabel,
                    color = entry.accentColor,
                    modifier = Modifier
                        .background(entry.accentBackground.copy(alpha = 0.78f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

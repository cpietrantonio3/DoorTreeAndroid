package codewhale.doortreeandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import kotlinx.coroutines.launch

@Composable
fun RequestRow(
    request: MaintenanceRequestItem,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 14.dp, interactive = onClick != null)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = request.title,
                color = DoorTreeTheme.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle ?: LF("requests.submitted_format", request.submittedDate),
                color = DoorTreeTheme.textSecondary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        StatusBadge(status = request.status)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissiblePendingRequestRow(
    request: MaintenanceRequestItem,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onDeleteConfirmed: suspend () -> Unit
) {
    if (request.status != StatusBadgeStyle.Pending) {
        RequestRow(request = request, subtitle = subtitle, onClick = onClick)
        return
    }

    val scope = rememberCoroutineScope()
    var showDeleteConfirmation by remember(request.id) { mutableStateOf(false) }
    var deleteError by remember(request.id) { mutableStateOf<String?>(null) }

    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmation = true
                false
            } else {
                true
            }
        }
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (
            dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
            dismissState.targetValue != SwipeToDismissBoxValue.Settled
        ) 1f else 0f,
        label = "requestDeleteBackgroundAlpha"
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundAlpha)
                    .liquidGlassSurface(
                        cornerRadius = 14.dp,
                        tint = DoorTreeTheme.destructive.copy(alpha = 0.16f)
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        systemIcon("trash.fill"),
                        contentDescription = null,
                        tint = DoorTreeTheme.destructive
                    )
                    Text(
                        text = L("Delete"),
                        color = DoorTreeTheme.destructive,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        RequestRow(request = request, subtitle = subtitle, onClick = onClick)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = L("Delete pending request?"),
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = L("Are you sure you want to delete this pending request?"),
                    color = DoorTreeTheme.textSecondary
                )
            },
            confirmButton = {
                Text(
                    text = L("Delete"),
                    color = DoorTreeTheme.destructive,
                    modifier = Modifier.clickable {
                        showDeleteConfirmation = false
                        scope.launch {
                            runCatching {
                                onDeleteConfirmed()
                            }.onFailure { error ->
                                deleteError = error.message ?: L("Unable to delete request")
                            }
                        }
                    }
                )
            },
            dismissButton = {
                Text(
                    text = L("Cancel"),
                    color = DoorTreeTheme.textSecondary,
                    modifier = Modifier.clickable { showDeleteConfirmation = false }
                )
            },
            containerColor = DoorTreeTheme.backgroundPrimary
        )
    }

    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = {
                Text(
                    text = L("Unable to delete request"),
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = message,
                    color = DoorTreeTheme.textSecondary
                )
            },
            confirmButton = {
                Text(
                    text = L("common.ok"),
                    color = DoorTreeTheme.textPrimary,
                    modifier = Modifier.clickable { deleteError = null }
                )
            },
            containerColor = DoorTreeTheme.backgroundPrimary
        )
    }
}

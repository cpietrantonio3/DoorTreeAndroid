package codewhale.doortreeandroid

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun MaintenanceDetailView(
    request: MaintenanceRequestItem,
    onClose: () -> Unit
) {
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(request.id, request.photos) {
        debugMaintenanceImageLog(
            "detail opened requestId=${request.id} photoCount=${request.photos.size} urls=${request.photos.joinToString(" | ")}"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .topSafeAreaPadding()
                .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderIconButton(systemName = "chevron.left", onClick = onClose)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = L("maintenance.detail.title"),
                        color = DoorTreeTheme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                    Text(text = request.id, color = DoorTreeTheme.textSecondary)
                }
                StatusBadge(status = request.status)
            }

            MaintenanceOverviewCard(request = request)
            MaintenanceIssueSection(request = request)
            MaintenanceSummarySection(request = request)

            if (request.internalNotes.isNotBlank()) {
                MaintenanceNotesSection(request = request)
            }

            if (request.photos.isNotEmpty()) {
                MaintenancePhotosSection(
                    request = request,
                    onOpenPhoto = { photoUrl -> selectedPhotoUrl = photoUrl }
                )
            }
        }
    }

    selectedPhotoUrl?.let { photoUrl ->
        MaintenancePhotoViewer(
            photoUrl = photoUrl,
            onClose = { selectedPhotoUrl = null }
        )
    }
}

@Composable
private fun MaintenanceOverviewCard(request: MaintenanceRequestItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = request.title, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${request.propertyName.ifBlank { "Property" }} · ${request.unit.ifBlank { "-" }}",
                color = DoorTreeTheme.textSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MaintenanceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.detail.priority"),
                value = request.priority
            )
            MaintenanceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.detail.cost_estimate"),
                value = request.costEstimate
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MaintenanceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.detail.requested_on"),
                value = request.submittedDate
            )
            MaintenanceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.detail.preferred_date"),
                value = request.preferredDate
            )
        }
    }
}

@Composable
private fun MaintenanceMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .liquidGlassSurface(cornerRadius = 18.dp, tint = DoorTreeTheme.barGlassTint)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Text(text = value.ifBlank { "-" }, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MaintenanceIssueSection(request: MaintenanceRequestItem) {
    MaintenanceSection(title = L("maintenance.detail.issue")) {
        Text(text = request.details, color = DoorTreeTheme.textSecondary)
    }
}

@Composable
private fun MaintenanceSummarySection(request: MaintenanceRequestItem) {
    MaintenanceSection(title = L("maintenance.detail.summary")) {
        MaintenanceDetailRow(label = L("maintenance.detail.category"), value = request.category)
        MaintenanceDetailRow(label = L("maintenance.detail.status"), value = request.status.localizedLabel)
        MaintenanceDetailRow(
            label = L("maintenance.detail.assigned_to"),
            value = request.assignedTo.ifBlank { "-" }
        )
        MaintenanceDetailRow(
            label = L("maintenance.detail.property"),
            value = request.propertyName.ifBlank { "-" }
        )
        MaintenanceDetailRow(label = L("maintenance.detail.unit"), value = request.unit.ifBlank { "-" })
        MaintenanceDetailRow(label = L("maintenance.detail.tenant"), value = request.tenantName.ifBlank { "-" })
        MaintenanceDetailRow(label = L("maintenance.detail.created_at"), value = request.createdAt.ifBlank { "-" })
        MaintenanceDetailRow(label = L("maintenance.detail.updated_at"), value = request.updatedAt.ifBlank { "-" })
    }
}

@Composable
private fun MaintenanceNotesSection(request: MaintenanceRequestItem) {
    MaintenanceSection(title = L("maintenance.detail.internal_notes")) {
        Text(text = request.internalNotes, color = DoorTreeTheme.textSecondary)
    }
}

@Composable
private fun MaintenancePhotosSection(
    request: MaintenanceRequestItem,
    onOpenPhoto: (String) -> Unit
) {
    MaintenanceSection(title = L("maintenance.detail.photos")) {
        request.photos.forEach { photoUrl ->
            debugMaintenanceImageLog("detail rendering photo url=$photoUrl")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassSurface(cornerRadius = 16.dp, tint = DoorTreeTheme.barGlassTint)
                    .clickable {
                        debugMaintenanceImageLog("detail tapped photo url=$photoUrl")
                        onOpenPhoto(photoUrl)
                    }
                    .padding(10.dp)
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .background(DoorTreeTheme.backgroundSecondary, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        debugMaintenanceImageLog("detail photo success url=$photoUrl")
                    },
                    onError = { state ->
                        debugMaintenanceImageLog("detail photo failure url=$photoUrl error=${state.result.throwable.message.orEmpty()}")
                    },
                    onLoading = {
                        debugMaintenanceImageLog("detail photo loading url=$photoUrl")
                    }
                )
            }
        }
    }
}

@Composable
private fun MaintenanceSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 22.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MaintenanceDetailRow(
    label: String,
    value: String,
    valueColor: Color = DoorTreeTheme.textPrimary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MaintenancePhotoViewer(
    photoUrl: String,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = DoorTreeTheme.screenHorizontalPadding)
            ) {
                HeaderIconButton(systemName = "xmark", onClick = onClose)
            }
        }
    }
}

private fun debugMaintenanceImageLog(message: String) {
    Log.d("MaintenanceImages", message)
}

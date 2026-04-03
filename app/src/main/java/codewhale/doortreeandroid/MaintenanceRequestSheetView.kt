@file:OptIn(ExperimentalFoundationApi::class)

package codewhale.doortreeandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch

private object Limits {
    const val maxPhotos = 5
    val categoryTileHeight = 78.dp
}

private const val RefundRequestLabel = "Refund Request"

@Composable
fun MaintenanceRequestSheetView(
    tenantDataStore: TenantDataStore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val categoryPages = remember { MaintenanceCategory.entries.chunked(9) }
    val pagerState = rememberPagerState { categoryPages.size }
    val coroutineScope = rememberCoroutineScope()

    var selectedCategory by rememberSaveable { mutableStateOf<MaintenanceCategory?>(null) }
    var isRefundRequest by rememberSaveable { mutableStateOf(false) }
    var refundAmount by rememberSaveable { mutableStateOf("") }
    var otherCategoryName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedPriority by rememberSaveable { mutableStateOf(MaintenancePriority.Low) }
    var attachedPhotos by remember { mutableStateOf<List<MaintenancePhotoUpload>>(emptyList()) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var photoError by rememberSaveable { mutableStateOf<String?>(null) }
    var submissionError by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.toMaintenancePhotoUpload()?.let { photo ->
            attachedPhotos = appendPhotos(attachedPhotos, listOf(photo), Limits.maxPhotos)
            photoError = null
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            photoError = "Enable Camera access to take a maintenance photo."
        }
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(Limits.maxPhotos)
    ) { uris ->
        val remainingSlots = Limits.maxPhotos - attachedPhotos.size
        val pickedPhotos = uris
            .take(remainingSlots)
            .mapNotNull { uri -> uri.toMaintenancePhotoUpload(context) }

        if (pickedPhotos.isNotEmpty()) {
            attachedPhotos = appendPhotos(attachedPhotos, pickedPhotos, Limits.maxPhotos)
            photoError = null
        }
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != MaintenanceCategory.Other) {
            otherCategoryName = ""
        }

        val pageIndex = selectedCategory?.let { category ->
            categoryPages.indexOfFirst { page -> category in page }.takeIf { it >= 0 }
        }
        if (pageIndex != null && pagerState.currentPage != pageIndex) {
            pagerState.animateScrollToPage(pageIndex)
        }
    }

    LaunchedEffect(isRefundRequest) {
        if (isRefundRequest) {
            selectedCategory = null
            otherCategoryName = ""
        } else {
            refundAmount = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(
                start = DoorTreeTheme.screenHorizontalPadding,
                top = 8.dp,
                end = DoorTreeTheme.screenHorizontalPadding,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = L("maintenance.request_sheet.title"),
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = L("maintenance.section.category"), color = DoorTreeTheme.textSecondary)

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = selectedCategory == null && !isRefundRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(266.dp)
            ) { pageIndex ->
                CategoryPage(
                    categories = categoryPages[pageIndex],
                    selectedCategory = selectedCategory,
                    enabled = !isRefundRequest,
                    onSelectCategory = { category ->
                        selectedCategory = if (selectedCategory == category) null else category
                    }
                )
            }

            if (categoryPages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    categoryPages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(if (pagerState.currentPage == index) 18.dp else 6.dp)
                                .height(6.dp)
                                .background(
                                    if (pagerState.currentPage == index) DoorTreeTheme.gradientStart else DoorTreeTheme.cardBorder,
                                    RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }

            RefundRequestButton(
                isSelected = isRefundRequest,
                enabled = selectedCategory == null || isRefundRequest,
                onClick = { isRefundRequest = !isRefundRequest }
            )

            if (isRefundRequest) {
                Text(text = L("maintenance.refunds.description"), color = DoorTreeTheme.textSecondary)
                GlassInputField(
                    value = refundAmount,
                    onValueChange = { refundAmount = it },
                    placeholder = L("common.amount"),
                    leadingIcon = "dollarsign.circle.fill"
                )
            }

            if (selectedCategory == MaintenanceCategory.Other && !isRefundRequest) {
                Text(text = L("maintenance.section.specify_category"), color = DoorTreeTheme.textSecondary)
                GlassInputField(
                    value = otherCategoryName,
                    onValueChange = { otherCategoryName = it.take(25) },
                    placeholder = L("maintenance.other_category.placeholder")
                )
                Row {
                    Text(text = L("maintenance.other_category.required"), color = DoorTreeTheme.textSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = LF("maintenance.other_category.count", otherCategoryName.length, 25), color = DoorTreeTheme.textSecondary)
                }
            }

            Text(text = L("maintenance.section.description"), color = DoorTreeTheme.textSecondary)
            GlassInputField(
                value = description,
                onValueChange = { description = it },
                placeholder = L("maintenance.description_placeholder"),
                singleLine = false
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoSourceButton(
                    modifier = Modifier.weight(1f),
                    icon = "camera.fill",
                    title = "Take Photo",
                    enabled = attachedPhotos.size < Limits.maxPhotos && !isSubmitting,
                    onClick = {
                        photoError = null
                        when {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                                cameraLauncher.launch(null)
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                )

                PhotoSourceButton(
                    modifier = Modifier.weight(1f),
                    icon = "photo",
                    title = "Library",
                    enabled = attachedPhotos.size < Limits.maxPhotos && !isSubmitting,
                    onClick = {
                        photoError = null
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            Text(
                text = LF("maintenance.photos.count", attachedPhotos.size, Limits.maxPhotos),
                color = DoorTreeTheme.textSecondary
            )

            if (attachedPhotos.isNotEmpty()) {
                AttachedPhotoPreviews(
                    photos = attachedPhotos,
                    onRemovePhoto = { index ->
                        attachedPhotos = attachedPhotos.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            photoError?.takeIf { it.isNotBlank() }?.let { error ->
                Text(text = error, color = DoorTreeTheme.destructive)
            }

            Text(text = L("maintenance.section.priority"), color = DoorTreeTheme.textSecondary)
            MaintenancePriority.entries.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { priority ->
                        Text(
                            text = priority.localizedTitle,
                            color = if (selectedPriority == priority) DoorTreeTheme.textPrimary else DoorTreeTheme.textSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedPriority == priority) DoorTreeTheme.gradientStart.copy(alpha = 0.22f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPriority = priority }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            submissionError?.takeIf { it.isNotBlank() }?.let { error ->
                Text(text = error, color = DoorTreeTheme.destructive)
            }

            GradientButton(
                title = L("maintenance.submit_request"),
                enabled = canSubmit(selectedCategory, otherCategoryName, description, isRefundRequest, refundAmount) && !isSubmitting,
                onClick = {
                    val category = selectedCategory ?: if (isRefundRequest) MaintenanceCategory.Other else return@GradientButton
                    submissionError = null
                    isSubmitting = true

                    coroutineScope.launch {
                        runCatching {
                            tenantDataStore.submitMaintenanceRequest(
                                category = category,
                                customCategoryName = if (isRefundRequest) RefundRequestLabel else otherCategoryName,
                                description = description,
                                priority = selectedPriority,
                                refundAmount = refundAmount,
                                isRefundRequest = isRefundRequest,
                                photos = attachedPhotos
                            )
                        }.onSuccess {
                            isSubmitting = false
                            onDismiss()
                        }.onFailure { error ->
                            isSubmitting = false
                            submissionError = error.message ?: "Unable to submit maintenance request."
                        }
                    }
                }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        color = DoorTreeTheme.textPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPage(
    categories: List<MaintenanceCategory>,
    selectedCategory: MaintenanceCategory?,
    enabled: Boolean,
    onSelectCategory: (MaintenanceCategory) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { category ->
                    Box(modifier = Modifier.weight(1f)) {
                        MaintenanceRequestCategoryTile(
                            category = category,
                            isSelected = selectedCategory == category,
                            enabled = enabled,
                            onClick = { if (enabled) onSelectCategory(category) }
                        )
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun canSubmit(
    selectedCategory: MaintenanceCategory?,
    otherCategoryName: String,
    description: String,
    isRefundRequest: Boolean,
    refundAmount: String
): Boolean {
    val hasDescription = description.trim().isNotBlank()
    if (isRefundRequest) {
        return hasDescription && refundAmount.trim().isNotBlank()
    }

    return when (selectedCategory) {
        null -> false
        MaintenanceCategory.Other -> hasDescription && otherCategoryName.trim().isNotBlank()
        else -> hasDescription
    }
}

@Composable
private fun PhotoSourceButton(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .liquidGlassSurface(cornerRadius = 14.dp, interactive = true)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(systemIcon(icon), contentDescription = null, tint = DoorTreeTheme.textSecondary)
        Text(
            text = title,
            color = DoorTreeTheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AttachedPhotoPreviews(
    photos: List<MaintenancePhotoUpload>,
    onRemovePhoto: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        photos.forEachIndexed { index, photo ->
            Box {
                val bitmap = remember(photo.bytes) {
                    BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.size)
                }

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .liquidGlassSurface(cornerRadius = 18.dp)
                )

                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                IconButton(
                    onClick = { onRemovePhoto(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = systemIcon("xmark"),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceRequestCategoryTile(
    category: MaintenanceCategory,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .height(Limits.categoryTileHeight)
            .liquidGlassSurface(
                cornerRadius = 14.dp,
                interactive = true,
                tint = if (isSelected) DoorTreeTheme.gradientStart.copy(alpha = 0.16f) else Color.Unspecified
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = systemIcon(category.icon),
            contentDescription = null,
            tint = if (isSelected) DoorTreeTheme.gradientStart else DoorTreeTheme.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = category.localizedTitle,
            color = DoorTreeTheme.textPrimary,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RefundRequestButton(
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .liquidGlassSurface(
                cornerRadius = 14.dp,
                interactive = true,
                tint = if (isSelected) DoorTreeTheme.gradientStart.copy(alpha = 0.16f) else Color.Unspecified
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = systemIcon("arrow.uturn.backward.circle.fill"),
            contentDescription = null,
            tint = if (isSelected) DoorTreeTheme.textPrimary else DoorTreeTheme.gradientStart
        )
        Text(
            text = RefundRequestLabel,
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun appendPhotos(
    current: List<MaintenancePhotoUpload>,
    additions: List<MaintenancePhotoUpload>,
    maxPhotos: Int
): List<MaintenancePhotoUpload> {
    return (current + additions).take(maxPhotos)
}

private fun Bitmap.toMaintenancePhotoUpload(): MaintenancePhotoUpload? {
    val outputStream = ByteArrayOutputStream()
    val compressed = compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
    if (!compressed) {
        return null
    }

    return MaintenancePhotoUpload(
        bytes = outputStream.toByteArray(),
        contentType = "image/jpeg",
        fileExtension = "jpg"
    )
}

private fun Uri.toMaintenancePhotoUpload(context: Context): MaintenancePhotoUpload? {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(this)?.use { stream -> stream.readBytes() } ?: return null
    val mimeType = resolver.getType(this).orEmpty().ifBlank { "image/jpeg" }
    return MaintenancePhotoUpload(
        bytes = bytes,
        contentType = mimeType,
        fileExtension = fileExtensionForMimeType(mimeType)
    )
}

private fun fileExtensionForMimeType(mimeType: String): String {
    return when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }
}

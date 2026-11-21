package com.lensstock.app.ui.screens.scan

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.BuildConfig
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.domain.PairFormat
import com.lensstock.app.ui.common.AnimatedErrorText
import com.lensstock.app.ui.common.EmptyState
import com.lensstock.app.ui.common.rememberBounceInteraction
import kotlinx.coroutines.launch

private enum class ScanStep { CAMERA, CONFIRM }

@Composable
fun ScanScreen(mode: ScanMode, onBack: () -> Unit, viewModel: ScanViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var step by rememberSaveable { mutableStateOf(ScanStep.CAMERA) }
    var showManualAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScanLineItem?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    // Leaving the screen shouldn't silently throw away what's been scanned/added so far — only
    // an explicit "Discard" does that. From the confirm list, back just steps to the camera.
    val requestBack: () -> Unit = {
        when {
            step == ScanStep.CONFIRM -> step = ScanStep.CAMERA
            state.items.isNotEmpty() -> showDiscardConfirm = true
            else -> onBack()
        }
    }
    BackHandler(onBack = requestBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mode.title) },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (step) {
                ScanStep.CAMERA -> CameraStep(
                    state = state,
                    onCaptured = viewModel::onTextRecognized,
                    onCaptureStarted = viewModel::onCaptureStarted,
                    onCaptureFailed = viewModel::onCaptureFailed,
                    onReview = { step = ScanStep.CONFIRM },
                    onAddManually = { showManualAdd = true },
                )
                ScanStep.CONFIRM -> ConfirmationStep(
                    mode = mode,
                    state = state,
                    onIncrement = viewModel::increment,
                    onDecrement = viewModel::decrement,
                    onRemove = viewModel::remove,
                    onEdit = { editingItem = it },
                    onAddManually = { showManualAdd = true },
                    onBackToCamera = { step = ScanStep.CAMERA },
                    onConfirmSave = { viewModel.save(mode) },
                )
            }
        }
    }

    state.pendingScan?.let { parsed ->
        ManualAddDialog(
            types = state.availableTypes,
            initial = ScanLineItem(type = parsed.type, sph = parsed.sph, cyl = parsed.cyl, quantity = 1, axis = parsed.axis, add = parsed.add),
            title = "Confirm scanned lens",
            confirmLabel = "Add to list",
            onDismiss = viewModel::dismissPendingScan,
            onAddType = viewModel::addLensType,
            onRemoveType = viewModel::removeLensType,
            onLookupExisting = viewModel::findExistingStock,
            onAdd = { type, sph, cyl, axis, add, quantity, boxNumbers, min, max ->
                viewModel.confirmPendingScan(type, sph, cyl, axis, add, quantity, boxNumbers, min, max)
            },
        )
    }

    if (showManualAdd) {
        ManualAddDialog(
            types = state.availableTypes,
            onDismiss = { showManualAdd = false },
            onAddType = viewModel::addLensType,
            onRemoveType = viewModel::removeLensType,
            onLookupExisting = viewModel::findExistingStock,
            onAdd = { type, sph, cyl, axis, add, quantity, boxNumbers, min, max ->
                val added = viewModel.addManually(type, sph, cyl, axis, add, quantity, boxNumbers, min, max)
                if (added) showManualAdd = false
                added
            },
        )
    }

    editingItem?.let { item ->
        ManualAddDialog(
            types = state.availableTypes,
            initial = item,
            onDismiss = { editingItem = null },
            onAddType = viewModel::addLensType,
            onRemoveType = viewModel::removeLensType,
            onLookupExisting = viewModel::findExistingStock,
            onAdd = { type, sph, cyl, axis, add, quantity, boxNumbers, min, max ->
                val updated = viewModel.updateItem(item.id, type, sph, cyl, axis, add, quantity, boxNumbers, min, max)
                if (updated) editingItem = null
                updated
            },
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this list?") },
            text = { Text("${PairFormat.format(state.items.sumOf { it.quantity })} pair you've added won't be saved. Remove items one at a time instead if you only meant to drop some of them.") },
            confirmButton = {
                Button(
                    onClick = { showDiscardConfirm = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Discard") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardConfirm = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun CameraStep(
    state: ScanUiState,
    onCaptured: (String) -> Unit,
    onCaptureStarted: () -> Unit,
    onCaptureFailed: (String) -> Unit,
    onReview: () -> Unit,
    onAddManually: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberCameraPermissionState()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val itemCount = state.items.sumOf { it.quantity }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onCaptureStarted()
            scope.launch {
                try {
                    val text = readTextFromUri(context, uri)
                    onCaptured(text)
                } catch (e: Exception) {
                    onCaptureFailed(e.message ?: "Couldn't read that image")
                }
            }
        }
    }

    if (!permission.granted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Camera permission is needed to scan packets.")
            Button(onClick = permission.request, modifier = Modifier.padding(top = 16.dp)) {
                Text("Grant camera permission")
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Pick photo from gallery instead")
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(modifier = Modifier.fillMaxSize(), onImageCaptureReady = { imageCapture = it })

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (itemCount > 0) {
                Text("${PairFormat.format(itemCount)} pair scanned so far", style = MaterialTheme.typography.bodyMedium)
            }
            AnimatedErrorText(state.error)

            if (state.isProcessing) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        onCaptureStarted()
                        scope.launch {
                            try {
                                val text = capture.captureAndReadText(context)
                                onCaptured(text)
                            } catch (e: Exception) {
                                onCaptureFailed(e.message ?: "Couldn't capture the photo")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Capture packet")
                }
                // Dev/testing convenience only (e.g. feeding it sample packet photos) — gated out
                // of release builds so shop staff only ever see the camera and manual-entry paths.
                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Pick from gallery (dev)")
                    }
                }
            }

            // Nothing scanned yet: one tap straight into the manual-entry form — no detour
            // through an empty review screen first. Once something's been scanned, this instead
            // goes to review it, since there's now something to check before saving.
            OutlinedButton(
                onClick = { if (itemCount > 0) onReview() else onAddManually() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (itemCount == 0L) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                }
                Text(if (itemCount > 0) "Review & confirm (${PairFormat.format(itemCount)})" else "Add manually")
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    mode: ScanMode,
    state: ScanUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onEdit: (ScanLineItem) -> Unit,
    onAddManually: () -> Unit,
    onBackToCamera: () -> Unit,
    onConfirmSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.items.isEmpty()) {
            EmptyConfirmationState(mode = mode, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.id }) { item ->
                    ScanItemCard(
                        item = item,
                        onIncrement = { onIncrement(item.id) },
                        onDecrement = { onDecrement(item.id) },
                        onRemove = { onRemove(item.id) },
                        onEdit = { onEdit(item) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        AnimatedErrorText(state.error)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = onBackToCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                Text("Scan more")
            }
            OutlinedButton(onClick = onAddManually, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                Text("Add manually")
            }
        }

        val (confirmInteraction, confirmBounce) = rememberBounceInteraction()
        Button(
            onClick = onConfirmSave,
            enabled = state.items.isNotEmpty() && !state.isProcessing,
            interactionSource = confirmInteraction,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).then(confirmBounce),
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 8.dp))
            } else {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
            }
            Text("Confirm & Save (${mode.title})")
        }
    }
}

@Composable
private fun EmptyConfirmationState(mode: ScanMode, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Filled.Inventory2,
            title = "Nothing added yet",
            subtitle = if (mode == ScanMode.IN) "Scan a packet or add a lens manually to add stock" else "Scan a packet or add a lens manually to remove stock",
        )
    }
}

@Composable
private fun ScanItemCard(
    item: ScanLineItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(LensTypes.label(item.type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.sph} / ${item.cyl}${item.axisAddSuffix()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.boxLabel()?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "${PairFormat.format(item.quantity)} pair",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(onClick = onDecrement) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    FilledTonalIconButton(onClick = onIncrement) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

package com.lensstock.app.ui.screens.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.data.model.LensTypes
import kotlinx.coroutines.launch

private enum class ScanStep { CAMERA, CONFIRM }

@Composable
fun ScanScreen(mode: ScanMode, onBack: () -> Unit, viewModel: ScanViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var step by rememberSaveable { mutableStateOf(ScanStep.CAMERA) }
    var showManualAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScanLineItem?>(null) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mode.title) },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("Back") } },
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

    if (showManualAdd) {
        ManualAddDialog(
            types = state.availableTypes,
            onDismiss = { showManualAdd = false },
            onAddType = viewModel::addLensType,
            onAdd = { type, sph, cyl, axis, add ->
                val added = viewModel.addManually(type, sph, cyl, axis, add)
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
            onAdd = { type, sph, cyl, axis, add ->
                val updated = viewModel.updateItem(item.id, type, sph, cyl, axis, add)
                if (updated) editingItem = null
                updated
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
                Text("$itemCount packet(s) scanned so far", style = MaterialTheme.typography.bodyMedium)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

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
                    Text("Capture packet")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Pick from gallery")
                }
            }

            OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
                Text(if (itemCount > 0) "Review & confirm ($itemCount)" else "Add manually / review")
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
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = { it.id }) { item ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${LensTypes.label(item.type)}  ${item.sph} / ${item.cyl}${item.axisAddSuffix()}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { onEdit(item) },
                                contentPadding = PaddingValues(horizontal = 14.dp),
                            ) { Text("Edit") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("×${item.quantity}", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { onDecrement(item.id) },
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                ) { Text("−") }
                                OutlinedButton(
                                    onClick = { onIncrement(item.id) },
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                ) { Text("+") }
                                OutlinedButton(
                                    onClick = { onRemove(item.id) },
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                ) { Text("✕") }
                            }
                        }
                    }
                }
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp)) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = onBackToCamera, modifier = Modifier.weight(1f)) { Text("Scan more") }
            OutlinedButton(onClick = onAddManually, modifier = Modifier.weight(1f)) { Text("Add manually") }
        }

        Button(
            onClick = onConfirmSave,
            enabled = state.items.isNotEmpty() && !state.isProcessing,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text("Confirm & Save (${mode.title})")
        }
    }
}

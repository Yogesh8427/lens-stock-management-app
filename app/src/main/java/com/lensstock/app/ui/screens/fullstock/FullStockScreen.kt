package com.lensstock.app.ui.screens.fullstock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.data.model.Lens
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.domain.PairFormat
import com.lensstock.app.ui.common.AnimatedErrorText
import com.lensstock.app.ui.common.BoxNumbersField
import com.lensstock.app.ui.common.DialogHeader
import com.lensstock.app.ui.common.EmptyState
import com.lensstock.app.ui.common.LabeledField
import com.lensstock.app.ui.common.LensStatusRow
import com.lensstock.app.ui.common.PillButton
import com.lensstock.app.ui.common.QuantityCard
import com.lensstock.app.ui.common.SectionLabel
import com.lensstock.app.ui.common.SignedPowerField
import com.lensstock.app.ui.common.StockStatus
import com.lensstock.app.ui.common.ThresholdField
import com.lensstock.app.ui.common.axisAddSuffix
import com.lensstock.app.ui.common.color
import com.lensstock.app.ui.common.icon
import com.lensstock.app.ui.common.powerLabel
import com.lensstock.app.ui.common.stockStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullStockScreen(onBack: () -> Unit, viewModel: FullStockViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var editingLens by remember { mutableStateOf<Lens?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full Stock") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableTypes.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.onTypeChange(type) },
                        shape = CircleShape,
                        label = {
                            Text(
                                LensTypes.label(type),
                                fontWeight = if (state.selectedType == type) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = if (state.selectedType == type) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            Legend(modifier = Modifier.padding(top = 12.dp))

            AnimatedErrorText(state.error)

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.rows.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = "No ${LensTypes.label(state.selectedType)} stock yet",
                        subtitle = "Scan or add a lens to get started",
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.rows, key = { it.id }) { lens ->
                        LensStatusRow(lens = lens, onClick = { editingLens = lens }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }

    editingLens?.let { lens ->
        EditLensSheet(
            lens = lens,
            availableTypes = state.availableTypes,
            onDismiss = { editingLens = null },
            onSave = { type, sph, cyl, axis, add, quantity, boxNumbers, min, max ->
                val saved = viewModel.editLens(lens, type, sph, cyl, axis, add, quantity, boxNumbers, min, max)
                if (saved) editingLens = null
                saved
            },
            onDelete = {
                viewModel.deleteLens(lens)
                editingLens = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendItem(StockStatus.LOW, "Below min")
        LegendItem(StockStatus.CAUTION, "Low (near min)")
        LegendItem(StockStatus.FULL, "At/above max")
    }
}

@Composable
private fun LegendItem(status: StockStatus, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(status.icon(), contentDescription = null, tint = status.color(), modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Full edit for an existing stock row — not just thresholds: type, power, quantity, boxes, and
 * min/max can all be changed directly here instead of only via Scan In/Out.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditLensSheet(
    lens: Lens,
    availableTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        type: String, sph: String, cyl: String, axis: String, add: String,
        quantity: Long, boxNumbers: List<String>, min: Long, max: Long,
    ) -> Boolean,
    onDelete: () -> Unit,
) {
    var type by remember { mutableStateOf(lens.type) }
    var sph by remember { mutableStateOf(lens.sph) }
    var cyl by remember { mutableStateOf(lens.cyl.takeIf { it != "0.00" } ?: "") }
    var axis by remember { mutableStateOf(lens.axis.takeIf { it != 0L }?.toString() ?: "") }
    var add by remember { mutableStateOf(lens.add) }
    var quantity by remember { mutableStateOf(lens.quantity) }
    var boxNumbers by remember { mutableStateOf(lens.boxNumbers) }
    var min by remember { mutableStateOf(lens.minQuantity.toString()) }
    var max by remember { mutableStateOf(lens.maxQuantity.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(lens.axis != 0L || lens.add.isNotBlank()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DialogHeader(icon = Icons.Filled.Edit, title = "Edit lens", subtitle = "Change anything about this stock row", onClose = dismiss)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Lens type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableTypes.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            shape = CircleShape,
                            label = { Text(LensTypes.label(option), fontWeight = if (type == option) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (type == option) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Prescription (power)")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SignedPowerField("SPH", "e.g. 2.00", sph, { sph = it }, Modifier.weight(1f))
                    SignedPowerField("CYL", "e.g. 0.75", cyl, { cyl = it }, Modifier.weight(1f))
                }
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        if (showAdvanced) "Hide AXIS / ADD" else "Toric or bifocal? Add AXIS / ADD",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField("AXIS", "e.g. 180", axis, { axis = it }, Modifier.weight(1f))
                        LabeledField("ADD", "e.g. +1.00", add, { add = it }, Modifier.weight(1f))
                    }
                }
            }

            QuantityCard(
                quantity = quantity,
                onDecrease = { if (quantity > 0) quantity-- },
                onIncrease = { quantity++ },
                onQuantityChange = { text -> text.toLongOrNull()?.let { if (it >= 0) quantity = it } },
                onDecreasePair = { quantity = (quantity - 2).coerceAtLeast(0) },
                onIncreasePair = { quantity += 2 },
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Boxes")
                BoxNumbersField(boxNumbers = boxNumbers, onBoxNumbersChange = { boxNumbers = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Stock thresholds")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThresholdField("Minimum stock", "e.g. 2", min, { min = it }, Modifier.weight(1f))
                    ThresholdField("Maximum stock", "e.g. 10", max, { max = it }, Modifier.weight(1f))
                }
            }

            AnimatedErrorText(error)

            TextButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    "Stop tracking — remove permanently",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = dismiss) { Text("Cancel") }
                PillButton(
                    text = "Save",
                    icon = Icons.Filled.Check,
                    onClick = {
                        val minVal = min.trim().toLongOrNull()
                        val maxVal = max.trim().toLongOrNull()
                        when {
                            minVal == null || minVal < 0 -> error = "Enter a valid minimum"
                            maxVal == null || maxVal < minVal -> error = "Maximum must be ≥ minimum"
                            else -> {
                                val saved = onSave(type, sph, cyl, axis, add, quantity, boxNumbers, minVal, maxVal)
                                if (saved) dismiss() else error = "Enter a valid power in steps of 0.25"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Stop tracking this lens?") },
            text = {
                Text(
                    "${LensTypes.label(lens.type)} ${lens.powerLabel()}${lens.axisAddSuffix()} will be removed from " +
                        "Full Stock permanently. This can't be undone.",
                )
            },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

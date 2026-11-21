package com.lensstock.app.ui.screens.scan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.ui.common.AlreadyInBoxesRow
import com.lensstock.app.ui.common.AnimatedErrorText
import com.lensstock.app.ui.common.BoxNumbersField
import com.lensstock.app.ui.common.DialogHeader
import com.lensstock.app.ui.common.IconField
import com.lensstock.app.ui.common.LabeledField
import com.lensstock.app.ui.common.PillButton
import com.lensstock.app.ui.common.PillTonalButton
import com.lensstock.app.ui.common.QuantityCard
import com.lensstock.app.ui.common.SectionLabel
import com.lensstock.app.ui.common.SignedPowerField
import com.lensstock.app.ui.common.ThresholdField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualAddDialog(
    types: List<String>,
    initial: ScanLineItem? = null,
    title: String = if (initial != null) "Edit lens" else "Add manually",
    confirmLabel: String = if (initial != null) "Save" else "Add",
    headerIcon: ImageVector = when {
        confirmLabel == "Add to list" -> Icons.Filled.CameraAlt
        initial != null -> Icons.Filled.Edit
        else -> Icons.Filled.AddCircle
    },
    onDismiss: () -> Unit,
    onAddType: (String) -> Unit,
    onRemoveType: (String, onResult: (Boolean) -> Unit) -> Unit,
    onLookupExisting: suspend (type: String, sph: String, cyl: String, axis: String, add: String) -> ExistingStockInfo? =
        { _, _, _, _, _ -> null },
    onAdd: (
        type: String, sph: String, cyl: String, axis: String, add: String,
        quantity: Long, boxNumbers: List<String>, minQuantity: Long, maxQuantity: Long,
    ) -> Boolean,
) {
    var type by remember { mutableStateOf(initial?.type ?: types.firstOrNull() ?: LensTypes.NORMAL) }
    var sph by remember { mutableStateOf(initial?.sph ?: "") }
    var cyl by remember { mutableStateOf(initial?.cyl?.takeIf { it != "0.00" } ?: "") }
    var axis by remember { mutableStateOf(initial?.axis?.takeIf { it != 0L }?.toString() ?: "") }
    var add by remember { mutableStateOf(initial?.add ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity?.takeIf { it > 0 } ?: 1L) }
    var boxNumbers by remember { mutableStateOf(initial?.boxNumbers ?: emptyList()) }
    var minQuantity by remember { mutableStateOf((initial?.minQuantity ?: 2L).toString()) }
    var maxQuantity by remember { mutableStateOf((initial?.maxQuantity ?: 10L).toString()) }
    // Boxes this exact power is already stocked in, shown for context only — never merged into
    // [boxNumbers] above, since a stock write only ever appends box numbers and this dialog can't
    // actually retract one that's already on record (see [AlreadyInBoxesRow]).
    var existingBoxNumbers by remember { mutableStateOf<List<String>>(emptyList()) }
    // Once this power already has stock, its thresholds are fetched and shown here automatically
    // — but only until the shop owner actually types over one, so a deliberate edit never gets
    // silently clobbered by the next lookup (e.g. after tweaking the power again).
    var thresholdsAutofilled by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewType by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }
    // Most lenses are plain single-vision — hide AXIS/ADD by default so the common case looks
    // short and quick, and only expand them when there's already a toric/bifocal value to show.
    var showAdvanced by remember { mutableStateOf((initial?.axis ?: 0L) != 0L || !initial?.add.isNullOrBlank()) }

    LaunchedEffect(type, sph, cyl, axis, add) {
        delay(300)
        val found = onLookupExisting(type, sph, cyl, axis, add)
        existingBoxNumbers = found?.boxNumbers ?: emptyList()
        if (thresholdsAutofilled) {
            minQuantity = (found?.minQuantity ?: 2L).toString()
            maxQuantity = (found?.maxQuantity ?: 10L).toString()
        }
    }

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
            DialogHeader(icon = headerIcon, title = title, subtitle = "Enter lens prescription details", onClose = dismiss)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Lens type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            shape = CircleShape,
                            label = { Text(LensTypes.label(option), fontWeight = if (type == option) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (type == option) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            trailingIcon = if (option !in LensTypes.builtIns) {
                                {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove type",
                                        modifier = Modifier.width(16.dp).clickable {
                                            onRemoveType(option) { success ->
                                                if (!success) {
                                                    error = "Can't remove '${LensTypes.label(option)}' — it still has stock"
                                                } else if (type == option) {
                                                    type = types.firstOrNull { it != option } ?: LensTypes.NORMAL
                                                }
                                            }
                                        },
                                    )
                                }
                            } else null,
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showNewType = true },
                        shape = CircleShape,
                        label = { Text("New type") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
                AnimatedVisibility(
                    visible = showNewType,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconField(
                            icon = Icons.Filled.Layers,
                            placeholder = "New type name",
                            value = newTypeName,
                            onValueChange = { newTypeName = it },
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f),
                        )
                        PillTonalButton(
                            text = "Create",
                            enabled = newTypeName.isNotBlank(),
                            onClick = {
                                if (newTypeName.isNotBlank()) {
                                    onAddType(newTypeName)
                                    type = newTypeName.trim()
                                    newTypeName = ""
                                    showNewType = false
                                }
                            },
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
                onDecrease = { if (quantity > 1) quantity-- },
                onIncrease = { quantity++ },
                onQuantityChange = { text -> text.toLongOrNull()?.let { if (it > 0) quantity = it } },
                onDecreasePair = { quantity = (quantity - 2).coerceAtLeast(1) },
                onIncreasePair = { quantity += 2 },
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Boxes")
                AlreadyInBoxesRow(existingBoxNumbers)
                BoxNumbersField(boxNumbers = boxNumbers, onBoxNumbersChange = { boxNumbers = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Min / max stock")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThresholdField(
                        "Minimum stock", "e.g. 2", minQuantity,
                        { minQuantity = it; thresholdsAutofilled = false },
                        Modifier.weight(1f),
                    )
                    ThresholdField(
                        "Maximum stock", "e.g. 10", maxQuantity,
                        { maxQuantity = it; thresholdsAutofilled = false },
                        Modifier.weight(1f),
                    )
                }
            }

            AnimatedErrorText(error)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = dismiss) { Text("Cancel") }
                PillButton(
                    text = confirmLabel,
                    icon = Icons.Filled.Check,
                    onClick = {
                        val minVal = minQuantity.trim().toLongOrNull()
                        val maxVal = maxQuantity.trim().toLongOrNull()
                        when {
                            minVal == null || minVal < 0 -> error = "Enter a valid minimum stock"
                            maxVal == null || maxVal < minVal -> error = "Maximum stock must be ≥ minimum"
                            onAdd(type, sph, cyl, axis, add, quantity, boxNumbers, minVal, maxVal) -> dismiss()
                            else -> error = "Enter a valid power in steps of 0.25"
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
    }
}

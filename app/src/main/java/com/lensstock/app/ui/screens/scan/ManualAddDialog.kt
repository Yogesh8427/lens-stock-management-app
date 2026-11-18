package com.lensstock.app.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lensstock.app.data.model.LensTypes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualAddDialog(
    types: List<String>,
    initial: ScanLineItem? = null,
    onDismiss: () -> Unit,
    onAddType: (String) -> Unit,
    onAdd: (type: String, sph: String, cyl: String, axis: String, add: String) -> Boolean,
) {
    var type by remember { mutableStateOf(initial?.type ?: types.firstOrNull() ?: LensTypes.NORMAL) }
    var sph by remember { mutableStateOf(initial?.sph ?: "") }
    var cyl by remember { mutableStateOf(initial?.cyl?.takeIf { it != "0.00" } ?: "") }
    var axis by remember { mutableStateOf(initial?.axis?.takeIf { it != 0L }?.toString() ?: "") }
    var add by remember { mutableStateOf(initial?.add ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewType by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Edit lens" else "Add manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(LensTypes.label(option)) },
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showNewType = true },
                        label = { Text("+ New type") },
                    )
                }
                if (showNewType) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTypeName,
                            onValueChange = { newTypeName = it },
                            label = { Text("New type name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TextButton(onClick = {
                        if (newTypeName.isNotBlank()) {
                            onAddType(newTypeName)
                            type = newTypeName.trim()
                            newTypeName = ""
                            showNewType = false
                        }
                    }) { Text("Create type") }
                }
                OutlinedTextField(
                    value = sph,
                    onValueChange = { sph = it },
                    label = { Text("SPH, e.g. -1.25") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cyl,
                    onValueChange = { cyl = it },
                    label = { Text("CYL, e.g. -0.50 (optional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = axis,
                    onValueChange = { axis = it },
                    label = { Text("AXIS, e.g. 180 (optional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = add,
                    onValueChange = { add = it },
                    label = { Text("ADD, e.g. +2.50 (bifocal, optional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (onAdd(type, sph, cyl, axis, add)) onDismiss() else error = "Enter a valid power in steps of 0.25"
            }) { Text(if (initial != null) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

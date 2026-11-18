package com.lensstock.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.data.model.LensTypes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableTypes.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(LensTypes.label(type)) },
                    )
                }
            }

            OutlinedTextField(
                value = state.sph,
                onValueChange = viewModel::onSphChange,
                label = { Text("SPH, e.g. -1.25") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.cyl,
                onValueChange = viewModel::onCylChange,
                label = { Text("CYL, e.g. -0.50 (optional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.axis,
                onValueChange = viewModel::onAxisChange,
                label = { Text("AXIS, e.g. 180 (optional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.add,
                onValueChange = viewModel::onAddChange,
                label = { Text("ADD, e.g. +2.50 (bifocal, optional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth()) {
                Text("Search")
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.searched) {
                val result = state.result
                val axisAddSuffix = buildString {
                    if (state.axisValue != 0L) append(" X${state.axisValue}")
                    if (state.add.isNotBlank()) append(" ADD${state.add}")
                }
                if (result != null) {
                    Text(
                        "${LensTypes.label(state.type)} ${state.sph}/${state.cyl}$axisAddSuffix: ${result.quantity} pcs, Box ${result.boxNumber.ifBlank { "unassigned" }}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    Text(
                        "Not in stock: ${LensTypes.label(state.type)} ${state.sph}/${state.cyl}$axisAddSuffix",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { viewModel.adjust(-1) }) { Text("−1") }
                    OutlinedButton(onClick = { viewModel.adjust(1) }) { Text("+1") }
                }
            }
        }
    }
}

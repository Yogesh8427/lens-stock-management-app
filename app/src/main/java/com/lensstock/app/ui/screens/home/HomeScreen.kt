package com.lensstock.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onSearch: () -> Unit,
    onToOrder: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lens Stock") },
                actions = {
                    Button(onClick = onSettings) { Text("Settings") }
                },
            )
        },
    ) { padding ->
        HomeButtons(padding, onScanIn, onScanOut, onSearch, onToOrder)
    }
}

@Composable
private fun HomeButtons(
    padding: PaddingValues,
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onSearch: () -> Unit,
    onToOrder: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BigButton("Add Lens", onScanIn)
        BigButton("Remove Lens", onScanOut)
        BigButton("Search", onSearch)
        BigButton("To Order", onToOrder)
    }
}

@Composable
private fun BigButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(72.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

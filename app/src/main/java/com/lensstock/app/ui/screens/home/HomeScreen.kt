package com.lensstock.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onSearch: () -> Unit,
    onToOrder: () -> Unit,
    onSettings: () -> Unit,
    onFullStock: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lens Stock") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        HomeActions(padding, onScanIn, onScanOut, onSearch, onToOrder, onFullStock)
    }
}

private data class HomeAction(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val containerColor: @Composable () -> Color,
    val onClick: () -> Unit,
)

@Composable
private fun HomeActions(
    padding: PaddingValues,
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onSearch: () -> Unit,
    onToOrder: () -> Unit,
    onFullStock: () -> Unit,
) {
    val actions = listOf(
        HomeAction("Add Lens", "Scan or enter packets in", Icons.Filled.AddCircle, { MaterialTheme.colorScheme.primaryContainer }, onScanIn),
        HomeAction("Remove Lens", "Scan or enter packets out", Icons.Filled.RemoveCircle, { MaterialTheme.colorScheme.secondaryContainer }, onScanOut),
        HomeAction("Search", "Look up a single power", Icons.Filled.Search, { MaterialTheme.colorScheme.tertiaryContainer }, onSearch),
        HomeAction("Full Stock", "Browse the whole power series", Icons.Filled.Inventory2, { MaterialTheme.colorScheme.primaryContainer }, onFullStock),
        HomeAction("To Order", "What's low or out of stock", Icons.Filled.ShoppingCart, { MaterialTheme.colorScheme.secondaryContainer }, onToOrder),
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        actions.forEach { action -> HomeActionCard(action) }
    }
}

@Composable
private fun HomeActionCard(action: HomeAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = action.containerColor()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(action.label, style = MaterialTheme.typography.titleLarge)
                Text(action.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

package com.lensstock.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lensstock.app.ui.common.rememberBounceInteraction

@Composable
fun HomeScreen(
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onSearch: () -> Unit,
    onToOrder: () -> Unit,
    onSettings: () -> Unit,
    onFullStock: () -> Unit,
) {
    Scaffold { padding ->
        HomeContent(padding, onScanIn, onScanOut, onSearch, onToOrder, onSettings, onFullStock)
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onSearch: () -> Unit,
    onToOrder: () -> Unit,
    onSettings: () -> Unit,
    onFullStock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp),
    ) {
        HomeHeader(onSettings = onSettings)

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BigActionTile(
                    label = "Add Lens",
                    subtitle = "Scan or enter packets in",
                    icon = Icons.Filled.AddCircle,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onScanIn,
                    modifier = Modifier.weight(1f),
                )
                BigActionTile(
                    label = "Remove Lens",
                    subtitle = "Scan or enter packets out",
                    icon = Icons.Filled.RemoveCircle,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onScanOut,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "MANAGE STOCK",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                CompactActionTile("Search", "Look up a single power", Icons.Filled.Search, onSearch)
                CompactActionTile("Full Stock", "Browse the whole power series", Icons.Filled.Inventory2, onFullStock)
                CompactActionTile("To Order", "What's low or out of stock", Icons.Filled.ShoppingCart, onToOrder)
            }
        }
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Lens Stock",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        IconButton(
            onClick = onSettings,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

/** A big, square-ish tile for the two actions used most — scanning a packet in or out. */
@Composable
private fun BigActionTile(
    label: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interactionSource, bounce) = rememberBounceInteraction()
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.aspectRatio(0.92f).then(bounce),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(shape = MaterialTheme.shapes.large, color = contentColor.copy(alpha = 0.14f)) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(30.dp))
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

/** A compact, full-width row for the secondary, less-frequent actions. */
@Composable
private fun CompactActionTile(label: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val (interactionSource, bounce) = rememberBounceInteraction()
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().then(bounce),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

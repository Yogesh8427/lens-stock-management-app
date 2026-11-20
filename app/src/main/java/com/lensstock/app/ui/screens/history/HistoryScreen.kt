package com.lensstock.app.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.data.model.ChangeReason
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.data.model.StockHistoryEntry
import com.lensstock.app.domain.PairFormat
import com.lensstock.app.ui.common.StatusFullColor
import com.lensstock.app.ui.common.StatusLowColor
import java.text.SimpleDateFormat
import java.util.Locale

private val AddColor = StatusFullColor
private val RemoveColor = StatusLowColor
private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.groups.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Nothing added or removed yet",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header_${group.label}") {
                            Text(
                                group.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items(group.entries, key = { it.id }) { entry ->
                            HistoryRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: StockHistoryEntry) {
    val isAddition = entry.change > 0
    val color = if (isAddition) AddColor else RemoveColor
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.1f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (isAddition) Icons.Filled.NorthEast else Icons.Filled.SouthWest,
                contentDescription = if (isAddition) "Added" else "Removed",
                tint = color,
            )
            Column {
                Text(
                    "${LensTypes.label(entry.type)} ${entry.powerLabel()}${entry.axisAddSuffix()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReasonChip(entry.reason, color)
                    Text(
                        entry.createdAt?.let { timeFormat.format(it) } ?: "just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry.byUser.isNotBlank()) {
                    Text(entry.byUser, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "${if (isAddition) "+" else "-"}${PairFormat.format(Math.abs(entry.change))} pair",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun ReasonChip(reason: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Text(
            reason.reasonLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun String.reasonLabel(): String = when (this) {
    ChangeReason.SCAN_IN -> "Scanned in"
    ChangeReason.SCAN_OUT -> "Scanned out"
    ChangeReason.MANUAL -> "Manual edit"
    ChangeReason.DAMAGED -> "Damaged"
    ChangeReason.UNDO -> "Undo"
    else -> this
}

/** "-1.25/-0.50" style label, matching [com.lensstock.app.ui.common.powerLabel] for a [Lens]. */
private fun StockHistoryEntry.powerLabel(): String = when {
    cyl == "0.00" -> "$sph SPH"
    sph == "0.00" -> "$cyl CYL"
    else -> "$sph/$cyl"
}

private fun StockHistoryEntry.axisAddSuffix(): String {
    val axisPart = if (axis != 0L) " X$axis" else ""
    val addPart = if (add.isNotBlank()) " ADD$add" else ""
    return axisPart + addPart
}

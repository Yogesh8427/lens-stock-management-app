package com.lensstock.app.ui.screens.search

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.ui.common.AnimatedErrorText
import com.lensstock.app.ui.common.LabeledField
import com.lensstock.app.ui.common.LensStatusRow
import com.lensstock.app.ui.common.PillButton
import com.lensstock.app.ui.common.SectionLabel
import com.lensstock.app.ui.common.SignedPowerField

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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Lens type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableTypes.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            shape = CircleShape,
                            label = {
                                Text(
                                    LensTypes.label(type),
                                    fontWeight = if (state.type == type) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            leadingIcon = if (state.type == type) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Search by power")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SignedPowerField("SPH", "e.g. 1.25", state.sph, viewModel::onSphChange, Modifier.weight(1f))
                    SignedPowerField("CYL", "e.g. 0.50", state.cyl, viewModel::onCylChange, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabeledField("AXIS", "e.g. 180", state.axis, viewModel::onAxisChange, Modifier.weight(1f))
                    LabeledField("ADD", "e.g. +2.50", state.add, viewModel::onAddChange, Modifier.weight(1f))
                }
            }

            PillButton(
                text = "Search",
                icon = Icons.Filled.Search,
                onClick = viewModel::search,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedErrorText(state.error)

            if (state.searched) {
                val result = state.result
                if (result != null) {
                    LensStatusRow(lens = result)
                } else {
                    val axisAddSuffix = buildString {
                        if (state.axisValue != 0L) append(" X${state.axisValue}")
                        if (state.add.isNotBlank()) append(" ADD${state.add}")
                    }
                    NotInStockCard(
                        powerLabel = "${LensTypes.label(state.type)} ${state.sph}/${state.cyl}$axisAddSuffix",
                    )
                }
            }
        }
    }
}

/** A friendlier, on-brand "nothing here" state instead of a plain line of red text. */
@Composable
private fun NotInStockCard(powerLabel: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                "Not in stock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                powerLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "This power hasn't been added to stock yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

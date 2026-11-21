package com.lensstock.app.ui.screens.toorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.ui.common.AnimatedErrorText
import com.lensstock.app.ui.common.EmptyState
import com.lensstock.app.ui.common.LensStatusRow

@Composable
fun ToOrderScreen(onBack: () -> Unit, viewModel: ToOrderViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To Order") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            AnimatedErrorText(state.error)

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.rows.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.CheckCircle,
                        title = "Fully stocked!",
                        subtitle = "Nothing is low or out of stock right now",
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.rows.groupBy { it.type }.forEach { (type, lenses) ->
                        item(key = "header_$type") {
                            Text(
                                LensTypes.label(type),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(lenses, key = { it.id }) { lens ->
                            LensStatusRow(lens = lens, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

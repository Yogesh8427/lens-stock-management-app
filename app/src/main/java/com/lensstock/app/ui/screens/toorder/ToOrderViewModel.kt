package com.lensstock.app.ui.screens.toorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensstock.app.data.model.Lens
import com.lensstock.app.data.repository.LensRepository
import com.lensstock.app.domain.PowerFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToOrderUiState(
    val rows: List<Lens> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class ToOrderViewModel : ViewModel() {
    private val repository = LensRepository()
    private val _uiState = MutableStateFlow(ToOrderUiState())
    val uiState: StateFlow<ToOrderUiState> = _uiState

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // Anything at or below its minimum needs ordering — not just lenses sitting at zero.
                val rows = repository.lowStock().sortedWith(
                    compareBy(
                        { it.type },
                        { PowerFormat.toDouble(it.sph) ?: 0.0 },
                        { PowerFormat.toDouble(it.cyl) ?: 0.0 },
                    ),
                )
                _uiState.update { it.copy(rows = rows, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load stock") }
            }
        }
    }
}

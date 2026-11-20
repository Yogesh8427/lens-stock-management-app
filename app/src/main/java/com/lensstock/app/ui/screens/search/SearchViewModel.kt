package com.lensstock.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensstock.app.data.model.Lens
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.data.repository.LensRepository
import com.lensstock.app.domain.PowerFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val type: String = LensTypes.NORMAL,
    val sph: String = "",
    val cyl: String = "",
    val axis: String = "",
    val add: String = "",
    val isSearching: Boolean = false,
    val result: Lens? = null,
    val searched: Boolean = false,
    val error: String? = null,
    val availableTypes: List<String> = LensTypes.builtIns,
) {
    val axisValue: Long get() = axis.toLongOrNull() ?: 0
}

class SearchViewModel : ViewModel() {
    private val repository = LensRepository()
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    init {
        viewModelScope.launch {
            val custom = repository.listCustomTypes()
            _uiState.update { it.copy(availableTypes = (LensTypes.builtIns + custom).distinct()) }
        }
    }

    fun onTypeChange(type: String) = _uiState.update { it.copy(type = type) }
    fun onSphChange(sph: String) = _uiState.update { it.copy(sph = sph) }
    fun onCylChange(cyl: String) = _uiState.update { it.copy(cyl = cyl) }
    fun onAxisChange(axis: String) = _uiState.update { it.copy(axis = axis) }
    fun onAddChange(add: String) = _uiState.update { it.copy(add = add) }

    fun search() {
        val state = _uiState.value
        val sph = normalizePower(state.sph) ?: run {
            _uiState.update { it.copy(error = "Enter a valid SPH, e.g. -1.25") }
            return
        }
        val cyl = if (state.cyl.isBlank()) "0.00" else normalizePower(state.cyl) ?: run {
            _uiState.update { it.copy(error = "Enter a valid CYL, e.g. -0.50") }
            return
        }
        // Blank AXIS means "any axis" — still finds a toric lens even without knowing its exact axis.
        val axis: Long? = if (state.axis.isBlank()) null else PowerFormat.normalizeAxis(state.axis) ?: run {
            _uiState.update { it.copy(error = "Enter a valid AXIS, 0-180") }
            return
        }
        val add = if (state.add.isBlank()) "" else normalizePower(state.add) ?: run {
            _uiState.update { it.copy(error = "Enter a valid ADD, e.g. +2.50") }
            return
        }

        _uiState.update { it.copy(isSearching = true, error = null) }
        viewModelScope.launch {
            try {
                val lens = repository.findLens(state.type, sph, cyl, axis, add)
                _uiState.update {
                    it.copy(isSearching = false, result = lens, searched = true, sph = sph, cyl = cyl, add = add)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, error = e.message ?: "Search failed") }
            }
        }
    }

    companion object {
        fun normalizePower(raw: String): String? = PowerFormat.normalize(raw)
    }
}

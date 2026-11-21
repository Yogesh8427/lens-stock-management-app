package com.lensstock.app.ui.screens.fullstock

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

data class FullStockUiState(
    val availableTypes: List<String> = LensTypes.builtIns,
    val selectedType: String = LensTypes.NORMAL,
    val rows: List<Lens> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class FullStockViewModel : ViewModel() {
    private val repository = LensRepository()
    private val _uiState = MutableStateFlow(FullStockUiState())
    val uiState: StateFlow<FullStockUiState> = _uiState

    private var allLenses: List<Lens> = emptyList()

    init {
        viewModelScope.launch {
            val custom = repository.listCustomTypes()
            _uiState.update { it.copy(availableTypes = (LensTypes.builtIns + custom).distinct()) }
        }
        load()
    }

    fun onTypeChange(type: String) {
        _uiState.update { it.copy(selectedType = type) }
        rebuildRows()
    }

    fun refresh() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                allLenses = repository.allStock()
                rebuildRows()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load stock") }
            }
        }
    }

    /** One full-width row per lens of the selected type, sorted low to high by SPH then CYL. */
    private fun rebuildRows() {
        val type = _uiState.value.selectedType
        val rows = allLenses
            .filter { it.type == type }
            .sortedWith(
                compareBy(
                    { PowerFormat.toDouble(it.sph) ?: 0.0 },
                    { PowerFormat.toDouble(it.cyl) ?: 0.0 },
                ),
            )
        _uiState.update { it.copy(rows = rows, isLoading = false, error = null) }
    }

    /**
     * Full edit of an existing row — type/power/quantity/boxes/thresholds can all change at
     * once. Returns false (and changes nothing) if the power isn't a valid multiple of 0.25.
     */
    fun editLens(
        lens: Lens,
        type: String,
        sph: String,
        cyl: String,
        axis: String,
        add: String,
        quantity: Long,
        boxNumbers: List<String>,
        minQuantity: Long,
        maxQuantity: Long,
    ): Boolean {
        val normSph = if (sph.isBlank()) "0.00" else PowerFormat.normalize(sph) ?: return false
        val normCyl = if (cyl.isBlank()) "0.00" else PowerFormat.normalize(cyl) ?: return false
        val normAxis = if (axis.isBlank()) 0L else PowerFormat.normalizeAxis(axis) ?: return false
        val normAdd = if (add.isBlank()) "" else PowerFormat.normalize(add) ?: return false

        repository.editLens(lens, type, normSph, normCyl, normAxis, normAdd, quantity, boxNumbers, minQuantity, maxQuantity)

        val newId = Lens.documentId(type, normSph, normCyl, normAxis, normAdd)
        allLenses = if (newId == lens.id) {
            allLenses.map {
                if (it.id == lens.id) {
                    it.copy(quantity = quantity, boxNumbers = boxNumbers, minQuantity = minQuantity, maxQuantity = maxQuantity)
                } else it
            }
        } else {
            val moved = lens.copy(
                id = newId, type = type, sph = normSph, cyl = normCyl, axis = normAxis, add = normAdd,
                quantity = (allLenses.find { it.id == newId }?.quantity ?: 0L) + quantity,
                boxNumbers = boxNumbers, minQuantity = minQuantity, maxQuantity = maxQuantity,
            )
            allLenses.filterNot { it.id == lens.id || it.id == newId } + moved
        }
        rebuildRows()
        return true
    }

    /** Stops tracking a discontinued power — removes it from Full Stock permanently. */
    fun deleteLens(lens: Lens) {
        repository.deleteLens(lens)
        allLenses = allLenses.filterNot { it.id == lens.id }
        rebuildRows()
    }
}

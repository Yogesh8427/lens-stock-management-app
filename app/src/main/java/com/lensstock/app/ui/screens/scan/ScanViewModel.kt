package com.lensstock.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.data.repository.LensRepository
import com.lensstock.app.domain.PowerFormat
import com.lensstock.app.ocr.LensParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val items: List<ScanLineItem> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val availableTypes: List<String> = LensTypes.builtIns,
)

class ScanViewModel : ViewModel() {
    private val repository = LensRepository()
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState

    init {
        viewModelScope.launch {
            val custom = repository.listCustomTypes()
            _uiState.update { it.copy(availableTypes = (LensTypes.builtIns + custom).distinct()) }
        }
    }

    /** Adds a shop-defined lens type (e.g. "Photochromic") so it shows up as a pickable type. */
    fun addLensType(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed in _uiState.value.availableTypes) return
        repository.addCustomType(trimmed)
        _uiState.update { it.copy(availableTypes = it.availableTypes + trimmed) }
    }

    fun onCaptureStarted() = _uiState.update { it.copy(isProcessing = true, error = null) }

    fun onCaptureFailed(message: String) = _uiState.update { it.copy(isProcessing = false, error = message) }

    fun onTextRecognized(text: String) {
        val parsed = LensParser.parse(text)
        _uiState.update { state ->
            if (parsed == null) {
                state.copy(isProcessing = false, error = "Couldn't read a power from that photo — try again or add manually")
            } else {
                state.copy(
                    items = mergeOrAdd(state.items, parsed.type, parsed.sph, parsed.cyl, parsed.axis, parsed.add),
                    isProcessing = false,
                    error = null,
                )
            }
        }
    }

    fun addManually(type: String, sph: String, cyl: String, axis: String = "", add: String = ""): Boolean {
        val normSph = PowerFormat.normalize(sph) ?: return false
        val normCyl = if (cyl.isBlank()) "0.00" else PowerFormat.normalize(cyl) ?: return false
        val normAxis = if (axis.isBlank()) 0L else PowerFormat.normalizeAxis(axis) ?: return false
        val normAdd = if (add.isBlank()) "" else PowerFormat.normalize(add) ?: return false
        _uiState.update { it.copy(items = mergeOrAdd(it.items, type, normSph, normCyl, normAxis, normAdd), error = null) }
        return true
    }

    /** Corrects a line item's type/power after a bad OCR read — merges into a match if one exists. */
    fun updateItem(id: String, type: String, sph: String, cyl: String, axis: String, add: String): Boolean {
        val normSph = PowerFormat.normalize(sph) ?: return false
        val normCyl = if (cyl.isBlank()) "0.00" else PowerFormat.normalize(cyl) ?: return false
        val normAxis = if (axis.isBlank()) 0L else PowerFormat.normalizeAxis(axis) ?: return false
        val normAdd = if (add.isBlank()) "" else PowerFormat.normalize(add) ?: return false
        _uiState.update { state ->
            val current = state.items.find { it.id == id } ?: return@update state
            val others = state.items.filterNot { it.id == id }
            val duplicate = others.find {
                it.type == type && it.sph == normSph && it.cyl == normCyl && it.axis == normAxis && it.add == normAdd
            }
            val updatedItems = if (duplicate != null) {
                others.map { if (it.id == duplicate.id) it.copy(quantity = it.quantity + current.quantity) else it }
            } else {
                others + current.copy(type = type, sph = normSph, cyl = normCyl, axis = normAxis, add = normAdd)
            }
            state.copy(items = updatedItems, error = null)
        }
        return true
    }

    fun increment(id: String) = updateQuantity(id, +1)
    fun decrement(id: String) = updateQuantity(id, -1)

    fun remove(id: String) = _uiState.update { state ->
        state.copy(items = state.items.filterNot { it.id == id })
    }

    private fun updateQuantity(id: String, delta: Long) {
        _uiState.update { state ->
            state.copy(
                items = state.items.mapNotNull { item ->
                    when {
                        item.id != id -> item
                        item.quantity + delta <= 0 -> null
                        else -> item.copy(quantity = item.quantity + delta)
                    }
                },
            )
        }
    }

    private fun mergeOrAdd(
        items: List<ScanLineItem>,
        type: String,
        sph: String,
        cyl: String,
        axis: Long = 0,
        add: String = "",
    ): List<ScanLineItem> {
        val existing = items.find { it.type == type && it.sph == sph && it.cyl == cyl && it.axis == axis && it.add == add }
        return if (existing != null) {
            items.map { if (it.id == existing.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            items + ScanLineItem(type = type, sph = sph, cyl = cyl, quantity = 1, axis = axis, add = add)
        }
    }

    fun save(mode: ScanMode) {
        val items = _uiState.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            if (mode == ScanMode.OUT) {
                val shortages = items.mapNotNull { item ->
                    val available = repository.findLens(item.type, item.sph, item.cyl, item.axis, item.add)?.quantity ?: 0
                    if (item.quantity > available) {
                        "${LensTypes.label(item.type)} ${item.sph}/${item.cyl} (have $available, need ${item.quantity})"
                    } else null
                }
                if (shortages.isNotEmpty()) {
                    _uiState.update { it.copy(error = "Not enough stock — " + shortages.joinToString("; ")) }
                    return@launch
                }
            }
            // Local-first: changeStock() queues the writes in Firestore's offline cache and returns
            // immediately, so there's nothing to await here — see the comment on changeStock().
            try {
                items.forEach { item ->
                    repository.changeStock(
                        item.type, item.sph, item.cyl, item.quantity * mode.sign, mode.reason,
                        axis = item.axis, add = item.add,
                    )
                }
                _uiState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Save failed") }
            }
        }
    }
}

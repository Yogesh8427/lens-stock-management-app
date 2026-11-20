package com.lensstock.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensstock.app.data.model.LensTypes
import com.lensstock.app.data.repository.LensRepository
import com.lensstock.app.domain.PowerFormat
import com.lensstock.app.ocr.LensParser
import com.lensstock.app.ocr.ParsedLens
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
    /** Set right after a successful OCR read — shown as a review dialog, not yet added to [items]. */
    val pendingScan: ParsedLens? = null,
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

    /** Removes a shop-defined type — fails (returns false) if any of its lenses still have stock. */
    fun removeLensType(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val removed = repository.removeCustomType(name)
            if (removed) _uiState.update { it.copy(availableTypes = it.availableTypes - name) }
            onResult(removed)
        }
    }

    fun onCaptureStarted() = _uiState.update { it.copy(isProcessing = true, error = null) }

    fun onCaptureFailed(message: String) = _uiState.update { it.copy(isProcessing = false, error = message) }

    /** OCR succeeded — hand it to the review dialog instead of adding it to [ScanUiState.items] directly. */
    fun onTextRecognized(text: String) {
        val parsed = LensParser.parse(text)
        _uiState.update { state ->
            if (parsed == null) {
                state.copy(isProcessing = false, error = "Couldn't read a power from that photo — try again or add manually")
            } else {
                state.copy(isProcessing = false, error = null, pendingScan = parsed)
            }
        }
    }

    /** Closes the review dialog without adding anything — e.g. the scan was of the wrong packet. */
    fun dismissPendingScan() = _uiState.update { it.copy(pendingScan = null) }

    /** User reviewed/corrected the scanned values in the dialog and confirmed — now it joins the list. */
    fun confirmPendingScan(
        type: String,
        sph: String,
        cyl: String,
        axis: String,
        add: String,
        quantity: Long,
        boxNumbers: List<String>,
    ): Boolean {
        val merged = normalizeAndMerge(_uiState.value.items, type, sph, cyl, axis, add, quantity, boxNumbers) ?: return false
        _uiState.update { it.copy(items = merged, pendingScan = null, error = null) }
        return true
    }

    fun addManually(
        type: String,
        sph: String,
        cyl: String,
        axis: String = "",
        add: String = "",
        quantity: Long = 1,
        boxNumbers: List<String> = emptyList(),
    ): Boolean {
        val merged = normalizeAndMerge(_uiState.value.items, type, sph, cyl, axis, add, quantity, boxNumbers) ?: return false
        _uiState.update { it.copy(items = merged, error = null) }
        return true
    }

    /** Corrects a line item's type/power/quantity/box after a bad OCR read — merges into a match if one exists. */
    fun updateItem(
        id: String,
        type: String,
        sph: String,
        cyl: String,
        axis: String,
        add: String,
        quantity: Long,
        boxNumbers: List<String>,
    ): Boolean {
        val normSph = PowerFormat.normalize(sph) ?: return false
        val normCyl = if (cyl.isBlank()) "0.00" else PowerFormat.normalize(cyl) ?: return false
        val normAxis = if (axis.isBlank()) 0L else PowerFormat.normalizeAxis(axis) ?: return false
        val normAdd = if (add.isBlank()) "" else PowerFormat.normalize(add) ?: return false
        val qty = quantity.coerceAtLeast(1)
        _uiState.update { state ->
            val current = state.items.find { it.id == id } ?: return@update state
            val others = state.items.filterNot { it.id == id }
            val duplicate = others.find {
                it.type == type && it.sph == normSph && it.cyl == normCyl && it.axis == normAxis && it.add == normAdd
            }
            val updatedItems = if (duplicate != null) {
                others.map {
                    if (it.id == duplicate.id) {
                        it.copy(quantity = it.quantity + qty, boxNumbers = (it.boxNumbers + boxNumbers).distinct())
                    } else it
                }
            } else {
                others + current.copy(
                    type = type, sph = normSph, cyl = normCyl, axis = normAxis, add = normAdd,
                    quantity = qty, boxNumbers = boxNumbers,
                )
            }
            state.copy(items = updatedItems, error = null)
        }
        return true
    }

    /** Validates/normalizes raw dialog input and merges it into [items]; null means invalid power. */
    private fun normalizeAndMerge(
        items: List<ScanLineItem>,
        type: String,
        sph: String,
        cyl: String,
        axis: String,
        add: String,
        quantity: Long,
        boxNumbers: List<String>,
    ): List<ScanLineItem>? {
        val normSph = PowerFormat.normalize(sph) ?: return null
        val normCyl = if (cyl.isBlank()) "0.00" else PowerFormat.normalize(cyl) ?: return null
        val normAxis = if (axis.isBlank()) 0L else PowerFormat.normalizeAxis(axis) ?: return null
        val normAdd = if (add.isBlank()) "" else PowerFormat.normalize(add) ?: return null
        return mergeOrAdd(items, type, normSph, normCyl, normAxis, normAdd, quantity.coerceAtLeast(1), boxNumbers)
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
        quantity: Long = 1,
        boxNumbers: List<String> = emptyList(),
    ): List<ScanLineItem> {
        val existing = items.find { it.type == type && it.sph == sph && it.cyl == cyl && it.axis == axis && it.add == add }
        return if (existing != null) {
            items.map {
                if (it.id == existing.id) {
                    it.copy(quantity = it.quantity + quantity, boxNumbers = (it.boxNumbers + boxNumbers).distinct())
                } else it
            }
        } else {
            items + ScanLineItem(type = type, sph = sph, cyl = cyl, quantity = quantity, axis = axis, add = add, boxNumbers = boxNumbers)
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
                        boxNumbers = item.boxNumbers.takeIf { it.isNotEmpty() },
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

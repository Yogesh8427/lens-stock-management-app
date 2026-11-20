package com.lensstock.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensstock.app.data.model.StockHistoryEntry
import com.lensstock.app.data.repository.LensRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One date's worth of additions/removals, newest date first. */
data class HistoryGroup(val label: String, val entries: List<StockHistoryEntry>)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val groups: List<HistoryGroup> = emptyList(),
)

class HistoryViewModel : ViewModel() {
    private val repository = LensRepository()
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // A handful of entries predate the lens's power being recorded directly on the
                // history entry — they'd only show up as a blank row, so they're left out rather
                // than shown as noise. Every entry shown here is a real, committed stock change.
                val entries = repository.recentHistory(limit = 300).filter { it.sph.isNotBlank() }
                _uiState.update { it.copy(groups = groupByDay(entries), isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load activity") }
            }
        }
    }

    /**
     * Entries arrive newest-first from Firestore (see [LensRepository.recentHistory]'s ordering),
     * so grouping by day key preserves that order without needing to re-sort afterwards. An entry
     * still waiting on the server clock (offline write, [StockHistoryEntry.createdAt] still null)
     * is treated as happening right now, i.e. today.
     */
    private fun groupByDay(entries: List<StockHistoryEntry>): List<HistoryGroup> {
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dayLabelFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val today = dayKeyFormat.format(Date())
        val yesterday = dayKeyFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)

        return entries
            .groupBy { dayKeyFormat.format(it.createdAt ?: Date()) }
            .map { (key, group) ->
                val label = when (key) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> dayLabelFormat.format(group.first().createdAt ?: Date())
                }
                HistoryGroup(label, group)
            }
    }
}

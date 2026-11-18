package com.lensstock.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

object ChangeReason {
    const val SCAN_IN = "SCAN_IN"
    const val SCAN_OUT = "SCAN_OUT"
    const val MANUAL = "MANUAL"
    const val DAMAGED = "DAMAGED"
    const val UNDO = "UNDO"
}

/** Mirrors a `stock_history` document. */
data class StockHistoryEntry(
    @DocumentId val id: String = "",
    val lensId: String = "",
    val change: Long = 0,
    val reason: String = "",
    val byUser: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

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
    /** A lens was deleted ("Stop tracking") from Full Stock while it still had stock. */
    const val DELETED = "DELETED"
}

/**
 * Mirrors a `stock_history` document. The lens's type/power are denormalized onto the entry
 * itself (not just looked up via [lensId]) so the Activity log still reads correctly after the
 * lens doc it refers to has since been edited to a different power, or deleted entirely.
 */
data class StockHistoryEntry(
    @DocumentId val id: String = "",
    val lensId: String = "",
    val change: Long = 0,
    val reason: String = "",
    val byUser: String = "",
    val type: String = "",
    val sph: String = "",
    val cyl: String = "",
    val axis: Long = 0,
    val add: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

package com.lensstock.app.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.lensstock.app.data.model.ChangeReason
import com.lensstock.app.data.model.Lens
import kotlinx.coroutines.tasks.await

class LensRepository {
    private val db = Firebase.firestore
    private val lensStock = db.collection("lens_stock")
    private val stockHistory = db.collection("stock_history")
    private val lensTypes = db.collection("lens_types")

    /** Custom lens types a shop has added, beyond the built-in Normal/Blue Cut. */
    suspend fun listCustomTypes(): List<String> =
        lensTypes.get().await().documents.mapNotNull { it.getString("name") }

    fun addCustomType(name: String) {
        lensTypes.document(name).set(mapOf("name" to name), SetOptions.merge())
    }

    suspend fun findLens(type: String, sph: String, cyl: String, axis: Long = 0, add: String = ""): Lens? {
        val id = Lens.documentId(type, sph, cyl, axis, add)
        val snapshot = lensStock.document(id).get().await()
        return if (snapshot.exists()) snapshot.toObject(Lens::class.java) else null
    }

    suspend fun lowStock(): List<Lens> {
        val all = lensStock.orderBy("type").get().await().toObjects(Lens::class.java)
        return all.filter { it.quantity <= it.minQuantity }
    }

    suspend fun recentHistory(limit: Long = 50) =
        stockHistory.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(com.lensstock.app.data.model.StockHistoryEntry::class.java)

    /**
     * Applies [delta] to a lens's quantity and records the change. Uses FieldValue.increment
     * so two phones changing the same lens at the same time never clobber each other —
     * never read-modify-write the quantity by hand.
     *
     * Deliberately NOT suspend/awaited: Firestore applies a set()/add() to the local cache (and
     * to any read-your-writes read, via latency compensation) the instant this is called, but the
     * returned Task only completes once the backend acknowledges it — which can take indefinitely
     * long while the shop has no internet. Awaiting it here would hang Scan In/Out offline, which
     * defeats the whole point of the app. The SDK queues and syncs the write on its own.
     */
    fun changeStock(
        type: String,
        sph: String,
        cyl: String,
        delta: Long,
        reason: String,
        boxNumber: String? = null,
        axis: Long = 0,
        add: String = "",
    ) {
        val id = Lens.documentId(type, sph, cyl, axis, add)
        val ref = lensStock.document(id)

        val fields = mutableMapOf<String, Any>(
            "type" to type,
            "sph" to sph,
            "cyl" to cyl,
            "axis" to axis,
            "add" to add,
            "quantity" to FieldValue.increment(delta),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (boxNumber != null) fields["boxNumber"] = boxNumber

        ref.set(fields, SetOptions.merge())

        stockHistory.add(
            mapOf(
                "lensId" to id,
                "change" to delta,
                "reason" to reason,
                "byUser" to (Firebase.auth.currentUser?.email ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    fun setBoxNumber(type: String, sph: String, cyl: String, boxNumber: String, axis: Long = 0, add: String = "") {
        val id = Lens.documentId(type, sph, cyl, axis, add)
        lensStock.document(id).set(mapOf("boxNumber" to boxNumber), SetOptions.merge())
    }

    fun manualAdjust(type: String, sph: String, cyl: String, delta: Long, axis: Long = 0, add: String = "") {
        changeStock(type, sph, cyl, delta, ChangeReason.MANUAL, axis = axis, add = add)
    }
}

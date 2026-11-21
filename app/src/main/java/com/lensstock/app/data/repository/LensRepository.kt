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

    /**
     * Removes a shop-defined type — but only when nothing under it still has stock, so inventory
     * is never silently lost. Any already-zeroed lens docs of that type are cleaned up too, since
     * they'd otherwise become orphaned (no type chip left to find them under).
     */
    suspend fun removeCustomType(name: String): Boolean {
        val stockForType = lensStock.whereEqualTo("type", name).get().await().toObjects(Lens::class.java)
        if (stockForType.any { it.quantity > 0 }) return false
        stockForType.forEach { lensStock.document(it.id).delete() }
        lensTypes.document(name).delete()
        return true
    }

    /**
     * [axis] null means "unspecified" — e.g. the search screen's AXIS field was left blank — and
     * looks the lens up regardless of axis, preferring a plain (axis 0) match when one exists, so
     * a toric lens can still be found by someone who doesn't know its exact axis.
     */
    suspend fun findLens(type: String, sph: String, cyl: String, axis: Long? = 0, add: String = ""): Lens? {
        if (axis != null) {
            val id = Lens.documentId(type, sph, cyl, axis, add)
            val snapshot = lensStock.document(id).get().await()
            return if (snapshot.exists()) snapshot.toObject(Lens::class.java) else null
        }
        val matches = lensStock
            .whereEqualTo("type", type)
            .whereEqualTo("sph", sph)
            .whereEqualTo("cyl", cyl)
            .whereEqualTo("add", add)
            .get().await().toObjects(Lens::class.java)
        return matches.firstOrNull { it.axis == 0L } ?: matches.firstOrNull()
    }

    suspend fun lowStock(): List<Lens> {
        val all = lensStock.orderBy("type").get().await().toObjects(Lens::class.java)
        return all.filter { it.quantity <= it.minQuantity }
    }

    /** Every lens currently in stock, for the Full Stock screen. */
    suspend fun allStock(): List<Lens> =
        lensStock.orderBy("type").get().await().toObjects(Lens::class.java)

    /**
     * Stops tracking a lens entirely — e.g. a discontinued power — regardless of its quantity.
     * Whatever stock it still had is logged as a removal first, so deleting it from Full Stock
     * doesn't silently drop it from the Activity log too.
     */
    fun deleteLens(lens: Lens) {
        if (lens.quantity != 0L) {
            changeStock(lens.type, lens.sph, lens.cyl, -lens.quantity, ChangeReason.DELETED, axis = lens.axis, add = lens.add)
        }
        lensStock.document(lens.id).delete()
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
        boxNumbers: List<String>? = null,
        axis: Long = 0,
        add: String = "",
        minQuantity: Long? = null,
        maxQuantity: Long? = null,
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
        // arrayUnion appends server-side without needing to know the doc's current box list —
        // same reasoning as increment() above: this call never reads before it writes, so stock
        // arriving in a new box doesn't clobber boxes already recorded for this lens.
        if (!boxNumbers.isNullOrEmpty()) fields["boxNumbers"] = FieldValue.arrayUnion(*boxNumbers.toTypedArray())
        // Only passed by the Scan In/Out flow (whose dialog always shows these, pre-filled from
        // any existing stock row) — every other caller (deletes, plain manual adjustments) leaves
        // these null so it never clobbers thresholds set elsewhere.
        if (minQuantity != null) fields["minQuantity"] = minQuantity
        if (maxQuantity != null) fields["maxQuantity"] = maxQuantity

        ref.set(fields, SetOptions.merge())

        stockHistory.add(
            mapOf(
                "lensId" to id,
                "change" to delta,
                "reason" to reason,
                "byUser" to (Firebase.auth.currentUser?.email ?: ""),
                "type" to type,
                "sph" to sph,
                "cyl" to cyl,
                "axis" to axis,
                "add" to add,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    fun manualAdjust(type: String, sph: String, cyl: String, delta: Long, axis: Long = 0, add: String = "") {
        changeStock(type, sph, cyl, delta, ChangeReason.MANUAL, axis = axis, add = add)
    }

    /**
     * Full edit from the Full Stock screen: type/power/axis/add, quantity, boxes and thresholds
     * can all change at once. When the power/type/axis/add changes, [old]'s stock moves to the
     * (possibly already-existing) doc for the new identity rather than silently overwriting it.
     */
    fun editLens(
        old: Lens,
        type: String,
        sph: String,
        cyl: String,
        axis: Long,
        add: String,
        quantity: Long,
        boxNumbers: List<String>,
        minQuantity: Long,
        maxQuantity: Long,
    ) {
        val newId = Lens.documentId(type, sph, cyl, axis, add)
        if (newId == old.id) {
            val delta = quantity - old.quantity
            if (delta != 0L) changeStock(type, sph, cyl, delta, ChangeReason.MANUAL, axis = axis, add = add)
            lensStock.document(old.id).set(
                mapOf(
                    "boxNumbers" to boxNumbers,
                    "minQuantity" to minQuantity,
                    "maxQuantity" to maxQuantity,
                ),
                SetOptions.merge(),
            )
        } else {
            if (old.quantity != 0L) {
                changeStock(old.type, old.sph, old.cyl, -old.quantity, ChangeReason.MANUAL, axis = old.axis, add = old.add)
            }
            lensStock.document(old.id).delete()
            if (quantity != 0L) {
                changeStock(type, sph, cyl, quantity, ChangeReason.MANUAL, boxNumbers = boxNumbers, axis = axis, add = add)
            }
            lensStock.document(newId).set(
                mapOf("minQuantity" to minQuantity, "maxQuantity" to maxQuantity),
                SetOptions.merge(),
            )
        }
    }
}

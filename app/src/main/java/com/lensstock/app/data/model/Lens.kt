package com.lensstock.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors a `lens_stock` document. Doc ID: "{type}_{sph}_{cyl}", plus "_ax{axis}" and/or
 * "_add{add}" suffixes when those are set — so a plain single-vision lens keeps its original,
 * stable ID while a toric or bifocal variant of the same SPH/CYL gets a distinct stock entry.
 */
data class Lens(
    @DocumentId val id: String = "",
    val type: String = "",
    val sph: String = "",
    val cyl: String = "",
    val axis: Long = 0,
    val add: String = "",
    val quantity: Long = 0,
    val boxNumbers: List<String> = emptyList(),
    val minQuantity: Long = 2,
    val maxQuantity: Long = 10,
    @ServerTimestamp val updatedAt: Date? = null,
) {
    companion object {
        fun documentId(type: String, sph: String, cyl: String, axis: Long = 0, add: String = ""): String {
            val axisPart = if (axis != 0L) "_ax$axis" else ""
            val addPart = if (add.isNotBlank()) "_add$add" else ""
            return "${type}_${sph}_${cyl}$axisPart$addPart"
        }
    }
}

/**
 * Lens types are an open set: "NORMAL" and "BLUE_CUT" always exist (and OCR can detect them from
 * a packet), but a shop can also define its own custom types at the "Add manually" dialog — those
 * are stored in the `lens_types` collection and merged in alongside the two built-ins everywhere
 * a type picker is shown.
 */
object LensTypes {
    const val NORMAL = "NORMAL"
    const val BLUE_CUT = "BLUE_CUT"
    val builtIns = listOf(NORMAL, BLUE_CUT)

    /** "NORMAL" -> "Normal", "BLUE_CUT" -> "Blue Cut", a custom type is shown exactly as entered. */
    fun label(type: String): String = when (type) {
        NORMAL -> "Normal"
        BLUE_CUT -> "Blue Cut"
        else -> type
    }
}

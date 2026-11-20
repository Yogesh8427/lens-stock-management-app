package com.lensstock.app.ui.screens.scan

import java.util.UUID

/** One row in the confirmation list — a lens type/power plus how many packets were scanned. */
data class ScanLineItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val sph: String,
    val cyl: String,
    val quantity: Long,
    val axis: Long = 0,
    val add: String = "",
    val boxNumbers: List<String> = emptyList(),
)

/** " X90 ADD+2.50" — blank when neither is set, for compact display next to SPH/CYL. */
fun ScanLineItem.axisAddSuffix(): String {
    val axisPart = if (axis != 0L) " X$axis" else ""
    val addPart = if (add.isNotBlank()) " ADD$add" else ""
    return axisPart + addPart
}

/** "Box 12, 14", or null when no box is attached yet. */
fun ScanLineItem.boxLabel(): String? = boxNumbers.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
    ?.joinToString(", ")?.let { "Box $it" }

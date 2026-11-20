package com.lensstock.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lensstock.app.data.model.Lens

/** Shared by any screen that lists lens stock (Full Stock, To Order, ...) so the look stays consistent. */
enum class StockStatus { LOW, CAUTION, FULL }

fun Lens.stockStatus(): StockStatus = when {
    quantity < minQuantity -> StockStatus.LOW
    quantity >= maxQuantity -> StockStatus.FULL
    else -> StockStatus.CAUTION
}

val StatusLowColor = Color(0xFFD32F2F)
val StatusCautionColor = Color(0xFFF9A825)
val StatusFullColor = Color(0xFF2E7D32)

fun StockStatus.color(): Color = when (this) {
    StockStatus.LOW -> StatusLowColor
    StockStatus.CAUTION -> StatusCautionColor
    StockStatus.FULL -> StatusFullColor
}

fun StockStatus.icon(): ImageVector = when (this) {
    StockStatus.LOW -> Icons.Filled.Error
    StockStatus.CAUTION -> Icons.Filled.Warning
    StockStatus.FULL -> Icons.Filled.CheckCircle
}

fun Lens.axisAddSuffix(): String {
    val axisPart = if (axis != 0L) " X$axis" else ""
    val addPart = if (add.isNotBlank()) " ADD$add" else ""
    return axisPart + addPart
}

/**
 * "-1.25/-0.50" when both powers are set; just "-1.25 SPH" or "-0.75 CYL" (labelled, so a lone
 * number is never mistaken for the other power) when only one of them is non-zero.
 */
fun Lens.powerLabel(): String = when {
    cyl == "0.00" -> "$sph SPH"
    sph == "0.00" -> "$cyl CYL"
    else -> "$sph/$cyl"
}

/** "Box 12, 14, 19", or null when no box is assigned — callers should omit the line entirely then. */
fun Lens.boxLabel(): String? = boxNumbers.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
    ?.joinToString(", ")?.let { "Box $it" }

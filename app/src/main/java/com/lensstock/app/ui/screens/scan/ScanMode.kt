package com.lensstock.app.ui.screens.scan

import com.lensstock.app.data.model.ChangeReason

/** Add Lens and Remove Lens share one flow — this is the only thing that differs between them. */
enum class ScanMode(val title: String, val reason: String, val sign: Long) {
    IN("Add Lens", ChangeReason.SCAN_IN, 1L),
    OUT("Remove Lens", ChangeReason.SCAN_OUT, -1L),
}

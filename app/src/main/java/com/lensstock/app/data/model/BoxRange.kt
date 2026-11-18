package com.lensstock.app.data.model

import com.google.firebase.firestore.DocumentId

/** Mirrors a `boxes` document — used to auto-suggest a box code for a power (Phase 3). */
data class BoxRange(
    @DocumentId val id: String = "",
    val code: String = "",
    val type: String = "",
    val sphFrom: String = "",
    val sphTo: String = "",
)

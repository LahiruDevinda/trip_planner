package com.devinda.tripplanner.data.model

import com.google.firebase.firestore.DocumentId

data class Trip (
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val destination: String = "",
    val title: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val budget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
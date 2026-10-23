package com.devinda.tripplanner.data.model

import com.google.firebase.firestore.DocumentId

data class TripLocation(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Trip(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val estimatedBudget: Double = 0.0,
    val date: String = "",
    val transportType: String = "Car",
    val locations: List<TripLocation> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
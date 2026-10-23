package com.devinda.tripplanner.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
data class TripLocation(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
data class Trip(
    @DocumentId
    var id: String = "",
    val userId: String = "",
    val title: String = "",
    val estimatedBudget: Double = 0.0,
    val date: String = "",
    val transportType: String = "Car",
    @get:PropertyName("locations")
    @set:PropertyName("locations")
    var locations: List<TripLocation> = emptyList()
) {
    constructor() : this("", "", "", 0.0, "", "Car", emptyList())

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, data: Map<String, Any?>?): Trip {
            if (data == null) return Trip(id = id)

            val rawLocations = data["locations"] as? List<*> ?: emptyList<Any>()
            val parsedLocations = rawLocations.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        val name = item["name"] as? String ?: ""
                        val lat = (item["latitude"] as? Number)?.toDouble() ?: 0.0
                        val lon = (item["longitude"] as? Number)?.toDouble() ?: 0.0
                        TripLocation(name, lat, lon)
                    }
                    is TripLocation -> item
                    else -> null
                }
            }

            return Trip(
                id = id,
                userId = data["userId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                estimatedBudget = (data["estimatedBudget"] as? Number)?.toDouble() ?: 0.0,
                date = data["date"] as? String ?: "",
                transportType = data["transportType"] as? String ?: "Car",
                locations = parsedLocations
            )
        }
    }
}
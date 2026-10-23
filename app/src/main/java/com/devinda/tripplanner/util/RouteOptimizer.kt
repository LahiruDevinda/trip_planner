package com.devinda.tripplanner.util

import android.location.Location
import com.devinda.tripplanner.data.model.TripLocation
import com.google.android.gms.maps.model.LatLng

object RouteOptimizer {

    fun optimizeRoute(startLatLng: LatLng, locations: List<TripLocation>): List<TripLocation> {
        if (locations.size <= 1) return locations

        val remaining = locations.toMutableList()
        val optimized = mutableListOf<TripLocation>()
        var currentLat = startLatLng.latitude
        var currentLng = startLatLng.longitude

        while (remaining.isNotEmpty()) {
            val nearest = remaining.minByOrNull { loc ->
                distanceBetween(currentLat, currentLng, loc.latitude, loc.longitude)
            } ?: break

            optimized.add(nearest)
            currentLat = nearest.latitude
            currentLng = nearest.longitude
            remaining.remove(nearest)
        }

        return optimized
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}
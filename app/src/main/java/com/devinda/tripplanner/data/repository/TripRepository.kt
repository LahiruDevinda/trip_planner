package com.devinda.tripplanner.data.repository

import com.devinda.tripplanner.data.model.Trip
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val firestore = Firebase.firestore
    private val tripsCollection = firestore.collection("trips")

    suspend fun addTrip(trip: Trip): Result<Unit> {
        return try {
            tripsCollection.add(trip).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTripsForUser(userId: String): Flow<List<Trip>> = callbackFlow {
        val listener = tripsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val trips = snapshot?.toObjects(Trip::class.java) ?: emptyList()
                trySend(trips)
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripsCollection.document(tripId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.devinda.tripplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devinda.tripplanner.data.model.Trip
import com.devinda.tripplanner.data.repository.TripRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TripUiState {
    object Loading : TripUiState()
    data class Success(val trips: List<Trip>) : TripUiState()
    data class Error(val message: String) : TripUiState()
}

class TripViewModel(
    private val repository: TripRepository = TripRepository()
) : ViewModel() {

    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Loading)
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    init {
        loadUserTrips()
    }

    fun loadUserTrips() {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getTripsForUser(currentUserId).collect { trips ->
                _uiState.value = TripUiState.Success(trips)
            }
        }
    }

    fun createTrip(
        destination: String,
        title: String,
        startDate: String,
        endDate: String,
        budget: Double,
        onSuccess: () -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val newTrip = Trip(
            userId = currentUserId,
            destination = destination.trim(),
            title = title.trim(),
            startDate = startDate.trim(),
            endDate = endDate.trim(),
            budget = budget
        )

        viewModelScope.launch {
            val result = repository.addTrip(newTrip)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}
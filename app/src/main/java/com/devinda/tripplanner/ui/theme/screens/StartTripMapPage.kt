package com.devinda.tripplanner.ui.theme.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.devinda.tripplanner.BuildConfig
import com.devinda.tripplanner.R
import com.devinda.tripplanner.data.model.Trip
import com.devinda.tripplanner.util.DirectionsHelper
import com.devinda.tripplanner.util.RouteOptimizer
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTripMapScreen(
    tripId: String,
    navController: NavController
) {
    val context = LocalContext.current
    var trip by remember { mutableStateOf<Trip?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var roadPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isRouteLoading by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchDeviceLocation() {
        // Try last location first
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                userLocation = LatLng(loc.latitude, loc.longitude)
            } else {
                // If lastLocation is null (very common on emulators), request a fresh current location
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { freshLoc ->
                        if (freshLoc != null) {
                            userLocation = LatLng(freshLoc.latitude, freshLoc.longitude)
                        } else {
                            // Fallback to Colombo if emulator provides no coordinates
                            userLocation = LatLng(6.9271, 79.8612)
                        }
                    }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchDeviceLocation()
        } else {
            // Default position if user denies permission
            userLocation = LatLng(6.9271, 79.8612)
        }
    }

    // 1. Fetch Trip details and Location permissions
    LaunchedEffect(tripId) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            fetchDeviceLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        Firebase.firestore.collection("trips").document(tripId).get()
            .addOnSuccessListener { snapshot ->
                trip = snapshot.toObject(Trip::class.java)
            }
    }

    // 2. Fetch Directions as soon as Trip and User Location are both ready
    LaunchedEffect(trip, userLocation) {
        val currentTrip = trip
        val currentLoc = userLocation
        if (currentTrip != null && currentLoc != null && currentTrip.locations.isNotEmpty()) {
            isRouteLoading = true

            // Optimize stops order
            val optimized = RouteOptimizer.optimizeRoute(currentLoc, currentTrip.locations)

            // Request real road polyline
            val points = DirectionsHelper.fetchRoadPolyline(
                context = context,
                origin = currentLoc,
                stops = optimized,
                transportType = currentTrip.transportType,
                apiKey = BuildConfig.API_KEY
            )

            roadPoints = points
            isRouteLoading = false
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.9271, 79.8612), 9f)
    }

    // Center camera on user location when acquired
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 10f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.title ?: "Trip Route") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = userLocation != null)
            ) {
                // Show markers for all stops
                trip?.locations?.forEachIndexed { index, stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = "${index + 1}. ${stop.name}",
                        snippet = "Stop on route"
                    )
                }

                // Render polyline along actual roads
                if (roadPoints.isNotEmpty()) {
                    Polyline(
                        points = roadPoints,
                        color = colorResource(id = R.color.button_container),
                        width = 12f
                    )
                }
            }

            if (isRouteLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}
package com.devinda.tripplanner.ui.theme.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.devinda.tripplanner.data.model.Trip
import com.devinda.tripplanner.data.model.TripLocation
import com.devinda.tripplanner.util.RouteOptimizer
import com.google.android.gms.location.LocationServices
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
    var optimizedStops by remember { mutableStateOf<List<TripLocation>>(emptyList()) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude)
            }
        }
    }

    // 1. Fetch Trip details & location permission
    LaunchedEffect(tripId) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude)
            }
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

    // 2. Compute the shortest route when user location and trip data are ready
    LaunchedEffect(userLocation, trip) {
        val currentTrip = trip
        val currentLoc = userLocation
        if (currentTrip != null && currentLoc != null) {
            optimizedStops = RouteOptimizer.optimizeRoute(currentLoc, currentTrip.locations)
        } else if (currentTrip != null) {
            optimizedStops = currentTrip.locations
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        val defaultCenter = userLocation ?: LatLng(6.9271, 79.8612)
        position = CameraPosition.fromLatLngZoom(defaultCenter, 12f)
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
            val polylinePoints = remember(userLocation, optimizedStops) {
                val list = mutableListOf<LatLng>()
                userLocation?.let { list.add(it) }
                list.addAll(optimizedStops.map { LatLng(it.latitude, it.longitude) })
                list
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = userLocation != null)
            ) {
                // Numbered markers for optimized stops
                optimizedStops.forEachIndexed { index, stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = "Stop ${index + 1}: ${stop.name}",
                        snippet = "Optimized Destination"
                    )
                }

                // Render Route Line connecting optimized stops
                if (polylinePoints.size >= 2) {
                    Polyline(
                        points = polylinePoints,
                        color = Color(0xFFFF9800),
                        width = 12f
                    )
                }
            }

            // Summary overlay panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Optimized Route Navigation", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${optimizedStops.size} stops reordered to minimize travel distance via ${trip?.transportType ?: "vehicle"}.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
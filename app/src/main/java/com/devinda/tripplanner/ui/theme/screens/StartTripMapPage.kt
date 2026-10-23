package com.devinda.tripplanner.ui.theme.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.devinda.tripplanner.BuildConfig
import com.devinda.tripplanner.R
import com.devinda.tripplanner.data.model.Trip
import com.devinda.tripplanner.data.model.TripLocation
import com.devinda.tripplanner.util.DirectionsHelper
import com.devinda.tripplanner.util.RouteOptimizer
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTripMapScreen(
    tripId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var trip by remember { mutableStateOf<Trip?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var remainingStops by remember { mutableStateOf<List<TripLocation>>(emptyList()) }
    var roadPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isRerouting by remember { mutableStateOf(false) }
    var hasInitialRoute by remember { mutableStateOf(false) }
    var currentSpeedKmh by remember { mutableIntStateOf(0) }
    var isFollowingUser by remember { mutableStateOf(true) }

    // Observable permission state ensures the tracking listener activates immediately when granted
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.9271, 79.8612), 16f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            hasLocationPermission = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun requestRouteUpdate(origin: LatLng, stops: List<TripLocation>, transportType: String) {
        if (isRerouting || stops.isEmpty()) return
        coroutineScope.launch {
            isRerouting = true
            val points = withContext(Dispatchers.IO) {
                DirectionsHelper.fetchRoadPolyline(
                    context = context,
                    origin = origin,
                    stops = stops,
                    transportType = transportType,
                    apiKey = BuildConfig.API_KEY
                )
            }
            if (points.isNotEmpty()) {
                roadPoints = points
                hasInitialRoute = true
            }
            isRerouting = false
        }
    }

    fun trimPolylineAhead(currentLoc: LatLng, points: List<LatLng>): List<LatLng> {
        if (points.size < 2) return points
        var closestIdx = 0
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        for (i in 0 until minOf(points.size, 20)) {
            val pt = points[i]
            Location.distanceBetween(currentLoc.latitude, currentLoc.longitude, pt.latitude, pt.longitude, results)
            if (results[0] < minDistance) {
                minDistance = results[0]
                closestIdx = i
            }
        }

        return if (minDistance < 60f) {
            listOf(currentLoc) + points.drop(closestIdx + 1)
        } else {
            listOf(currentLoc) + points
        }
    }

    // Detect manual user touch/pan
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving to cameraPositionState.cameraMoveStartedReason }
            .collect { (isMoving, reason) ->
                if (isMoving && reason == CameraMoveStartedReason.GESTURE) {
                    isFollowingUser = false
                }
            }
    }

    // Active location listener tied directly to hasLocationPermission
    DisposableEffect(hasLocationPermission, trip) {
        if (!hasLocationPermission) return@DisposableEffect onDispose { }

        // Seed with immediate current location so we don't wait on intervals
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val pt = LatLng(loc.latitude, loc.longitude)
                    userLocation = pt
                    Log.d("LOCATION_DEBUG", "Seeded current location: $pt")
                }
            }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // 1-second update
        ).apply {
            setMinUpdateDistanceMeters(0f) // Receive updates even on minute movements or emulator single clicks
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(false)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val currentPoint = LatLng(loc.latitude, loc.longitude)
                userLocation = currentPoint
                currentSpeedKmh = ((loc.speed * 3.6f).toInt()).coerceAtLeast(0)
                Log.d("LOCATION_DEBUG", "Live location update: $currentPoint, speed: $currentSpeedKmh")

                if (isFollowingUser) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(currentPoint)
                                    .zoom(cameraPositionState.position.zoom.coerceAtLeast(16f))
                                    .bearing(if (loc.hasBearing()) loc.bearing else cameraPositionState.position.bearing)
                                    .tilt(if (loc.hasBearing()) 45f else 0f)
                                    .build()
                            ),
                            600
                        )
                    }
                }

                val activeTrip = trip ?: return
                if (remainingStops.isEmpty()) return

                // Check waypoint completion
                val nextStop = remainingStops.first()
                val distToStop = FloatArray(1)
                Location.distanceBetween(
                    currentPoint.latitude, currentPoint.longitude,
                    nextStop.latitude, nextStop.longitude,
                    distToStop
                )

                if (distToStop[0] < 35f) {
                    val updatedRemaining = remainingStops.drop(1)
                    remainingStops = updatedRemaining
                    if (updatedRemaining.isNotEmpty()) {
                        requestRouteUpdate(currentPoint, updatedRemaining, activeTrip.transportType)
                    } else {
                        roadPoints = emptyList()
                    }
                    return
                }

                if (roadPoints.isNotEmpty()) {
                    val isOnRoad = PolyUtil.isLocationOnPath(currentPoint, roadPoints, true, 60.0)
                    if (isOnRoad) {
                        roadPoints = trimPolylineAhead(currentPoint, roadPoints)
                    } else {
                        requestRouteUpdate(currentPoint, remainingStops, activeTrip.transportType)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Load Trip from Firestore
    LaunchedEffect(tripId) {
        Firebase.firestore.collection("trips").document(tripId).get()
            .addOnSuccessListener { snapshot ->
                val loadedTrip = snapshot.toObject(Trip::class.java)
                trip = loadedTrip
                if (loadedTrip != null) {
                    remainingStops = loadedTrip.locations
                }
            }
    }

    // Initial Route Generation
    LaunchedEffect(trip, userLocation) {
        val activeTrip = trip
        val origin = userLocation
        if (!hasInitialRoute && activeTrip != null && origin != null && remainingStops.isNotEmpty()) {
            val optimized = RouteOptimizer.optimizeRoute(origin, remainingStops)
            remainingStops = optimized
            requestRouteUpdate(origin, optimized, activeTrip.transportType)
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(origin, 16.5f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.title ?: "Navigation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isFollowingUser,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        isFollowingUser = true
                        userLocation?.let { loc ->
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(loc)
                                            .zoom(16.5f)
                                            .tilt(45f)
                                            .build()
                                    ),
                                    600
                                )
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    text = { Text("Recenter") },
                    containerColor = colorResource(id = R.color.button_container),
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = userLocation != null && hasLocationPermission),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    compassEnabled = true,
                    zoomControlsEnabled = false
                )
            ) {
                remainingStops.forEachIndexed { index, stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = "${index + 1}. ${stop.name}",
                        snippet = if (index == 0) "Next destination" else "Upcoming stop"
                    )
                }

                if (roadPoints.isNotEmpty()) {
                    Polyline(
                        points = roadPoints,
                        color = colorResource(id = R.color.button_container),
                        width = 14f
                    )
                }
            }

            if (isRerouting && !hasInitialRoute) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = colorResource(id = R.color.button_container),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (remainingStops.isNotEmpty()) "Next: ${remainingStops.first().name}" else "Arrived at destination!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isRerouting) "Rerouting..." else "Remaining stops: ${remainingStops.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentSpeedKmh > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$currentSpeedKmh km/h",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.devinda.tripplanner.ui.theme.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.devinda.tripplanner.BuildConfig
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun createNavigationChevronBitmap(): BitmapDescriptor? {
    return try {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        // 1. Soft drop shadow halo
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(60, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, size * 0.46f, shadowPaint)

        // 2. White outer rim
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, size * 0.40f, rimPaint)

        // 3. Core Google Blue disc
        val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(26, 115, 232)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, size * 0.34f, bluePaint)

        // 4. Direction arrow pointing straight forward (0°)
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val arrowPath = Path().apply {
            moveTo(center, size * 0.22f)                       // Forward tip
            lineTo(size * 0.65f, size * 0.66f)                // Bottom right wing
            lineTo(center, size * 0.54f)                       // Inner rear notch
            lineTo(size * 0.35f, size * 0.66f)                // Bottom left wing
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        Log.e("NAV_ICON", "Failed generating chevron bitmap: ${e.message}")
        null
    }
}

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
    var userBearing by remember { mutableFloatStateOf(0f) }
    var remainingStops by remember { mutableStateOf<List<TripLocation>>(emptyList()) }
    var roadPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isRerouting by remember { mutableStateOf(false) }
    var hasInitialRoute by remember { mutableStateOf(false) }
    var currentSpeedKmh by remember { mutableIntStateOf(0) }
    var isFollowingUser by remember { mutableStateOf(true) }

    // Direct solid color avoids XML dynamic resource crashes
    val routeColor = Color(0xFF1E88E5)

    // Holder for custom navigation icon, populated safely in onMapLoaded
    var navigationChevronIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Use applicationContext to prevent broker package binding mismatches
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.9271, 79.8612), 14f)
    }

    fun requestRouteUpdate(origin: LatLng, stops: List<TripLocation>, transportType: String) {
        val validStops = stops.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (validStops.isEmpty()) return

        coroutineScope.launch {
            isRerouting = true
            val points = withContext(Dispatchers.IO) {
                try {
                    DirectionsHelper.fetchRoadPolyline(
                        context = context,
                        origin = origin,
                        stops = validStops,
                        transportType = transportType,
                        apiKey = BuildConfig.API_KEY
                    )
                } catch (e: Exception) {
                    Log.e("NAV_DEBUG", "Routing API error: ${e.message}")
                    emptyList()
                }
            }

            roadPoints = if (points.isNotEmpty()) {
                points
            } else {
                listOf(origin) + validStops.map { LatLng(it.latitude, it.longitude) }
            }

            hasInitialRoute = true
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

    // Release camera lock when user drags or zooms the map
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving to cameraPositionState.cameraMoveStartedReason }
            .collect { (isMoving, reason) ->
                if (isMoving && reason == CameraMoveStartedReason.GESTURE) {
                    isFollowingUser = false
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) hasLocationPermission = true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Continuous location tracking loop
    DisposableEffect(hasLocationPermission, trip) {
        if (!hasLocationPermission) return@DisposableEffect onDispose { }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    userLocation = LatLng(loc.latitude, loc.longitude)
                    if (loc.hasBearing()) userBearing = loc.bearing
                }
            }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1200L
        ).apply {
            setMinUpdateDistanceMeters(0f)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(false)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val currentPoint = LatLng(loc.latitude, loc.longitude)
                userLocation = currentPoint

                if (loc.hasBearing() && loc.speed > 0.4f) {
                    userBearing = loc.bearing
                }

                currentSpeedKmh = ((loc.speed * 3.6f).toInt()).coerceAtLeast(0)

                if (isFollowingUser) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(currentPoint)
                                    .zoom(cameraPositionState.position.zoom.coerceAtLeast(16f))
                                    .bearing(if (loc.hasBearing()) loc.bearing else cameraPositionState.position.bearing)
                                    .tilt(if (loc.hasBearing()) 40f else 0f)
                                    .build()
                            ),
                            600
                        )
                    }
                }

                val activeTrip = trip ?: return
                if (remainingStops.isEmpty()) return

                val nextStop = remainingStops.firstOrNull() ?: return
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
                        val reoptimized = RouteOptimizer.optimizeRoute(currentPoint, updatedRemaining)
                        remainingStops = reoptimized
                        requestRouteUpdate(currentPoint, reoptimized, activeTrip.transportType)
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
                        val reoptimized = RouteOptimizer.optimizeRoute(currentPoint, remainingStops)
                        remainingStops = reoptimized
                        requestRouteUpdate(currentPoint, reoptimized, activeTrip.transportType)
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

    // Real-time Firestore sync: updates instantly when destinations are edited in TripDetailsPage
    DisposableEffect(tripId) {
        if (tripId.isBlank()) return@DisposableEffect onDispose {}

        val registration = Firebase.firestore.collection("trips").document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val loadedTrip = Trip.fromMap(snapshot.id, snapshot.data)
                    trip = loadedTrip

                    val origin = userLocation ?: loadedTrip.locations.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
                    if (origin != null && loadedTrip.locations.isNotEmpty()) {
                        val optimizedStops = RouteOptimizer.optimizeRoute(origin, loadedTrip.locations)
                        remainingStops = optimizedStops
                        requestRouteUpdate(origin, optimizedStops, loadedTrip.transportType)
                    } else {
                        remainingStops = loadedTrip.locations
                    }
                }
            }

        onDispose { registration.remove() }
    }

    // Initial Route Generation bootstrap
    LaunchedEffect(trip, userLocation) {
        val activeTrip = trip ?: return@LaunchedEffect
        if (hasInitialRoute || activeTrip.locations.isEmpty()) return@LaunchedEffect

        val origin = userLocation
        if (origin != null) {
            val optimized = RouteOptimizer.optimizeRoute(origin, activeTrip.locations)
            remainingStops = optimized
            requestRouteUpdate(origin, optimized, activeTrip.transportType)
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(origin, 15.5f))
        } else {
            delay(1200)
            if (!hasInitialRoute && userLocation == null && activeTrip.locations.isNotEmpty()) {
                val first = activeTrip.locations.first()
                val start = LatLng(first.latitude, first.longitude)
                val optimized = RouteOptimizer.optimizeRoute(start, activeTrip.locations)
                remainingStops = optimized
                requestRouteUpdate(start, optimized, activeTrip.transportType)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(start, 14f))
            }
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
                        val target = userLocation ?: remainingStops.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
                        target?.let { loc ->
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(loc)
                                            .zoom(16f)
                                            .tilt(40f)
                                            .build()
                                    ),
                                    600
                                )
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    text = { Text("Recenter") },
                    containerColor = routeColor,
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Keeps false to prevent the Maps SDK from calling the crashed internal getColor()
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    compassEnabled = true,
                    zoomControlsEnabled = false
                ),
                onMapLoaded = {
                    // Safe to generate BitmapDescriptor here: Maps internal binder is fully ready
                    navigationChevronIcon = createNavigationChevronBitmap()
                        ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                }
            ) {
                // Live Vehicle Beacon (Rotating Chevron Navigation Marker)
                userLocation?.let { loc ->
                    val userMarkerState = remember(loc.latitude, loc.longitude) { MarkerState(position = loc) }
                    Marker(
                        state = userMarkerState,
                        title = "Your Position",
                        icon = navigationChevronIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        anchor = Offset(0.5f, 0.5f),
                        rotation = userBearing,
                        flat = true
                    )
                }

                // Destination Markers
                remainingStops.forEachIndexed { index, stop ->
                    if (stop.latitude != 0.0 && stop.longitude != 0.0) {
                        val stopMarkerState = remember(stop.name, stop.latitude, stop.longitude) {
                            MarkerState(position = LatLng(stop.latitude, stop.longitude))
                        }
                        Marker(
                            state = stopMarkerState,
                            title = "${index + 1}. ${stop.name}",
                            snippet = if (index == 0) "Next destination" else "Upcoming stop"
                        )
                    }
                }

                // Polyline Route
                if (roadPoints.size >= 2) {
                    Polyline(
                        points = roadPoints,
                        color = routeColor,
                        width = 14f
                    )
                }
            }

            if (isRerouting && !hasInitialRoute) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = routeColor
                )
            }

            // Bottom Navigation HUD Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = routeColor.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = routeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val nextStop = remainingStops.firstOrNull()
                        Text(
                            text = if (nextStop != null) "Next: ${nextStop.name}" else "Arrived at Destination!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isRerouting -> "Optimizing & Rerouting..."
                                remainingStops.isNotEmpty() -> "${remainingStops.size} stop(s) remaining"
                                else -> "All stops completed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRerouting) routeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentSpeedKmh > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$currentSpeedKmh km/h",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
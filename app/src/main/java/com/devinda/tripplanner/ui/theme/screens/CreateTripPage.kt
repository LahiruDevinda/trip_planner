package com.devinda.tripplanner.ui.theme.screens

import android.app.DatePickerDialog
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devinda.tripplanner.R
import com.devinda.tripplanner.data.model.Trip
import com.devinda.tripplanner.data.model.TripLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripPage(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedTransport by remember { mutableStateOf("Car") }
    val transportModes = listOf("Car", "Van", "Bike", "Train", "Bus", "Flight")

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val addedLocations = remember { mutableStateListOf<TripLocation>() }

    // Calendar Picker Dialog setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val defaultCenter = LatLng(6.9271, 79.8612)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 10f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Trip", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Trip Title") },
                    placeholder = { Text("e.g., Southern Coast Road Trip") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Est. Budget (LKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { datePickerDialog.show() }
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            label = { Text("Trip Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Mode of Transport",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transportModes) { mode ->
                        FilterChip(
                            selected = selectedTransport == mode,
                            onClick = { selectedTransport = mode },
                            label = { Text(mode) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Add Locations / Stops",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search city, town, or landmark") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        enabled = !isSearching,
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                coroutineScope.launch {
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = withContext(Dispatchers.IO) {
                                            @Suppress("DEPRECATION")
                                            geocoder.getFromLocationName(searchQuery, 1)
                                        }

                                        if (!addresses.isNullOrEmpty()) {
                                            val addr = addresses[0]
                                            val loc = TripLocation(
                                                name = addr.featureName ?: searchQuery,
                                                latitude = addr.latitude,
                                                longitude = addr.longitude
                                            )
                                            addedLocations.add(loc)
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(loc.latitude, loc.longitude),
                                                    13f
                                                )
                                            )
                                            searchQuery = ""
                                        } else {
                                            Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error finding location", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        }
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Or tap on map to drop a pin:",
                    style = MaterialTheme.typography.bodySmall
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            coroutineScope.launch {
                                val name = withContext(Dispatchers.IO) {
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        @Suppress("DEPRECATION")
                                        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                        addresses?.firstOrNull()?.locality
                                            ?: addresses?.firstOrNull()?.subAdminArea
                                            ?: addresses?.firstOrNull()?.featureName
                                            ?: "Marked Spot"
                                    } catch (e: Exception) {
                                        "Pinned Location"
                                    }
                                }
                                addedLocations.add(
                                    TripLocation(
                                        name = name,
                                        latitude = latLng.latitude,
                                        longitude = latLng.longitude
                                    )
                                )
                            }
                        }
                    ) {
                        addedLocations.forEachIndexed { index, loc ->
                            Marker(
                                state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                                title = "${index + 1}. ${loc.name}"
                            )
                        }

                        // Preview route line connecting the added pins
                        if (addedLocations.size >= 2) {
                            Polyline(
                                points = addedLocations.map { LatLng(it.latitude, it.longitude) },
                                color = colorResource(id = R.color.button_container),
                                width = 8f
                            )
                        }
                    }
                }
            }

            items(addedLocations) { loc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(loc.name, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = { addedLocations.remove(loc) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        val currentUserId = Firebase.auth.currentUser?.uid
                        if (currentUserId == null) {
                            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (title.isBlank() || addedLocations.isEmpty()) {
                            Toast.makeText(context, "Add title and at least one stop", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSaving = true
                        val trip = Trip(
                            userId = currentUserId,
                            title = title.trim(),
                            estimatedBudget = budget.toDoubleOrNull() ?: 0.0,
                            date = date.trim(),
                            transportType = selectedTransport,
                            locations = addedLocations.toList()
                        )

                        Firebase.firestore.collection("trips")
                            .add(trip)
                            .addOnSuccessListener {
                                isSaving = false
                                Toast.makeText(context, "Trip created successfully!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.button_container)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save & Create Trip", color = Color.White, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
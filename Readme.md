# Trip Planner Android App

A modern, offline-resilient, and intelligent travel companion built using **Jetpack Compose**, **Google Maps Compose**, and **Firebase Firestore**. The app provides end-to-end itinerary organization, interactive place exploration, dynamic route optimization, and real-time turn-by-turn navigation with live GPS vehicle orientation.

---

## 🌟 Key Features

### 1. Interactive Itinerary Planning & Management
- **Map-Based Waypoint Creation**: Add stops by searching location names via Geocoder or selecting coordinates directly on Google Maps.
- **Full CRUD Operations**: Create, view, update, and delete trip itineraries with flexible budget settings, departure dates, and transportation profiles (Car, Van, Bike, Bus, Train, Flight).
- **In-Place Place Management**: Dynamically insert, re-order, or remove waypoints inside existing trips with automatic data re-serialization.

### 2. Intelligent Route Optimization
- **Heuristic TSP Sequencing**: Automatically re-orders stops using nearest-neighbor optimization algorithms (`RouteOptimizer`) from the current vehicle location to reduce travel time and total distance.
- **Dynamic Re-Routing**: Trims traversed polyline segments as waypoints are reached (< 35m proximity trigger) and calculates refreshed routes when drifting off-track.
- **Guaranteed Straight-Line Fallback**: Preserves navigation continuity by rendering direct waypoint polylines if remote routing APIs or network signals drop.

### 3. Real-Time Turn-by-Turn Navigation
- **Live GPS Telemetry**: Fused Location Provider client delivering continuous updates, bearing alignment, and live vehicle speed metrics ($km/h$).
- **Authentic Google Navigation Chevron**: Renders a zero-flicker, 360° rotating forward direction arrow (`flat = true`) anchored over current coordinates.
- **Camera Follow & Re-Center**: Smoothly tracks user bearing and tilt ($40^\circ$) during active navigation, with manual gestures unlocking free exploration and a 1-tap **Recenter** FAB.
- **Live Firestore Sync**: Uses real-time `SnapshotListener` instances so remote edits to destinations reflect immediately on the navigation HUD without restarting the session.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technologies |
|---|---|
| **UI Framework** | Jetpack Compose (Material 3), Compose Navigation |
| **Maps & Location** | Google Maps Compose (`play-services-maps`), Google Play Location Services (`FusedLocationProviderClient`), Android Maps Utils (`PolyUtil`) |
| **Backend & Sync** | Firebase Cloud Firestore, Firebase Android SDK |
| **Concurrency & Async** | Kotlin Coroutines & `StateFlow`, `snapshotFlow` |
| **Build System** | Gradle Version Catalogs (`libs.versions.toml`), Kotlin 2.x |

---

## ⚙️ Configuration & Setup

### 1. Prerequisites
- Android Studio Ladybug | 2024.2+ or newer
- JDK 17+
- Android SDK 35 (API 26 min SDK)
- Active Google Cloud Platform (GCP) project with **Maps SDK for Android** and **Directions API** enabled.
- Active Firebase Project with **Cloud Firestore** activated.

### 2. Credentials Setup

#### Firebase Configuration
Place your project's `google-services.json` inside the `app/` directory:
```text
TripPlanner/
└── app/
    └── google-services.json
```

Ensure the package name in `google-services.json` matches your `applicationId`:
```kotlin
// app/build.gradle.kts
defaultConfig {
    applicationId = "com.devinda.tripplanner"
    minSdk = 26
    targetSdk = 35
}
```

#### Google Maps API Key
Add your Google Maps API key to your `local.properties` file (git-ignored) or directly expose it via `BuildConfig`:

```properties
# local.properties
MAPS_API_KEY=your_actual_google_maps_api_key_here
```

Inject it through `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "API_KEY", "\"${project.findProperty("MAPS_API_KEY") ?: ""}\"")
        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY") ?: ""
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}
```

Add the meta-data entry in `app/src/main/AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

---

## 📱 Permissions & Manifest Declaration

Ensure package visibility and permissions are present in `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Play Services Broker Package Visibility -->
    <queries>
        <package android:name="com.google.android.gms" />
    </queries>

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.TripPlanner">
        <!-- App components -->
    </application>
</manifest>
```

---

## 📂 Project Structure

```text
com.devinda.tripplanner/
├── data/
│   └── model/
│       ├── Trip.kt                 # Firestore-compliant data model with resilient Map parser
│       └── TripLocation.kt         # Stop coordinate model (lat, lon, label)
├── ui/
│   └── theme/
│       └── screens/
│           ├── HomePage.kt           # Trip dashboard & expedition cards
│           ├── TripDetailsPage.kt    # Overview, waypoint timeline & edit dialog
│           └── StartTripMapScreen.kt # Fullscreen navigation HUD & live polyline renderer
└── util/
    ├── DirectionsHelper.kt     # OSRM/Google Directions polyline decoder
    └── RouteOptimizer.kt       # Dynamic heuristic nearest-neighbor optimizer
```

---

## 🚀 Running the Project

1. Clone this repository:
   ```bash
   git clone https://github.com/LahiruDevinda/trip-planner.git
   cd trip-planner
   ```
2. Open the project in Android Studio.
3. Sync Gradle and ensure dependencies resolve without conflict.
4. Run on a physical device or an Android Virtual Device (AVD) running **Google Play Services**.
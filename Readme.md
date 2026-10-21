# 📍 Wayfinder / Trip Planner Android App

A smart, interactive multi-stop trip planning Android application built using **Kotlin**, **Jetpack Compose**, and the **Google Maps Platform (Maps SDK & Routes API)**. The app enables users to plan road journeys, set origins and destinations, add intermediate waypoints, and automatically solve the route sequence using Google's traveling salesperson waypoint optimization engine.

---

## ✨ Features

- 🔐 **User Authentication:** Secure email/password login and registration powered by Firebase Authentication.
- 🗺️ **Interactive Canvas:** Reactive Google Map canvas built with `maps-compose` supporting dynamic stop placement, custom marker styling, and auto-fitting camera bounds.
- ⚡ **Route Optimization:** Multi-waypoint path calculation leveraging the Google Routes API (`computeRoutes`) with automated Traveling Salesperson Problem (TSP) sequence optimization.
- 🚗 **Vehicle Profiles & Mode Routing:** Supports multiple travel modes (`DRIVE` for cars/vans, `TWO_WHEELER` for motorbikes/scooters) with client-side fuel consumption and cost estimation.
- 📐 **Encoded Polyline Decoding:** High-efficiency path rendering via client-side ASCII polyline decompression.
- 💾 **Trip Persistence:** Save and manage custom itineraries with cloud sync and offline access.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture (StateFlow, Unidirectional Data Flow)
- **Mapping & Routing:** Google Maps Compose, Google Routes API v2, Android Maps Utils
- **Networking:** Retrofit 2 + KotlinX Serialization
- **Authentication & Backend:** Firebase Authentication & Cloud Firestore
- **Dependency Management:** Gradle Version Catalogs (`libs.versions.toml`)

---

## ⚙️ Firebase Setup for Contributors

This project uses **Firebase Authentication** for user accounts. Because Firebase configuration files contain project identifiers, the actual `google-services.json` is not tracked in version control.

Follow these steps to connect your own Firebase instance for local development:

### 1. Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **Add project** (or link an existing Google Cloud project).
2. In the left navigation menu, go to **Build > Authentication**.
3. Under the **Sign-in method** tab, enable **Email/Password** provider.

### 2. Register Your Local Android App
1. On the Firebase Project Overview screen, click the **Android icon** (`+ Add app`).
2. Enter the app's package name: `com.devinda.tripplanner` *(or check `applicationId` in `app/build.gradle.kts`)*.
3. Add your local **Debug SHA-1 fingerprint**:
    - In Android Studio, open the **Terminal** tab and run:
      ```bash
      ./gradlew signingReport
      # On Windows:
      .\gradlew signingReport
      ```
    - Copy the `SHA1` string under `Variant: debug` and paste it into Firebase.
4. Click **Register app**.

### 3. Add the Configuration File
1. Download the generated **`google-services.json`** file.
2. Move the downloaded file into your local project's `app/` directory:
   ```text
   TripPlanner/
   ├── app/
   │   ├── google-services.json  <-- Place file here
   │   ├── build.gradle.kts
   │   └── src/
   ├── build.gradle.kts
   └── README.md

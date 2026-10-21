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

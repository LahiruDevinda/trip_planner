package com.devinda.tripplanner.util

import android.content.Context
import android.util.Log
import com.devinda.tripplanner.data.model.TripLocation
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object DirectionsHelper {

    private val client = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4 = addresses.filterIsInstance<Inet4Address>()
                return if (ipv4.isNotEmpty()) ipv4 else addresses
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun fetchRoadPolyline(
        context: Context,
        origin: LatLng,
        stops: List<TripLocation>,
        transportType: String,
        apiKey: String
    ): List<LatLng> = withContext(Dispatchers.IO) {
        if (stops.isEmpty()) return@withContext emptyList()

        // 1. Try plain HTTP OSRM (avoids emulator TLS Handshake drop)
        val points = fetchFromOsrm(
            baseUrl = "http://router.project-osrm.org/route/v1/driving",
            origin = origin,
            stops = stops
        )

        if (points.isNotEmpty()) {
            Log.d("ROUTES_API_DEBUG", "Retrieved ${points.size} road points successfully!")
            return@withContext points
        }

        // 2. Try Secondary HTTP openstreetmap.de router
        val fallbackPoints = fetchFromOsrm(
            baseUrl = "http://routing.openstreetmap.de/routed-car/route/v1/driving",
            origin = origin,
            stops = stops
        )

        if (fallbackPoints.isNotEmpty()) {
            return@withContext fallbackPoints
        }

        // 3. Fallback straight lines
        Log.e("ROUTES_API_DEBUG", "Endpoints failed. Drawing straight line.")
        return@withContext buildList {
            add(origin)
            addAll(stops.map { LatLng(it.latitude, it.longitude) })
        }
    }

    private fun fetchFromOsrm(
        baseUrl: String,
        origin: LatLng,
        stops: List<TripLocation>
    ): List<LatLng> {
        val coordinates = buildList {
            add("${origin.longitude},${origin.latitude}")
            stops.forEach { add("${it.longitude},${it.latitude}") }
        }.joinToString(";")

        val url = "$baseUrl/$coordinates?overview=full&geometries=polyline"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val json = JSONObject(body)
            if (json.optString("code") == "Ok") {
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val geometry = routes.getJSONObject(0).getString("geometry")
                    return PolyUtil.decode(geometry)
                }
            } else {
                Log.w("ROUTES_API_DEBUG", "Router non-OK: $body")
            }
        } catch (e: Exception) {
            Log.e("ROUTES_API_DEBUG", "Fetch error: ${e.message}")
        }
        return emptyList()
    }
}
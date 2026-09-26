package com.roadsos.ui.map

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.roadsos.data.maps.RetrofitClient
import com.roadsos.domain.model.AccidentZone
import com.roadsos.domain.model.RiskLevel
import com.roadsos.domain.repository.AccidentRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Utility to decode Google's encoded polyline string
object PolylineDecoder {
    fun decode(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AccidentRepository(application)
    
    private val _accidentZones = mutableStateOf<List<AccidentZone>>(emptyList())
    val accidentZones: State<List<AccidentZone>> = _accidentZones

    private val _routePoints = mutableStateOf<List<LatLng>>(emptyList())
    val routePoints: State<List<LatLng>> = _routePoints

    private val _routeDistance = mutableStateOf("")
    val routeDistance: State<String> = _routeDistance

    private val _routeDuration = mutableStateOf("")
    val routeDuration: State<String> = _routeDuration

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    
    private val _highRiskCollisionZone = mutableStateOf<AccidentZone?>(null)
    val highRiskCollisionZone: State<AccidentZone?> = _highRiskCollisionZone
    
    private val _overallRisk = mutableStateOf("LOW")
    val overallRisk: State<String> = _overallRisk

    private val _routeOrigin = mutableStateOf("Chennai")
    val routeOrigin: State<String> = _routeOrigin

    private val _routeDestination = mutableStateOf("Tambaram")
    val routeDestination: State<String> = _routeDestination

    private val _routeRiskZones = mutableStateOf<List<AccidentZone>>(emptyList())
    val routeRiskZones: State<List<AccidentZone>> = _routeRiskZones

    init {
        viewModelScope.launch {
            _accidentZones.value = repository.loadAndClusterAccidentData()
        }
    }

    fun getDirections(origin: String, destination: String, apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _routeOrigin.value = origin
            _routeDestination.value = destination
            var routeFound = false

            // 1. Try modern Google Routes API
            try {
                val req = com.roadsos.data.maps.ComputeRoutesRequest(
                    origin = com.roadsos.data.maps.RouteWaypoint(address = origin),
                    destination = com.roadsos.data.maps.RouteWaypoint(address = destination)
                )
                val routesResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.routesApi.computeRoutes(apiKey = apiKey, request = req)
                }
                val computeRoutes = routesResponse.routes
                if (!computeRoutes.isNullOrEmpty()) {
                    val r = computeRoutes[0]
                    val polyline = r.polyline?.encodedPolyline
                    if (!polyline.isNullOrEmpty()) {
                        val decoded = PolylineDecoder.decode(polyline)
                        _routePoints.value = decoded
                        _routeDistance.value = formatDistance(r.distanceMeters)
                        _routeDuration.value = formatDuration(r.duration)
                        checkRouteForRisks(decoded)
                        routeFound = true
                    }
                }
            } catch (e: Exception) {
                // Routes API attempt completed; fallback if not found
            }

            // 2. Fallback to Directions API
            if (!routeFound) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.directionsApi.getDirections(origin, destination, apiKey)
                    }
                    if (response.status != "OK") {
                        val msg = response.error_message ?: "API Error: ${response.status}"
                        if (msg.contains("legacy API", ignoreCase = true) || msg.contains("LegacyApiNotActivatedMapError", ignoreCase = true)) {
                            _error.value = "Directions API is not enabled in Google Cloud Console. Enable 'Directions API' or 'Routes API' in APIs & Services."
                        } else {
                            _error.value = msg
                        }
                    } else if (response.routes.isNotEmpty()) {
                        val route = response.routes[0]
                        val polylineEncoded = route.overview_polyline.points
                        val decoded = PolylineDecoder.decode(polylineEncoded)
                        _routePoints.value = decoded
                        
                        if (route.legs.isNotEmpty()) {
                            _routeDistance.value = route.legs[0].distance.text
                            _routeDuration.value = route.legs[0].duration.text
                        }
                        
                        checkRouteForRisks(decoded)
                        routeFound = true
                    } else {
                        _error.value = "No routes found."
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to calculate route."
                }
            }

            _isLoading.value = false
        }
    }

    private fun formatDistance(meters: Long?): String {
        if (meters == null) return ""
        val km = meters / 1000.0
        return String.format("%.1f km", km)
    }

    private fun formatDuration(duration: String?): String {
        if (duration == null) return ""
        val seconds = duration.removeSuffix("s").toLongOrNull() ?: return duration
        val mins = seconds / 60
        val hours = mins / 60
        val remMins = mins % 60
        return if (hours > 0) "${hours} hr ${remMins} min" else "${mins} mins"
    }

    private suspend fun checkRouteForRisks(route: List<LatLng>) = withContext(Dispatchers.Default) {
        val zones = _accidentZones.value
        if (zones.isEmpty() || route.isEmpty()) {
            withContext(Dispatchers.Main) {
                _highRiskCollisionZone.value = null
                _overallRisk.value = "LOW"
                _routeRiskZones.value = emptyList()
            }
            return@withContext
        }

        // Bounding box of the route with ~5km buffer (0.05 degrees)
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE
        for (pt in route) {
            if (pt.latitude < minLat) minLat = pt.latitude
            if (pt.latitude > maxLat) maxLat = pt.latitude
            if (pt.longitude < minLng) minLng = pt.longitude
            if (pt.longitude > maxLng) maxLng = pt.longitude
        }
        val buffer = 0.05
        val rMinLat = minLat - buffer
        val rMaxLat = maxLat + buffer
        val rMinLng = minLng - buffer
        val rMaxLng = maxLng + buffer

        val candidateZones = zones.filter {
            it.centerLatitude in rMinLat..rMaxLat && it.centerLongitude in rMinLng..rMaxLng
        }

        var maxRisk = RiskLevel.LOW
        var primaryCollision: AccidentZone? = null
        val matchedZones = mutableListOf<AccidentZone>()
        val distanceArray = FloatArray(1)
        val step = if (route.size > 200) 2 else 1

        for (zone in candidateZones) {
            var zoneMatched = false
            for (i in route.indices step step) {
                val point = route[i]
                if (kotlin.math.abs(zone.centerLatitude - point.latitude) > 0.015 ||
                    kotlin.math.abs(zone.centerLongitude - point.longitude) > 0.015) {
                    continue
                }
                Location.distanceBetween(
                    zone.centerLatitude, zone.centerLongitude,
                    point.latitude, point.longitude,
                    distanceArray
                )
                if (distanceArray[0] < 1000f) {
                    if (zone.riskLevel > maxRisk) {
                        maxRisk = zone.riskLevel
                    }
                    if (zone.riskLevel == RiskLevel.HIGH || zone.riskLevel == RiskLevel.CRITICAL) {
                        primaryCollision = zone
                    }
                    zoneMatched = true
                    break
                }
            }
            if (zoneMatched && (zone.riskLevel == RiskLevel.HIGH || zone.riskLevel == RiskLevel.CRITICAL || zone.riskLevel == RiskLevel.MEDIUM)) {
                matchedZones.add(zone)
            }
        }

        withContext(Dispatchers.Main) {
            _overallRisk.value = maxRisk.name
            _highRiskCollisionZone.value = primaryCollision
            _routeRiskZones.value = matchedZones.take(15)
        }
    }
}

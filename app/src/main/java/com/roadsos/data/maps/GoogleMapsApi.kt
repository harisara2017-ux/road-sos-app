package com.roadsos.data.maps

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// Data models for Google Directions API response
data class DirectionsResponse(
    val routes: List<Route>,
    val status: String,
    val error_message: String?
)

data class Route(
    val overview_polyline: OverviewPolyline,
    val legs: List<Leg>
)

data class OverviewPolyline(
    val points: String
)

data class Leg(
    val distance: TextValue,
    val duration: TextValue
)

data class TextValue(
    val text: String,
    val value: Int
)

interface GoogleDirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): DirectionsResponse
}

// Data models for modern Google Routes API
data class ComputeRoutesRequest(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val travelMode: String = "DRIVE"
)

data class RouteWaypoint(
    val address: String
)

data class ComputeRoutesResponse(
    val routes: List<ComputeRoute>?
)

data class ComputeRoute(
    val distanceMeters: Long?,
    val duration: String?,
    val polyline: RoutePolyline?
)

data class RoutePolyline(
    val encodedPolyline: String?
)

interface GoogleRoutesApi {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline",
        @Body request: ComputeRoutesRequest
    ): ComputeRoutesResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://maps.googleapis.com/"
    private const val ROUTES_BASE_URL = "https://routes.googleapis.com/"

    val routesApi: GoogleRoutesApi by lazy {
        Retrofit.Builder()
            .baseUrl(ROUTES_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleRoutesApi::class.java)
    }

    val directionsApi: GoogleDirectionsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleDirectionsApi::class.java)
    }
    
    val placesApi: GooglePlacesApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GooglePlacesApi::class.java)
    }
}

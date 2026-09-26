package com.roadsos.data.maps

import retrofit2.http.GET
import retrofit2.http.Query

data class PlacesResponse(
    val results: List<PlaceResult>
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val vicinity: String, // Address
    val geometry: Geometry,
    val rating: Double?
)

data class Geometry(
    val location: PlaceLocation
)

data class PlaceLocation(
    val lat: Double,
    val lng: Double
)

interface GooglePlacesApi {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyHospitals(
        @Query("location") location: String, // lat,lng
        @Query("radius") radius: Int = 15000, // 15km
        @Query("type") type: String = "hospital",
        @Query("key") apiKey: String
    ): PlacesResponse
}

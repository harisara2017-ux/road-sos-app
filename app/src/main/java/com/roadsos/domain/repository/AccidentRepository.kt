package com.roadsos.domain.repository

import android.content.Context
import com.roadsos.domain.model.AccidentRecord
import com.roadsos.domain.model.AccidentZone
import com.roadsos.domain.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class AccidentRepository(private val context: Context) {

    private val cachedZones = mutableListOf<AccidentZone>()

    suspend fun loadAndClusterAccidentData(): List<AccidentZone> = withContext(Dispatchers.IO) {
        if (cachedZones.isNotEmpty()) return@withContext cachedZones

        val records = mutableListOf<AccidentRecord>()
        
        try {
            val inputStream = context.assets.open("indian_roads_dataset.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Skip header
            reader.readLine()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split(",")
                if (tokens.size >= 24) {
                    try {
                        val record = AccidentRecord(
                            accidentId = tokens[0],
                            latitude = tokens[3].toDoubleOrNull() ?: 0.0,
                            longitude = tokens[4].toDoubleOrNull() ?: 0.0,
                            date = tokens[5],
                            severity = tokens[18],
                            cause = tokens[17],
                            riskScore = tokens[23].toDoubleOrNull() ?: 0.0
                        )
                        records.add(record)
                    } catch (e: Exception) {
                        // Skip malformed row
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Clustering algorithm (simplified spatial grouping)
        val zones = clusterRecordsIntoZones(records)
        cachedZones.addAll(zones)
        return@withContext cachedZones
    }

    private fun clusterRecordsIntoZones(records: List<AccidentRecord>): List<AccidentZone> {
        // Group by rounding lat/lng to 2 decimal places (roughly 1.1km grid) using fast integer math
        val grouped = records.groupBy { 
            Pair(
                (it.latitude * 100).toInt(), 
                (it.longitude * 100).toInt()
            )
        }

        return grouped.entries.mapIndexed { index, entry ->
            val clusterRecords = entry.value
            val count = clusterRecords.size
            var sumLat = 0.0
            var sumLng = 0.0
            for (r in clusterRecords) {
                sumLat += r.latitude
                sumLng += r.longitude
            }
            val centerLat = if (count > 0) sumLat / count else 0.0
            val centerLng = if (count > 0) sumLng / count else 0.0
            
            // Determine Risk Level based on frequency
            val riskLevel = when {
                count >= 50 -> RiskLevel.CRITICAL
                count >= 20 -> RiskLevel.HIGH
                count >= 5 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }

            AccidentZone(
                zoneId = "ZONE_$index",
                centerLatitude = centerLat,
                centerLongitude = centerLng,
                accidentCount = count,
                riskLevel = riskLevel,
                historicalPeriod = "Historical"
            )
        }
    }
}

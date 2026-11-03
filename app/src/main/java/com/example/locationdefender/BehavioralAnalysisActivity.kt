package com.example.locationdefender

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BehavioralAnalysisActivity : AppCompatActivity(), LocationListener {
    
    private lateinit var locationManager: LocationManager
    private lateinit var handler: Handler
    private var isMonitoring = false
    private val locationHistory = mutableListOf<LocationData>()
    private var lastFixTime = 0L
    private var lastFixLocation: Location? = null
    
    // Thresholds
    private val MAX_REALISTIC_SPEED_KMH = 150.0
    private val MIN_SATELLITE_COUNT = 4
    private val MAX_ACCURACY_JUMP_METERS = 500.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_behavioral)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        handler = Handler(Looper.getMainLooper())

        setupUI()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (!isMonitoring) startBehavioralMonitoring()
        }
        
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            if (isMonitoring) stopBehavioralMonitoring()
        }
        
        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            locationHistory.clear()
            lastFixLocation = null
            updateAnalysisUI()
        }
    }

    private fun startBehavioralMonitoring() {
        isMonitoring = true
        
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1 second interval
                10f,  // 10 meters minimum distance
                this,
                handler
            )
            
            runOnUiThread {
                findViewById<TextView>(R.id.tv_status).text = "🟢 Monitoring Active"
                findViewById<TextView>(R.id.tv_status).setTextColor(0xFF4CAF50.toInt())
            }
            
        } catch (e: SecurityException) {
            android.widget.Toast.makeText(this, "Location permission denied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopBehavioralMonitoring() {
        isMonitoring = false
        locationManager.removeUpdates(this)
        
        runOnUiThread {
            findViewById<TextView>(R.id.tv_status).text = "⚫ Monitoring Stopped"
            findViewById<TextView>(R.id.tv_status).setTextColor(0xFFFF9800.toInt())
        }
    }

    override fun onLocationChanged(location: Location) {
        val now = System.currentTimeMillis()
        val data = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            speed = location.speed,
            bearing = location.bearing,
            altitude = location.altitude,
            provider = location.provider ?: "unknown",
            timestamp = now,
            isMock = location.isMock,
            isFromMockProvider = location.isFromMockProvider()
        )
        
        locationHistory.add(data)
        
        if (locationHistory.size > 50) {
            locationHistory.removeAt(0)
        }
        
        analyzeBehavioralPatterns(data)
        updateAnalysisUI()
    }

    private fun analyzeBehavioralPatterns(current: LocationData) {
        val issues = mutableListOf<String>()
        var behavioralScore = 100

        // 1. Mock indicators
        if (current.isMock || current.isFromMockProvider) {
            issues.add("Mock location detected")
            behavioralScore -= 50
        }

        // 2. Speed analysis
        if (locationHistory.size >= 2) {
            val previous = locationHistory[locationHistory.size - 2]
            val timeDiff = (current.timestamp - previous.timestamp) / 1000.0
            
            if (timeDiff > 0) {
                val dist = calculateDistance(previous, current)
                val speedKmh = (dist / timeDiff) * 3.6
                
                if (speedKmh > MAX_REALISTIC_SPEED_KMH) {
                    issues.add("Impossible speed: $${String.format("%.0f", speedKmh)} km/h")
                    behavioralScore -= 40
                }
                
                if (dist > MAX_ACCURACY_JUMP_METERS && timeDiff < 5) {
                    issues.add("Teleportation: $${String.format("%.0f", dist)}m in ${"%.1f".format(timeDiff)}s")
                    behavioralScore -= 35
                }
            }
        }

        // 3. Coordinate jitter analysis
        if (locationHistory.size >= 5) {
            val recent = locationHistory.takeLast(5)
            val latVar = calculateVariance(recent.map { it.latitude })
            val lngVar = calculateVariance(recent.map { it.longitude })
            
            if (latVar < 0.0000001 && lngVar < 0.0000001) {
                issues.add("No coordinate jitter (suspicious)")
                behavioralScore -= 20
            }
        }

        // 4. Accuracy stability
        if (locationHistory.size >= 3) {
            val recentAccuracies = locationHistory.takeLast(3).map { it.accuracy }
            val accVariance = calculateVariance(recentAccuracies)
            
            if (accVariance < 0.01 && recentAccuracies.all { it < 5.0 }) {
                issues.add("Unnaturally perfect accuracy")
                behavioralScore -= 15
            }
        }

        // 5. Timestamp integrity
        if (locationHistory.size >= 2) {
            val prev = locationHistory[locationHistory.size - 2]
            val timeDiff = current.timestamp - prev.timestamp
            
            if (timeDiff < 0) {
                issues.add("Backward timestamp detected")
                behavioralScore -= 30
            }
            
            if (locationHistory.size >= 10) {
                val intervals = mutableListOf<Long>()
                for (i in 1 until locationHistory.size) {
                    intervals.add(locationHistory[i].timestamp - locationHistory[i-1].timestamp)
                }
                val intervalVar = calculateVariance(intervals.map { it.toDouble() })
                if (intervalVar < 100) {
                    issues.add("Too regular update intervals")
                    behavioralScore -= 10
                }
            }
        }

        // 6. Position stability with perfect accuracy
        if (locationHistory.size >= 10) {
            val recent = locationHistory.takeLast(10)
            val maxLatDiff = recent.maxOf { it.latitude } - recent.minOf { it.latitude }
            val maxLngDiff = recent.maxOf { it.longitude } - recent.minOf { it.longitude }
            
            if (maxLatDiff < 0.00001 && maxLngDiff < 0.00001 && 
                recent.all { it.accuracy < 3.0 }) {
                issues.add("Suspiciously stable position with perfect accuracy")
                behavioralScore -= 15
            }
        }

        runOnUiThread {
            val resultText = if (issues.isEmpty()) {
                "✅ No behavioral anomalies detected"
            } else {
                issues.joinToString("\n") { "⚠️ $it" }
            }
            
            findViewById<TextView>(R.id.tv_behavioral_result).text = resultText
            findViewById<TextView>(R.id.tv_behavioral_score).text = 
                "Behavioral Score: $behavioralScore/100"
            
            when {
                behavioralScore >= 80 -> {
                    findViewById<TextView>(R.id.tv_behavioral_status).text = "✅ TRUSTED"
                    findViewById<TextView>(R.id.tv_behavioral_status).setTextColor(0xFF4CAF50.toInt())
                }
                behavioralScore >= 50 -> {
                    findViewById<TextView>(R.id.tv_behavioral_status).text = "⚠️ SUSPICIOUS"
                    findViewById<TextView>(R.id.tv_behavioral_status).setTextColor(0xFFFFC107.toInt())
                }
                else -> {
                    findViewById<TextView>(R.id.tv_behavioral_status).text = "❌ COMPROMISED"
                    findViewById<TextView>(R.id.tv_behavioral_status).setTextColor(0xFFFF4444.toInt())
                }
            }
        }
    }

    private fun calculateDistance(a: LocationData, b: LocationData): Double {
        val loc1 = android.location.Location("temp1").apply {
            latitude = a.latitude
            longitude = a.longitude
        }
        val loc2 = android.location.Location("temp2").apply {
            latitude = b.latitude
            longitude = b.longitude
        }
        return loc1.distanceTo(loc2)
    }

    private fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val squaredDiffs = values.map { (it - mean) * (it - mean) }
        return squaredDiffs.average()
    }

    private fun updateAnalysisUI() {
        runOnUiThread {
            findViewById<TextView>(R.id.tv_location_count).text = 
                "Samples: ${locationHistory.size}"
            
            if (locationHistory.isNotEmpty()) {
                val latest = locationHistory.last()
                findViewById<TextView>(R.id.tv_latest_lat).text = 
                    "Lat: ${String.format("%.6f", latest.latitude)}"
                findViewById<TextView>(R.id.tv_latest_lng).text = 
                    "Lng: ${String.format("%.6f", latest.longitude)}"
                findViewById<TextView>(R.id.tv_latest_acc).text = 
                    "Accuracy: ${String.format("%.1f", latest.accuracy)}m"
                findViewById<TextView>(R.id.tv_latest_provider).text = 
                    "Provider: ${latest.provider}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBehavioralMonitoring()
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val speed: Float,
        val bearing: Float,
        val altitude: Double,
        val provider: String,
        val timestamp: Long,
        val isMock: Boolean,
        val isFromMockProvider: Boolean
    )
}

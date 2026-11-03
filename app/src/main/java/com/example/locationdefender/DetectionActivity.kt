package com.example.locationdefender

import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetectionActivity : AppCompatActivity() {
    
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private var lastTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        
        findViewById<Button>(R.id.btn_check).setOnClickListener {
            checkLocationSecurity()
        }
        
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            lastLocation = null
            lastTime = 0L
            findViewById<TextView>(R.id.tv_result).text = "Cache cleared"
        }
        
        checkLocationSecurity()
    }

    private fun checkLocationSecurity() {
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            updateUI(0, "No location available", emptyList())
            return
        }
        
        val (score, issues) = analyzeLocation(location)
        updateUI(score, location.toString(), issues)
    }

    private fun analyzeLocation(location: Location): Pair<Int, List<String>> {
        val issues = mutableListOf<String>()
        var trustScore = 100

        // ========== LAYER 1: API Check ==========
        if (location.isFromMockProvider()) {
            trustScore -= 50
            issues.add("❌ Layer 1: Mock Provider Detected")
        }
        if (location.isMock) {
            trustScore -= 50
            issues.add("❌ Layer 1: Mock Location Flag Set")
        }

        // ========== LAYER 2: Accuracy Check ==========
        if (location.hasAccuracy()) {
            if (location.accuracy < 1.0f) {
                trustScore -= 30
                issues.add("⚠️ Layer 2: Suspiciously Perfect Accuracy (< 1m)")
            } else if (location.accuracy > 1000f) {
                trustScore -= 10
                issues.add("⚠️ Layer 2: Very Low Accuracy (> 1km)")
            }
        }

        // ========== LAYER 3: Speed/Velocity Check ==========
        val currentTime = System.currentTimeMillis()
        if (lastLocation != null && lastTime != 0L) {
            val timeDelta = (currentTime - lastTime) / 1000.0
            if (timeDelta > 0) {
                val distance = lastLocation!!.distanceTo(location)
                val speed = distance / timeDelta // m/s
                val speedKmh = speed * 3.6
                
                if (speedKmh > 150) {
                    trustScore -= 40
                    issues.add("❌ Layer 3: Impossible Speed: $${String.format("%.0f", speedKmh)} km/h")
                } else if (speedKmh > 80) {
                    trustScore -= 20
                    issues.add("⚠️ Layer 3: High Speed: $${String.format("%.0f", speedKmh)} km/h")
                }
                
                // Teleportation check
                if (distance > 500 && timeDelta < 5) {
                    trustScore -= 35
                    issues.add("❌ Layer 3: Teleportation: $${String.format("%.0f", distance)}m in ${"%.1f".format(timeDelta)}s")
                }
            }
        }
        lastLocation = location
        lastTime = currentTime

        // ========== LAYER 4: Timestamp Check ==========
        val timeDiff = currentTime - location.time
        if (location.time > currentTime + 60000) {
            trustScore -= 30
            issues.add("❌ Layer 4: Future Timestamp Detected")
        } else if (timeDiff > 3600000) {
            trustScore -= 20
            issues.add("⚠️ Layer 4: Stale Location (> 1 hour)")
        }

        // ========== LAYER 5: Provider Check ==========
        when (location.provider) {
            null -> {
                trustScore -= 15
                issues.add("⚠️ Layer 5: Null Provider")
            }
            "gps" -> {} // Normal
            "network" -> {
                if (location.hasAccuracy() && location.accuracy < 10f) {
                    trustScore -= 15
                    issues.add("⚠️ Layer 5: Network provider with GPS accuracy")
                }
            }
            "mock", "fused" -> {
                trustScore -= 25
                issues.add("⚠️ Layer 5: Suspicious Provider: $${location.provider}")
            }
            else -> {
                trustScore -= 10
                issues.add("ℹ️ Layer 5: Unknown Provider: $${location.provider}")
            }
        }

        // ========== LAYER 6: Coordinate Analysis ==========
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            trustScore -= 20
            issues.add("❌ Layer 6: Zero Coordinates (Origin Point)")
        }
        
        if (lastLocation != null) {
            val latDiff = Math.abs(location.latitude - lastLocation!!.latitude)
            val lngDiff = Math.abs(location.longitude - lastLocation!!.longitude)
            if (latDiff < 0.0000001 && lngDiff < 0.0000001) {
                trustScore -= 15
                issues.add("⚠️ Layer 6: Identical Coordinates (No Jitter)")
            }
        }

        return Pair(Math.max(0, trustScore), issues)
    }

    private fun updateUI(score: Int, locationStr: String, issues: List<String>) {
        val resultView = findViewById<TextView>(R.id.tv_result)
        val scoreView = findViewById<TextView>(R.id.tv_score)
        val statusView = findViewById<TextView>(R.id.tv_status)
        val issuesView = findViewById<TextView>(R.id.tv_issues)
        
        scoreView.text = "Trust Score: $score/100"
        
        when {
            score >= 80 -> {
                statusView.text = "✅ SECURE"
                statusView.setTextColor(0xFF4CAF50.toInt())
            }
            score >= 50 -> {
                statusView.text = "⚠️ SUSPICIOUS"
                statusView.setTextColor(0xFFFFC107.toInt())
            }
            else -> {
                statusView.text = "❌ COMPROMISED"
                statusView.setTextColor(0xFFFF4444.toInt())
            }
        }
        
        val issuesText = if (issues.isEmpty()) "✅ No issues detected" else issues.joinToString("\n")
        issuesView.text = issuesText
        
        resultView.text = """
            Lat: ${String.format("%.6f", location.latitude)}
            Lng: ${String.format("%.6f", location.longitude)}
            Accuracy: ${String.format("%.1f", location.accuracy)}m
            Speed: ${String.format("%.1f", location.speed * 3.6)} km/h
            Provider: ${location.provider ?: "null"}
        """.trimIndent()
    }
}

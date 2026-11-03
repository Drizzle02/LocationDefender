package com.example.locationdefender

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            updateStatus("✅ Permissions Granted\nReady to monitor")
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE
            )
        }

        findViewById<Button>(R.id.btn_detection).setOnClickListener {
            startActivity(android.content.Intent(this, DetectionActivity::class.java))
        }

        findViewById<Button>(R.id.btn_behavioral).setOnClickListener {
            startActivity(android.content.Intent(this, BehavioralAnalysisActivity::class.java))
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        getSystemService(android.content.Context.PERMISSION_SERVICE) as PackageManager
            .checkSelfPermission(packageName, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            updateStatus("✅ All Permissions Granted")
        }
    }

    private fun updateStatus(text: String) {
        findViewById<TextView>(R.id.tv_status).text = text
    }

    companion object {
        private const val REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

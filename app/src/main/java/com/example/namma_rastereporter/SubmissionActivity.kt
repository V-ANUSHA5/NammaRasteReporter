package com.example.namma_rastereporter

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class SubmissionActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private lateinit var locationTextView: TextView
    private lateinit var locationEditText: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission)

        val imageUriString = intent.getStringExtra("imageUri")
        val imageUri = Uri.parse(imageUriString)

        val imageView: ImageView = findViewById(R.id.capturedImageView)
        imageView.setImageURI(imageUri)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val layout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.submissionLayout)
        
        locationTextView = TextView(this).apply {
            text = getString(R.string.fetching_location)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 20, 0, 0)
            textSize = 14f
        }
        layout.addView(locationTextView, 1)

        locationEditText = EditText(this).apply {
            hint = getString(R.string.edit_location_hint)
            visibility = View.GONE
        }
        layout.addView(locationEditText, 2)

        submitButton = findViewById(R.id.submitButton)
        submitButton.isEnabled = false
        submitButton.text = getString(R.string.waiting_for_location)

        requestLocationUpdates()

        val typeRadioGroup: RadioGroup = findViewById(R.id.typeRadioGroup)
        val severityRadioGroup: RadioGroup = findViewById(R.id.severityRadioGroup)
        val ticketIdTextView: TextView = findViewById(R.id.ticketIdTextView)

        submitButton.setOnClickListener {
            val selectedTypeId = typeRadioGroup.checkedRadioButtonId
            val type = findViewById<RadioButton>(selectedTypeId).text.toString()

            val selectedSeverityId = severityRadioGroup.checkedRadioButtonId
            val severity = findViewById<RadioButton>(selectedSeverityId).text.toString()

            val finalAddress = locationEditText.text.toString()
            
            val ticketNumber = generateNextTicketNumber()
            val ticketId = ticketNumber.toString()
            
            saveReport(ticketId, type, severity, imageUriString ?: "", finalAddress)

            // Success message showing both the Ticket No and the Exact GPS coordinates
            val successMessage = "Success! Ticket No: $ticketId\nArea: $finalAddress\nExact GPS: $currentLatitude, $currentLongitude"
            ticketIdTextView.text = successMessage
            ticketIdTextView.visibility = View.VISIBLE
            submitButton.isEnabled = false
            Toast.makeText(this, "Ticket #$ticketId Generated with GPS Data", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateNextTicketNumber(): Int {
        val sharedPrefs = getSharedPreferences("app_stats", MODE_PRIVATE)
        val lastNumber = sharedPrefs.getInt("last_ticket_number", 1000)
        val nextNumber = lastNumber + 1
        sharedPrefs.edit().putInt("last_ticket_number", nextNumber).apply()
        return nextNumber
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    var lat = location.latitude
                    var lon = location.longitude

                    // FORCE FIX FOR EMULATOR
                    // If emulator is at default Google HQ (37.422, -122.084)
                    // we FORCE it to Sambhram Institute of Technology
                    if (lat >= 37.421 && lat <= 37.423 && lon <= -122.083 && lon >= -122.085) {
                        lat = 13.0886
                        lon = 77.5458
                    }

                    currentLatitude = lat
                    currentLongitude = lon
                    updateLocationName(lat, lon)
                }
            }
        }, mainLooper)
    }

    private fun updateLocationName(lat: Double, lon: Double) {
        try {
            // If it's the Sambhram coordinates, we can force the name directly to be sure
            if (lat == 13.0886 && lon == 77.5458) {
                val sambhramAddress = "Sambhram Institute of Technology, M S Palya, Jalahalli East, Bangalore, Karnataka"
                runOnUiThread {
                    locationTextView.text = getString(R.string.location_format, sambhramAddress, lat, lon)
                    if (locationEditText.text.isEmpty()) {
                        locationEditText.setText(sambhramAddress)
                    }
                    locationEditText.visibility = View.VISIBLE
                    submitButton.isEnabled = true
                    submitButton.text = getString(R.string.submit_report)
                }
                return
            }

            val indianLocale = Locale("en", "IN")
            val geocoder = Geocoder(this, indianLocale)
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val fullAddress = address.getAddressLine(0) ?: "$lat, $lon"
                
                runOnUiThread {
                    // Show BOTH Address and Raw GPS Coordinates for "Exactness"
                    locationTextView.text = getString(R.string.location_format, fullAddress, lat, lon)
                    
                    if (locationEditText.text.isEmpty()) {
                        locationEditText.setText(fullAddress)
                    }
                    locationEditText.visibility = View.VISIBLE
                    
                    if (!submitButton.isEnabled) {
                        submitButton.isEnabled = true
                        submitButton.text = getString(R.string.submit_report)
                    }
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                locationTextView.text = getString(R.string.location_format, "Area not found", lat, lon)
                if (locationEditText.text.isEmpty()) {
                    locationEditText.setText("Unknown Area")
                }
                locationEditText.visibility = View.VISIBLE
                submitButton.isEnabled = true
                submitButton.text = getString(R.string.submit_report)
            }
        }
    }

    private fun saveReport(ticketId: String, type: String, severity: String, imagePath: String, placeName: String) {
        val sharedPrefs = getSharedPreferences("reports", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("${ticketId}_type", type)
            putString("${ticketId}_severity", severity)
            putString("${ticketId}_image", imagePath)
            putString("${ticketId}_lat", currentLatitude.toString())
            putString("${ticketId}_long", currentLongitude.toString())
            putString("${ticketId}_place", placeName)
            putString("${ticketId}_status", "Submitted")
            apply()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        }
    }
}

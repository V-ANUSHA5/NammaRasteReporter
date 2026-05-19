package com.example.namma_rastereporter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var loginContainer: LinearLayout
    private lateinit var statusResultTextView: TextView
    private lateinit var ticketIdEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginContainer = findViewById(R.id.loginContainer)
        statusResultTextView = findViewById(R.id.statusResultTextView)
        ticketIdEditText = findViewById(R.id.ticketIdEditText)

        val reportButton: Button = findViewById(R.id.reportButton)
        val trackButton: Button = findViewById(R.id.trackButton)
        val loginButton: Button = findViewById(R.id.loginButton)
        val usernameEditText: EditText = findViewById(R.id.usernameEditText)

        // Simple login check
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            loginContainer.visibility = View.VISIBLE
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            if (username.isNotEmpty()) {
                sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                loginContainer.visibility = View.GONE
                Toast.makeText(this, "Logged in as $username", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        reportButton.setOnClickListener {
            if (sharedPrefs.getBoolean("is_logged_in", false)) {
                startActivity(Intent(this, ReportActivity::class.java))
            } else {
                loginContainer.visibility = View.VISIBLE
            }
        }

        trackButton.setOnClickListener {
            val ticketId = ticketIdEditText.text.toString()
            if (ticketId.isNotEmpty()) {
                val reportsPrefs = getSharedPreferences("reports", MODE_PRIVATE)
                val status = reportsPrefs.getString("${ticketId}_status", "Not Found")
                statusResultTextView.text = getString(R.string.status_label, status)
            } else {
                Toast.makeText(this, "Enter Ticket ID", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
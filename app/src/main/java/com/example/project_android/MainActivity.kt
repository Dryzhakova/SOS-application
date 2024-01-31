package com.example.project_android

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.project_android.Contacts.ContactsDetails
import android.Manifest

class MainActivity : AppCompatActivity() {

    private val permissionsBelowAndroid14 = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.RECORD_AUDIO
    )

    // For Android 14 and above
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val permissionsAndroid14AndAbove = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.FOREGROUND_SERVICE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestCodePermissions = 123

    private var isTimerStarted: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var toggleButton: Button
    private lateinit var status: TextView
    private lateinit var gpsTextView: TextView
    private lateinit var accTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        toggleButton = findViewById(R.id.toggleButton)
        status = findViewById(R.id.statusTextView)
        gpsTextView = findViewById(R.id.gpsTextView)
        accTextView = findViewById(R.id.accTextView)

        ContactsDetails.message = sharedPreferences.getString("selected_message", "").toString()
        ContactsDetails.number = sharedPreferences.getString("selected_contact_number", "").toString()

        Log.d("Message", ContactsDetails.message)
        Log.d("Message", ContactsDetails.number)

        toggleButton.setOnClickListener {
            toggleSystem()
        }
    }

    private fun toggleSystem() {
        if (isTimerStarted) {
            status.text = "Status: System turned OFF"
            stopBackgroundService()
        } else {
            if (checkPermissions()) {
                if (ContactsDetails.number != "") {
                    status.text = "Status: System turned ON"
                    startBackgroundService()
                    Log.d("MainActivity", "Kraken is on the hunt")
                } else {
                    showErrorNameMessage()
                }
            }
        }
        isTimerStarted = !isTimerStarted
    }

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        startService(serviceIntent)
    }

    private fun stopBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        stopService(serviceIntent)
    }

    private fun showErrorNameMessage() {
        Toast.makeText(
            this,
            "Please select a contact in settings",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkPermissions(): Boolean {
        val missingPermissions = getMissingPermissions()

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions)
            return false
        } else {
            return true
        }
    }

    private fun getMissingPermissions(): Array<String> {
        val missingPermissions = mutableListOf<String>()

        val currentPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsAndroid14AndAbove
        } else {
            permissionsBelowAndroid14
        }

        for (permission in currentPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }

        return missingPermissions.toTypedArray()
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, requestCodePermissions)
    }

    private fun showPermissionDeniedToast() {
        Toast.makeText(
            this,
            "Please allow all the necessary permissions",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Other lifecycle methods...
}

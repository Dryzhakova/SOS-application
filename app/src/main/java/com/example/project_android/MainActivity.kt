package com.example.project_android



import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.project_android.Accelerometer.AccelerometerListener
import com.example.project_android.Location.LocationHelper
import com.example.project_android.Message.SMS
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.project_android.Contacts.ContactsDetails
import com.example.project_android.Timer.Timer
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    // MiniTasks:
    // 1. Пока что таймер просто запускается, но его никак не выключить. Кроме как, кнопкой TurnOff

    private var shakeTimerHandler = Handler(Looper.getMainLooper())
    private var shakeTimerRunnable: Runnable? = null
    private var isTimerStarted : Boolean = false

    val permissionsBelowAndroid14 = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.RECORD_AUDIO
    )

    // Для Android 14 и выше

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val permissionsAndroid14AndAbove = arrayOf(
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

    public lateinit var sharedPreferences: SharedPreferences

    private lateinit var accelerometerListener: AccelerometerListener
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsSender: SMS
    private lateinit var timer: Timer
    // Elements of layout //

    private lateinit var toggleButton : Button
    private lateinit var turnOn : Button
    private lateinit var turnOff : Button
    private lateinit var status : TextView

    private lateinit var gpsTextView: TextView
    private lateinit var accTextView: TextView

    private var lastAcceleration: FloatArray? = null
    private var lastLocation: Pair<Double, Double>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // initialization buttons and TextView
        toggleButton = findViewById(R.id.toggleButton)
        status = findViewById(R.id.statusTextView)
        gpsTextView = findViewById(R.id.gpsTextView)
        accTextView = findViewById(R.id.accTextView)


        ContactsDetails.message = sharedPreferences.getString("selected_message", "").toString()
        ContactsDetails.number = sharedPreferences.getString("selected_contact_number", "").toString()


        Log.d("Message", ContactsDetails.message)
        Log.d("Message", ContactsDetails.number)






        toggleButton.setOnClickListener {
            if (isTimerStarted) {
                // Code to perform action when turning OFF
                status.text = "Status: System turned OFF"
                val serviceIntent = Intent(this, BackgroundService::class.java)
                stopService(serviceIntent)
            } else {
                // Code to perform action when turning ON
                if (checkPermissions()) {
                    if(ContactsDetails.number != "" && ContactsDetails.message != "") {
                        status.text = "Status: System turned ON"
                        val serviceIntent = Intent(this, BackgroundService::class.java)
                        startService(serviceIntent)
                        Log.d("MainActivity", "Kraken is on the hunt")
                    }else{
                        showErrorNameMessage()

                    }
                } else {
                    // Handle case when permissions are not granted
                }
            }
            // Toggle the state
            isTimerStarted = !isTimerStarted
        }



    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Обрабатываем нажатие на пункт меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Открываем SettingsActivity при нажатии на кнопку настроек
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            android.R.id.home -> {
                // Обработка нажатия на кнопку "назад" в ActionBar
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Checking Permissions




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
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(permission)
            }
        }

        return missingPermissions.toTypedArray()
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, requestCodePermissions)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestCodePermissions) {
            val deniedPermissions = mutableListOf<String>()

            for ((index, result) in grantResults.withIndex()) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[index])
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                showPermissionDeniedToast()
            } else {
                // Разрешения предоставлены
                // Ваш код здесь
            }
        }
    }

    private fun showPermissionDeniedToast() {
        Toast.makeText(
            this,
            "Необходимые разрешения не были предоставлены",
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun showErrorNameMessage() {
        Toast.makeText(
            this,
            "Не вписаны номер телефона и сообщение",
            Toast.LENGTH_SHORT
        ).show()
    }





    // При изменении данных с акселерометра вызывает проверку GPS











    // Method Euclidian for calculating acceleration change





    override fun onResume() {
        super.onResume()
//        if (::accelerometerListener.isInitialized) {
//            accelerometerListener.register()
//        }

    }

    override fun onPause() {
        super.onPause()

    }

    // For testing







}


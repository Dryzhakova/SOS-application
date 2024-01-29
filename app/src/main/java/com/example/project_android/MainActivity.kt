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
        Manifest.permission.READ_CONTACTS
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
        Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
    )

    private val requestCodePermissions = 123


    private lateinit var accelerometerListener: AccelerometerListener
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsSender: SMS
    private lateinit var timer: Timer
    // Elements of layout //
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

        // initialization buttons and TextView
        turnOn = findViewById(R.id.turnOnButton)
        turnOff = findViewById(R.id.turnOffButton)
        status = findViewById(R.id.statusTextView)
        gpsTextView = findViewById(R.id.gpsTextView)
        accTextView = findViewById(R.id.accTextView)










        turnOn.setOnClickListener {
            if (checkPermissions()) {
                val serviceIntent = Intent(this, BackgroundService::class.java)
                startService(serviceIntent)
                Log.d("MainActivity", "Кракен вышел на охоту")
            } else {
                // Обработка случая, когда разрешения не получены
                // Можете вывести сообщение или выполнить другие действия
            }
        }

        turnOff.setOnClickListener {
            // Выключение слушателей и т.д.
            status.text = "Система выключена"
            val serviceIntent = Intent(this, BackgroundService::class.java)
            stopService(serviceIntent)
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


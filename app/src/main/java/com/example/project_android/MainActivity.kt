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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
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





        if(checkPermissions()) {
            accelerometerListener = AccelerometerListener(this) { acceleration ->
                onAccelerationChanged(acceleration)
            }

            locationHelper = LocationHelper(this) { latitude, longitude ->
                if (checkLocation(latitude, longitude)) {
                    timer.startTimer(30000) // 30 секунд
                }
            }
        }

        smsSender = SMS(this)

        timer = Timer {
            showFallDetectedDialog()
            smsSender.sendSMS("Началась тревога!")
        }


        turnOn.setOnClickListener {
            // Включение слушателей и т.д.
            status.text = "Система включена"
            accelerometerListener.register()
            locationHelper.requestLocationUpdates()
        }

        turnOff.setOnClickListener {
            // Выключение слушателей и т.д.
            status.text = "Система выключена"
            accelerometerListener.unregister()
            locationHelper.stopLocationUpdates()
            timer.cancelTimer()
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
    private fun checkPermissions(): Boolean{
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )

        for(permission in permissions){
            if(ContextCompat.checkSelfPermission(this,permission)
                != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, permissions, 1)
                return false
            }
        }
        return true
    }

    // При изменении данных с акселерометра вызывает проверку GPS
    private fun onAccelerationChanged(acceleration: FloatArray) {
        if (checkAcceleration(acceleration)) {
            locationHelper.requestLocationUpdates()
        }
    }


    private fun checkAcceleration(acceleration: FloatArray): Boolean {
        if (lastAcceleration != null) {
            val accelerationChange = calculateAccelerationChange(acceleration, lastAcceleration!!)

            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            // Retrieve the accelerometer sensitivity directly as an integer
            val accelerometerSensitivity: Int = try {
                preferences.getInt("accelerometer_sensitivity", 5)
            } catch (e: ClassCastException) {
                // Handle exception, if necessary
                5
            }
            if (accelerationChange > accelerometerSensitivity) {
                // Изменились данные акселерометра
                Log.d("Ac", "Сработало acceleration!")
                lastAcceleration = acceleration
                accTextView.text = "Acceleration: $lastAcceleration"
                return true
            }
        }

        // Сохранение новых данных акселерометра(обновляются каждые 200 мс)
        lastAcceleration = acceleration
        val accelerationString = "x: ${acceleration[0]}, y: ${acceleration[1]}, z: ${acceleration[2]}"
        accTextView.text = "Acceleration: $accelerationString"

        return false
    }


    // Method Euclidian for calculating acceleration change
    private fun calculateAccelerationChange(acceleration1: FloatArray, acceleration2: FloatArray): Double {
        val deltaAcceleration = FloatArray(3)
        for (i in 0 until 3) {
            deltaAcceleration[i] = acceleration1[i] - acceleration2[i]
        }

        val value = sqrt((deltaAcceleration[0] * deltaAcceleration[0]
                + deltaAcceleration[1] * deltaAcceleration[1]
                + deltaAcceleration[2] * deltaAcceleration[2]).toDouble())
        return value

    }


    private fun checkLocation(latitude: Double, longitude: Double): Boolean {
        // Проверяем изменение координат
        if (lastLocation != null) {
            val deltaLatitude = abs(latitude - lastLocation!!.first)
            val deltaLongitude = abs(longitude - lastLocation!!.second)

            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val gpsSensitivity: Double =
                (preferences.getInt("gps_sensitivity", 10) * 0.00001)

            if (deltaLatitude > gpsSensitivity || deltaLongitude > gpsSensitivity) {
                // Изменились данные GPS
                lastLocation = Pair(latitude, longitude)

                gpsTextView.text = "GPS: $lastLocation"

                // For Testing
                Log.d("GPS", "Сработало GPS!")
                showFallDetectedDialog()
                return true
            }
            Log.d("GPS", "No location")
        }

        // Сохраняем текущие значения координат
        lastLocation = Pair(latitude, longitude)
        gpsTextView.text = "GPS: $lastLocation"
        return false
    }

    override fun onResume() {
        super.onResume()
        accelerometerListener.register()

    }

    override fun onPause() {
        super.onPause()
        accelerometerListener.unregister()
        locationHelper.stopLocationUpdates()
        timer.cancelTimer()
    }

    // For testing
    private fun showFallDetectedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wykryt Wypadek")
        builder.setMessage("Było wykryto wypadek. Wezwać pomoc?")
        builder.setPositiveButton("Да") { _, _ ->
            // Здесь можно добавить код для вызова помощи
            // Например, отправка SMS с предупреждением о падении
        }
        builder.setNegativeButton("Нет") { _, _ ->
            // Здесь можно добавить код для отмены вызова помощи
            // Например, отправка SMS с отменой вызова
        }
        builder.show()
    }






}


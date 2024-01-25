package com.example.project_android


import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.project_android.Accelerometer.AccelerometerListener
import com.example.project_android.Location.LocationHelper
import com.example.project_android.Message.SMS
import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.project_android.Timer.Timer
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private lateinit var accelerometerListener: AccelerometerListener
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsSender: SMS
    private lateinit var timer: Timer
    // Elements of layout //
    private lateinit var turnOn : Button
    private lateinit var turnOff : Button
    private lateinit var status : TextView

    private var lastAcceleration: FloatArray? = null
    private var lastLocation: Pair<Double, Double>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        turnOn = findViewById(R.id.turnOnButton)
        turnOff = findViewById(R.id.turnOffButton)
        status = findViewById(R.id.statusTextView)




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

            // Это бы перенести в SharedPreferences для настройки чувствительности из меню настроек(activity меню надо будет создать еще)
            val threshold = 5.0
            if (accelerationChange > threshold) {
                // Изменились данные акселерометра
                lastAcceleration = acceleration
                return true
            }
        }

        // Сохранение новых данных акселерометра(обновляются каждые 200 мс)
        lastAcceleration = acceleration
        return false
    }


    // Method Euclidian for calculating acceleration change
    private fun calculateAccelerationChange(acceleration1: FloatArray, acceleration2: FloatArray): Double {
        val deltaAcceleration = FloatArray(3)
        for (i in 0 until 3) {
            deltaAcceleration[i] = acceleration1[i] - acceleration2[i]
        }
        return sqrt((deltaAcceleration[0] * deltaAcceleration[0]
                + deltaAcceleration[1] * deltaAcceleration[1]
                + deltaAcceleration[2] * deltaAcceleration[2]).toDouble())
    }


    private fun checkLocation(latitude: Double, longitude: Double): Boolean {
        // Проверяем изменение координат
        if (lastLocation != null) {
            val deltaLatitude = abs(latitude - lastLocation!!.first)
            val deltaLongitude = abs(longitude - lastLocation!!.second)

            // Это бы перенести в SharedPreferences для настройки чувствительности из меню настроек(activity меню надо будет создать еще)
            val threshold = 0.0001

            if (deltaLatitude > threshold || deltaLongitude > threshold) {
                // Изменились данные GPS
                lastLocation = Pair(latitude, longitude)
                showFallDetectedDialog()
                return true
            }
        }

        // Сохраняем текущие значения координат
        lastLocation = Pair(latitude, longitude)
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


package com.example.project_android

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.project_android.Accelerometer.AccelerometerListener
import com.example.project_android.Location.LocationHelper
import com.example.project_android.MainActivity
import com.example.project_android.Message.SMS
import com.example.project_android.Timer.Timer
import kotlin.math.abs
import kotlin.math.sqrt

class BackgroundService : Service() {

    private lateinit var accelerometerListener: AccelerometerListener
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsSender: SMS
    private lateinit var timer: Timer

    private var shakeTimerHandler = Handler(Looper.getMainLooper())
    private var shakeTimerRunnable: Runnable? = null
    private var isTimerStarted: Boolean = false

    private var lastAcceleration: FloatArray? = null
    private var lastLocation: Pair<Double, Double>? = null


    companion object {
        private const val NOTIFICATION_ID = 1
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Вызывается при старте службы
        Log.d("MainActivity", "Кракен готовится")

        val notification = createNotification()  // Создайте уведомление, которое будет отображаться в статус-баре
        startForeground(NOTIFICATION_ID, notification)

        accelerometerListener = AccelerometerListener(this) { acceleration ->
            onAccelerationChanged(acceleration)
        }

        locationHelper = LocationHelper(this) { latitude, longitude ->
            if (checkLocation(latitude, longitude)) {

            }
        }

        smsSender = SMS(this)

        timer = Timer {
//            showFallDetectedDialog()
            smsSender.sendSMS("Началась тревога!")
        }

        accelerometerListener.register()


        // Обработка случая, когда разрешения не получены


        return START_STICKY // Служба будет перезапущена в случае ее аварийной остановки
    }

    override fun onDestroy() {
        super.onDestroy()
        // Вызывается при остановке службы
        accelerometerListener.unregister()
        locationHelper.stopLocationUpdates()
        timer.cancelTimer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }



    private fun onAccelerationChanged(acceleration: FloatArray) {
        if (checkAcceleration(acceleration)) {
            startShakeTimer()
        }
    }

    private fun startShakeTimer() {
        shakeTimerRunnable?.let { shakeTimerHandler.removeCallbacks(it) }

        shakeTimerRunnable = Runnable {
//            showFallDetectedDialog()
            isTimerStarted = false
            locationHelper.requestLocationUpdates()
            shakeTimerRunnable = null
        }

        shakeTimerHandler.postDelayed(shakeTimerRunnable!!, 3000)
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
                Log.d("Ac", "x: ${acceleration[0]}, y: ${acceleration[1]}, z: ${acceleration[2]}")
                return true
            }
        }

        // Сохранение новых данных акселерометра(обновляются каждые 200 мс)
        lastAcceleration = acceleration
        val accelerationString = "x: ${acceleration[0]}, y: ${acceleration[1]}, z: ${acceleration[2]}"


        return false
    }

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

        if (lastLocation != null) {
            val deltaLatitude = abs(latitude - lastLocation!!.first)
            val deltaLongitude = abs(longitude - lastLocation!!.second)

            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val gpsSensitivity: Double =
                (preferences.getInt("gps_sensitivity", 10) * 0.00001)

            if (deltaLatitude > gpsSensitivity || deltaLongitude > gpsSensitivity) {
                // Изменились данные GPS
                lastLocation = Pair(latitude, longitude)



                // For Testing
                Log.d("GPS", "Сработало GPS!")
                return false
            }
            Log.d("GPS", "No location")
            lastLocation = Pair(latitude, longitude)

            if(!isTimerStarted) {
                val timer = Timer {
                    smsSender.sendSMS("Test")
                }
                isTimerStarted = true
                timer.startTimer(5000)

            }

            return true
        }

        // Сохраняем текущие значения координат
        lastLocation = Pair(latitude, longitude)

        return false
    }


    private fun createNotification(): Notification {
        val channelId = "your_channel_id"
        val channelName = "Your Channel Name"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running in foreground")
            .setSmallIcon(R.drawable.ic_launcher_background)  // Укажите свою иконку уведомления

        return builder.build()
    }

//    private fun showFallDetectedDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Wykryt Wypadek")
//        builder.setMessage("Było wykryto wypadek. Wezwać pomoc?")
//        builder.setPositiveButton("Да") { _, _ ->
//            // Здесь можно добавить код для вызова помощи
//            // Например, отправка SMS с предупреждением о падении
//        }
//        builder.setNegativeButton("Нет") { _, _ ->
//            // Здесь можно добавить код для отмены вызова помощи
//            // Например, отправка SMS с отменой вызова
//        }
//        builder.show()
//    }
}
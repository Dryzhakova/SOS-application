package com.example.project_android

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.PreferenceManager
import com.example.project_android.Accelerometer.AccelerometerListener
import com.example.project_android.Contacts.ContactsDetails
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
    private var coordinateLatitude : String = ""
    private var coordinateLongtitude : String = ""


    private var timerInstance: Timer? = null
    private var mediaPlayer : MediaPlayer? = null
    private var isMediaPlayerStarted : Boolean = false
    private var isSecondMediaPlayerStarted : Boolean = false
    private lateinit var speechRecognizer: SpeechRecognizer

    private var SpeechTimeHandler = Handler(Looper.getMainLooper())
    private var SpeechTimerRunnable : Runnable? = null
    private var shakeTimerHandler = Handler(Looper.getMainLooper())
    private var shakeTimerRunnable: Runnable? = null
    private var isTimerStarted: Boolean = false

    private var lastAcceleration: FloatArray? = null
    private var lastLocation: Pair<Double, Double>? = null
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Вызывается при старте службы



        if (intent?.action == "TURN_OFF_ACTION") {
            turnOFF()
        }


        Log.d("MainActivity", "Кракен готовится")


        // start speech




        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val message = sharedPreferences.getString(SettingsActivity.PREFS_KEY_SELECTED_MESSAGE, "")
        ContactsDetails.message = message.toString()
        val number = sharedPreferences.getString(SettingsActivity.PREFS_KEY_SELECTED_CONTACT_NUMBER, "")
        ContactsDetails.number = number.toString()



        accelerometerListener = AccelerometerListener(this) { acceleration ->
            onAccelerationChanged(acceleration)
        }

        locationHelper = LocationHelper(this) { latitude, longitude ->
            if (checkLocation(latitude, longitude)) {
                locationHelper.stopLocationUpdates()
                isMediaPlayerStarted = false
                isSecondMediaPlayerStarted = false
            }
        }

        smsSender = SMS(this)

        timer = Timer {
//            showFallDetectedDialog()
            smsSender.sendSMS("", "")
        }

        accelerometerListener.register()




        val notification =
            createNotification()  // Создайте уведомление, которое будет отображаться в статус-баре
        startForeground(NOTIFICATION_ID, notification)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Handle speech recognition errors
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> Log.d("Speech", "No match found")
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Log.d("Speech", "Speech recognition timed out")
                    // Add handling for other error cases as needed
                    else -> Log.d("Speech", "Error: $error")
                }
            }


            override fun onResults(results: Bundle?) {

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    val spokenText = it[0].toLowerCase()
                    Log.d("Speech",  spokenText)

                    // Check if the spoken text matches predefined phrases
                    if (spokenText =="stop") {
                        Log.d("Speech","Выполнено!")
                        turnOFF()

                    } else if (spokenText == "help") {
                        smsSender.sendSMS(ContactsDetails.message, ContactsDetails.number)
                        turnOFF()
                        Log.d("SpeechHelp",  "Help is on the way!")
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial speech recognition results
                // Not used in this example but can be extended for continuous processing
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle additional recognition events (if needed)
            }
        })


            // Обработка случая, когда разрешения не получены


//        loadSelectedContact()

        return START_STICKY // Служба будет перезапущена в случае ее аварийной остановки
    }


    fun turnOFF(){
        synchronized(this) {
            locationHelper.stopLocationUpdates()
            timerInstance?.cancelTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Вызывается при остановке службы
        accelerometerListener.unregister()
        locationHelper.stopLocationUpdates()
        timer.cancelTimer()
        speechRecognizer.destroy()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun onAccelerationChanged(acceleration: FloatArray) {
        if (checkAcceleration(acceleration))
        {

            startShakeTimer()


                // Create a new MediaPlayer instance and start playing the specified MP3




        }
    }


    private fun startVoiceRecognition() {
        // Start the voice recognition process
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak 'stop' or 'help'")

        // Add a custom list of phrases to be recognized
        val phrases = arrayListOf("stop", "help")
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Optional: use offline recognition if available
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US") // Optional: specify language preference

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)

        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        intent.putExtra(RecognizerIntent.EXTRA_RESULTS, phrases.toTypedArray())

        speechRecognizer.startListening(intent)
    }
//

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


    private fun startSpeechTimer() {
        SpeechTimerRunnable?.let { SpeechTimeHandler.removeCallbacks(it) }

        SpeechTimerRunnable = Runnable {
//            showFallDetectedDialog()


            startVoiceRecognition()


            SpeechTimerRunnable = null
        }

        SpeechTimeHandler.postDelayed(SpeechTimerRunnable!!, 6500)
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
        val accelerationString =
            "x: ${acceleration[0]}, y: ${acceleration[1]}, z: ${acceleration[2]}"


        return false
    }

    private fun calculateAccelerationChange(
        acceleration1: FloatArray,
        acceleration2: FloatArray
    ): Double {
        val deltaAcceleration = FloatArray(3)
        for (i in 0 until 3) {
            deltaAcceleration[i] = acceleration1[i] - acceleration2[i]
        }

        val value = sqrt(
            (deltaAcceleration[0] * deltaAcceleration[0]
                    + deltaAcceleration[1] * deltaAcceleration[1]
                    + deltaAcceleration[2] * deltaAcceleration[2]).toDouble()
        )
        return value

    }

//    private fun SecondMediaPlayer(){
//        if(!isSecondMediaPlayerStarted) {
//
//            isSecondMediaPlayerStarted = true
//        }
//    }

    private fun MediaPlayer(){
        if(!isMediaPlayerStarted) {
            mediaPlayer = MediaPlayer.create(this, R.raw.impact_detected)
            mediaPlayer?.start()
            isMediaPlayerStarted = true
        }
    }

    private fun checkLocation(latitude: Double, longitude: Double): Boolean {



        if (lastLocation != null) {
            val deltaLatitude = abs(latitude - lastLocation!!.first)
            val deltaLongitude = abs(longitude - lastLocation!!.second)

            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val gpsSensitivity: Double =
                (preferences.getInt("gps_sensitivity", 10) * 0.00001)

            val count : Int = (preferences.getInt("set_timer", 10) * 1000)
            Log.d("Message", count.toString())

            if (deltaLatitude > gpsSensitivity || deltaLongitude > gpsSensitivity) {
                // Изменились данные GPS
                lastLocation = Pair(latitude, longitude)


                // For Testing
                Log.d("GPS", "Сработало GPS!")
                return false
            }
            Log.d("GPS", "No location")
            coordinateLatitude = latitude.toString()
            coordinateLongtitude = longitude.toString()
            Log.d("coordinateLatitude", coordinateLatitude)
            Log.d("coordinateLongtitude", coordinateLongtitude)
            lastLocation = Pair(latitude, longitude)

            ContactsDetails.message = ContactsDetails.message + "\nCoordinates: $coordinateLatitude,$coordinateLatitude"

            MediaPlayer()
            startSpeechTimer()


            if (!isTimerStarted) {
                val timer = Timer {
                    mediaPlayer = MediaPlayer.create(this, R.raw.entering_sms_mode)
                    mediaPlayer?.start()
                    smsSender.sendSMS(ContactsDetails.message, ContactsDetails.number)




                }
                isTimerStarted = true
                timer.startTimer(count.toLong())
                timerInstance = timer

                return true
            }

        }

        // Сохраняем текущие значения координат
            lastLocation = Pair(latitude, longitude)

            return false
        }


        private fun createNotification(): Notification {
            val channelId = "1"
            val channelName = "Name"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this, BackgroundService::class.java)
            notificationIntent.action = "TURN_OFF_ACTION" // Уникальный action для вашего PendingIntent
            val pendingIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Foreground Service")
                .setContentText("Service is running in foreground")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)// Укажите свою иконку уведомления

            return builder.build()
        }







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



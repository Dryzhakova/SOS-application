package com.example.project_android

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.*
import android.preference.PreferenceManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.project_android.Accelerometer.AccelerometerListener
import com.example.project_android.Contacts.ContactsDetails
import com.example.project_android.Location.LocationHelper
import com.example.project_android.Message.SMS
import com.example.project_android.Timer.Timer
import kotlin.math.abs
import kotlin.math.sqrt

class BackgroundService : Service() {
    private lateinit var accelerometerListener: AccelerometerListener
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsSender: SMS
    private lateinit var timer: Timer
    private var coordinateLatitude: String = ""
    private var coordinateLongitude: String = ""

    private var timerInstance: Timer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isMediaPlayerStarted: Boolean = false
    private lateinit var speechRecognizer: SpeechRecognizer

    private var SpeechTimeHandler = Handler(Looper.getMainLooper())
    private var SpeechTimerRunnable: Runnable? = null
    private var shakeTimerHandler = Handler(Looper.getMainLooper())
    private var shakeTimerRunnable: Runnable? = null
    private var isTimerStarted: Boolean = false
    private var isAccelerationDetected: Boolean = false

    private var lastAcceleration: FloatArray? = null
    private var lastLocation: Pair<Double, Double>? = null
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val TURN_OFF_ACTION = "TURN_OFF_ACTION"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == TURN_OFF_ACTION) {
            turnOFF()
        }

        Log.d("MainActivity", "Кракен готовится")

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
            }
        }

        smsSender = SMS(this)

        timer = Timer {
            smsSender.sendSMS("", "")
        }

        accelerometerListener.register()

        val notification = createNotification()
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
                    when (spokenText) {
                        "stop" -> {
                            Log.d("Speech", "Выполнено!")
                            playTimerStoppedSound()
                            Handler(Looper.getMainLooper()).postDelayed({
                                turnOFF()
                            }, 1000)
                        }
                        "help" -> {
                            playEnteringSOSModeSound()
                            smsSender.sendSMS(ContactsDetails.message, ContactsDetails.number)
                            Handler(Looper.getMainLooper()).postDelayed({
                                turnOFF()
                            }, 3000)
                            Log.d("SpeechHelp", "Help is on the way!")
                        }
                        else -> {
                            // Handle other cases if needed
                        }
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

        return START_STICKY
    }

    private fun turnOFF() {
        synchronized(this) {
            locationHelper.stopLocationUpdates()
            timerInstance?.cancelTimer()
            stopSelf()  // Add this line to stop the service
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        accelerometerListener.unregister()
        locationHelper.stopLocationUpdates()
        timer.cancelTimer()
        speechRecognizer.destroy()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun playImpactSound() {
        // Release the previous MediaPlayer instance
        mediaPlayer?.release()

        // Create a new MediaPlayer instance
        mediaPlayer = MediaPlayer.create(this, R.raw.impact_detected)
        mediaPlayer?.start()
    }

    private fun playTimerStoppedSound() {
        // Release the previous MediaPlayer instance
        mediaPlayer?.release()

        // Create a new MediaPlayer instance
        mediaPlayer = MediaPlayer.create(this, R.raw.timer_stopped)
        mediaPlayer?.start()
    }

    private fun playEnteringSOSModeSound() {
        // Release the previous MediaPlayer instance
        mediaPlayer?.release()

        // Create a new MediaPlayer instance
        mediaPlayer = MediaPlayer.create(this, R.raw.entering_sos_mode)
        mediaPlayer?.start()
    }

    private fun onAccelerationChanged(acceleration: FloatArray) {
        if (checkAcceleration(acceleration)) {
            startShakeTimer()
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

    private fun startShakeTimer() {
        shakeTimerRunnable?.let { shakeTimerHandler.removeCallbacks(it) }

        shakeTimerRunnable = Runnable {
            locationHelper.requestLocationUpdates()
            shakeTimerRunnable = null
        }

        shakeTimerHandler.postDelayed(shakeTimerRunnable!!, 3000)
    }

    private fun startSpeechTimer() {

        SpeechTimerRunnable?.let { SpeechTimeHandler.removeCallbacks(it) }

        SpeechTimerRunnable = Runnable {
            startVoiceRecognition()
            SpeechTimerRunnable = null
        }

        SpeechTimeHandler.postDelayed(SpeechTimerRunnable!!, 6500)
    }

    private fun checkAcceleration(acceleration: FloatArray): Boolean {
        if (lastAcceleration != null && !isAccelerationDetected) {
            val accelerationChange = calculateAccelerationChange(acceleration, lastAcceleration!!)

            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val accelerometerSensitivity: Int = preferences.getInt("accelerometer_sensitivity", 5)

            if (accelerationChange > accelerometerSensitivity) {
                lastAcceleration = acceleration
                isAccelerationDetected = true
                return true
            }
        }

        lastAcceleration = acceleration
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

    private fun checkLocation(latitude: Double, longitude: Double): Boolean {
        if (lastLocation != null) {
            val deltaLatitude = abs(latitude - lastLocation!!.first)
            val deltaLongitude = abs(longitude - lastLocation!!.second)

            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val gpsSensitivity: Double =
                (preferences.getInt("gps_sensitivity", 10) * 0.00001)

            val count : Int = ((preferences.getInt("set_timer", 10) + 5)* 1000)
            Log.d("Message", count.toString())

            if (deltaLatitude > gpsSensitivity || deltaLongitude > gpsSensitivity) {
                // Изменились данные GPS
                lastLocation = Pair(latitude, longitude)
                isAccelerationDetected = false


                // For Testing
                Log.d("GPS", "Сработало GPS!")
                return false
            }
            Log.d("GPS", "No location")
            coordinateLatitude = latitude.toString()
            coordinateLongitude = longitude.toString()
            Log.d("coordinateLatitude", coordinateLatitude)
            Log.d("coordinateLongtitude", coordinateLongitude)
            lastLocation = Pair(latitude, longitude)

            ContactsDetails.message = ContactsDetails.message + "\nCoordinates: $coordinateLatitude,$coordinateLongitude"

            playImpactSound()
            startSpeechTimer()


            if (!isTimerStarted) {
                val timer = Timer {
                    playEnteringSOSModeSound()
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, BackgroundService::class.java)
        notificationIntent.action = TURN_OFF_ACTION
        val pendingIntent =
            PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running in foreground")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)

        return builder.build()
    }
}

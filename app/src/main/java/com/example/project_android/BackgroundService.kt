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
    private var isListening: Boolean = false
    private var isSignalingMode: Boolean = false
    private var isStopped: Boolean = false

    private var lastAcceleration: FloatArray? = null
    private var lastLocation: Pair<Double, Double>? = null
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val TURN_OFF_ACTION = "TURN_OFF_ACTION"
    }

    // Location update interval
    private val LOCATION_UPDATE_INTERVAL = 10 * 60 * 1000 // 10 minutes

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
                stopLocationUpdates()
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
                // Handling speech recognition errors
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> Log.d("Speech", "No match found")
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Log.d("Speech", "Speech recognition timed out")
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
                            if (!isStopped) {
                                Log.d("Speech", "Выполнено!")
                                turnOFF()
                            }
                        }
                        "help" -> {
                            if (!isStopped){
                                playEnteringSOSModeSound()
                                smsSender.sendSMS(ContactsDetails.message, ContactsDetails.number)
                                stopLocationUpdates()
                                timerInstance?.cancelTimer()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isSignalingMode = true
                                    startSignalingMode()
                                }, 3000)
                                Log.d("SpeechHelp", "Help is on the way!")
                            }
                        }
                        else -> {
                            // Handle other cases
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial speech recognition results
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle additional recognition events
            }
        })

        return START_STICKY
    }

    private fun turnOFF() {
        if (!isStopped) {
            isSignalingMode = false
            isStopped = true
            stopLocationUpdates()
            timerInstance?.cancelTimer()
            playTimerStoppedSound()
            synchronized(this) {
                Handler(Looper.getMainLooper()).postDelayed({
                    stopSelf()
                }, 1700)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        accelerometerListener.unregister()
        stopLocationUpdates()
        timer.cancelTimer()
        stopSpeechRecognition()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Release MediaPlayer resources immediately after use
    private fun playSound(resourceId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resourceId)
        mediaPlayer?.start()
    }

    private fun playImpactSound() {
        playSound(R.raw.impact_detected)
    }

    private fun playTimerStoppedSound() {
        playSound(R.raw.disabling_safety_mode)
    }

    private fun playEnteringSOSModeSound() {
        playSound(R.raw.entering_sos_mode)
    }

    private fun onAccelerationChanged(acceleration: FloatArray) {
        if (checkAcceleration(acceleration)) {
            startShakeTimer()
        }
    }

    private fun startSpeechRecognition() {
        if (!isListening) {
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
            isListening = true
        }
    }


    private fun stopSpeechRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    private fun startLocationUpdates() {
        locationHelper.requestLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationHelper.stopLocationUpdates()
    }

    private fun startShakeTimer() {
        shakeTimerRunnable?.let { shakeTimerHandler.removeCallbacks(it) }

        shakeTimerRunnable = Runnable {
            startLocationUpdates()
            shakeTimerRunnable = null
        }

        shakeTimerHandler.postDelayed(shakeTimerRunnable!!, 5000)
    }

    private fun startSpeechTimer() {
        SpeechTimerRunnable?.let { SpeechTimeHandler.removeCallbacks(it) }

        SpeechTimerRunnable = Runnable {
            startSpeechRecognition()
            SpeechTimerRunnable = null
        }

        SpeechTimeHandler.postDelayed(SpeechTimerRunnable!!, 6000)
    }

    private fun checkAcceleration(acceleration: FloatArray): Boolean {
        if (!isSignalingMode){
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
        if (!isSignalingMode) {
            if (lastLocation != null && isAccelerationDetected) {
                val deltaLatitude = abs(latitude - lastLocation!!.first)
                val deltaLongitude = abs(longitude - lastLocation!!.second)

                val preferences: SharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this)
                val gpsSensitivity: Double =
                    (preferences.getInt("gps_sensitivity", 10) * 0.00001)

                val count: Int = ((preferences.getInt("set_timer", 10) + 6) * 1000)
                Log.d("Message", count.toString())

                if (deltaLatitude > gpsSensitivity || deltaLongitude > gpsSensitivity) {
                    // Changed GPS data
                    lastLocation = Pair(latitude, longitude)
                    isAccelerationDetected = false

                    // Stop location updates when outside the sensitivity
                    stopLocationUpdates()

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

                ContactsDetails.message =
                    ContactsDetails.message + "\nCoordinates: $coordinateLatitude,$coordinateLongitude"

                playImpactSound()
                startSpeechTimer()

                if (!isTimerStarted) {
                    val timer = Timer {
                        playEnteringSOSModeSound()
                        smsSender.sendSMS(ContactsDetails.message, ContactsDetails.number)
                        stopLocationUpdates()
                        timerInstance?.cancelTimer()
                        Handler(Looper.getMainLooper()).postDelayed({
                            isSignalingMode = true
                            startSignalingMode()
                        }, 3000)
                    }
                    isTimerStarted = true
                    timer.startTimer(count.toLong())
                    timerInstance = timer

                    return true
                }
            }

            // Save current coordinates
            lastLocation = Pair(latitude, longitude)

            return false
        }
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
        val turnOffIntent = PendingIntent.getService(
            this,
            0,
            notificationIntent.setAction(TURN_OFF_ACTION),
            PendingIntent.FLAG_IMMUTABLE
        )

        val turnOffAction = NotificationCompat.Action.Builder(
            R.drawable.ic_turn_off,
            "Stop",
            turnOffIntent
        ).build()

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running in foreground")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .addAction(turnOffAction)

        return builder.build()
    }


    private fun startSignalingMode() {
        if (isSignalingMode){
            // Check if the MediaPlayer is not already started
            if (!isMediaPlayerStarted) {
                val signalingSoundResourceId = R.raw.alarm

                // Set the signaling interval in milliseconds
                val signalingInterval = 12000 // 1 minute

                // Start playing the signaling sound at intervals
                startMediaPlayerWithInterval(signalingSoundResourceId, signalingInterval.toLong())
            }
        }
    }

    private fun startMediaPlayerWithInterval(resourceId: Int, interval: Long) {
        if (isSignalingMode) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resourceId)

            val alarmHandler = Handler(Looper.getMainLooper())

            val alarmRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.start()
                }
            }

            mediaPlayer?.setOnCompletionListener {
                // Release resources when playback completes
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, resourceId)

                // Schedule the next play at the specified interval
                Handler(Looper.getMainLooper()).postDelayed({
                    startSignalingMode()
                }, interval)
            }

            // Start the initial playback
            alarmHandler.post(alarmRunnable)
        }
    }

}

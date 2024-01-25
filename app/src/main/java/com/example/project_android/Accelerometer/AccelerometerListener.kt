package com.example.project_android.Accelerometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class AccelerometerListener(context: Context, private val onAccelerationChanged: (FloatArray) -> Unit) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun register(){
        accelerometer?.let{
            sensorManager.registerListener(this,it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister(){
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val acceleration = event.values.copyOf() // Создаем копию значений
            onAccelerationChanged(acceleration)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

}
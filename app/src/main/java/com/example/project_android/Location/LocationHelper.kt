package com.example.project_android.Location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class LocationHelper(context: Context, private val onLocationChanged: (Double, Double) -> Unit) {

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocationChanged(location.latitude, location.longitude)
            stopLocationUpdates()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    fun requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } catch (e: SecurityException) {
            // Обработка ошибки
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: SecurityException) {
            // Обработка ошибки
        }
    }
}

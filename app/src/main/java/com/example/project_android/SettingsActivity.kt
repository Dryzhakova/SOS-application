package com.example.project_android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.convertTo
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference

class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)


            // Change value of Seekbar if user enter value
            val editTextPref = findPreference<EditTextPreference>("accelerometer_sensitivity_manual")
            val seekBarPref = findPreference<SeekBarPreference>("accelerometer_sensitivity")

            editTextPref?.setOnPreferenceChangeListener { _, newValue ->
                // Обновление значения SeekBarPreference при изменении EditTextPreference
                seekBarPref?.value = (newValue as String).toInt()
                seekBarPref?.setValue((newValue as String).toInt())
                true

            }

            val accelerometerPreference = findPreference<SeekBarPreference>("accelerometer_sensitivity")
            val gpsPreference = findPreference<SeekBarPreference>("gps_sensitivity")

            accelerometerPreference?.setOnPreferenceChangeListener { _, newValue ->
                // Обработка изменения настроек акселерометра
                val value = newValue as Int
                Log.d("Accelerometer", value.toString())
                println("Accelerometer sensitivity changed to: $value")
                true
            }

            gpsPreference?.setOnPreferenceChangeListener { _, newValue ->
                // Обработка изменения настроек GPS
                val value = newValue as Int
                Log.d("GPS", value.toString())
                println("GPS sensitivity changed to: $value")
                true
            }



        }
    }
}

package com.example.project_android.Message

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.telephony.SmsManager
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.project_android.Contacts.ContactsDetails
import com.example.project_android.R

class SMS(private val context: Context) {

    fun sendSMS(message : String, number : String) {
        try {
            Log.d("Activated!", message)
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
        } catch (e: Exception) {
            // Обработка ошибки
        }
    }
}
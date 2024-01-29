package com.example.project_android.Message

import android.content.Context
import android.content.SharedPreferences
import android.telephony.SmsManager
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.project_android.Contacts.ContactsDetails

class SMS(private val context: Context) {


    // Пока что вырубил данный пункт и вывод сделал в LogCat(пока что вроде не работает нормально)
    fun sendSMS(message : String, number : String) {
        try {


            Log.d("Activated!", message)

            val smsManager = SmsManager.getDefault()
             // Укажите номер телефона получателя
            smsManager.sendTextMessage(number, null, message, null, null)
        } catch (e: Exception) {
            // Обработка ошибки
        }
    }
}
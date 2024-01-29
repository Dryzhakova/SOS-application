package com.example.project_android.Message

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

class SMS(private val context: Context) {


    // Пока что вырубил данный пункт и вывод сделал в LogCat(пока что вроде не работает нормально)
    fun sendSMS(message: String) {
        try {
            Log.d("Activated!", message)

            val smsManager = SmsManager.getDefault()
            val phoneNumber = "+48577830955" // Укажите номер телефона получателя
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            // Обработка ошибки
        }
    }
}
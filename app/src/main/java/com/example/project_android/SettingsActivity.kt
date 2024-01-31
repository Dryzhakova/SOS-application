package com.example.project_android

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.example.project_android.Contacts.ContactsDetails

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PICK_CONTACT_REQUEST = 1
        const val PREFS_KEY_SELECTED_CONTACT_NAME = "selected_contact_name"
        const val PREFS_KEY_SELECTED_CONTACT_NUMBER = "selected_contact_number"
        const val PREFS_KEY_SELECTED_MESSAGE = "selected_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var sharedPreferences: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            val selectContactButton = findPreference<Preference>("contacts_button")

            selectContactButton?.setOnPreferenceClickListener {
                pickContact()
                true
            }

            val setTimer = findPreference<SeekBarPreference>("set_timer")
            setTimer?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as Int
                Log.d("Timer", value.toString())
                true
            }

            val accelerometerPreference = findPreference<SeekBarPreference>("accelerometer_sensitivity")
            val gpsPreference = findPreference<SeekBarPreference>("gps_sensitivity")
            val messageButton = findPreference<EditTextPreference>("message_text")

            accelerometerPreference?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as Int
                Log.d("Accelerometer", value.toString())
                true
            }

            gpsPreference?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as Int
                Log.d("GPS", value.toString())
                true
            }

            messageButton?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as String
                sharedPreferences.edit().apply {
                    putString(PREFS_KEY_SELECTED_MESSAGE, value)
                    apply()
                }
                ContactsDetails.message = value
                true
            }
        }

        private fun pickContact() {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST)
        }

        private fun saveSelectedContact(name: String, number: String) {
            sharedPreferences.edit().apply {
                putString(PREFS_KEY_SELECTED_CONTACT_NAME, name)
                putString(PREFS_KEY_SELECTED_CONTACT_NUMBER, number)
                apply()
            }
        }

        private fun loadSelectedContact() {
            val selectedContactName =
                sharedPreferences.getString(PREFS_KEY_SELECTED_CONTACT_NAME, "")
            ContactsDetails.number =
                sharedPreferences.getString(PREFS_KEY_SELECTED_CONTACT_NUMBER, "").toString()
            ContactsDetails.message =
                sharedPreferences.getString(PREFS_KEY_SELECTED_MESSAGE, "").toString()
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
                handleSelectedContact(data?.data)
            }
        }

        private fun handleSelectedContact(contactUri: android.net.Uri?) {
            val cursor = requireContext().contentResolver.query(contactUri!!, null, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                    if (nameIndex != -1) {
                        val contactName = it.getString(nameIndex)

                        val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                        if (idIndex != -1) {
                            val contactId = it.getString(idIndex)
                            val phoneCursor = requireContext().contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                                        ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL",
                                arrayOf(contactId),
                                null
                            )

                            phoneCursor?.use { phoneCursor  ->
                                if (phoneCursor.moveToFirst()) {
                                    val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                                    if (numberIndex != -1) {
                                        val contactNumber = phoneCursor.getString(numberIndex)

                                        Log.d("MainActivity", "Contact Number: $contactNumber")
                                        ContactsDetails.number = contactNumber
                                        saveSelectedContact(contactName, contactNumber)
                                    } else {
                                        Log.d("MainActivity", "Phone number index not found.")
                                    }
                                } else {
                                    Log.d("MainActivity", "No phone number found for the contact.")
                                }
                            }
                        } else {
                            Log.d("MainActivity", "_ID index not found.")
                        }
                    }
                }
            }
        }
    }
}

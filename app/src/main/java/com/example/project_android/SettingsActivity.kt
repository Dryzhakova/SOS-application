package com.example.project_android

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.convertTo
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

            // Contacts

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            val selectContactButton = findPreference<Preference>("contacts_button")


            selectContactButton?.setOnPreferenceClickListener{ pickContact()
                true}


            // Change value of Seekbar if user enter value
            val editTextPref = findPreference<EditTextPreference>("accelerometer_sensitivity_manual")
            val seekBarPref = findPreference<SeekBarPreference>("accelerometer_sensitivity")
            val message_Button = findPreference<EditTextPreference>("message_text")

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

            message_Button?.setOnPreferenceChangeListener{_, newValue ->


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
            // Save the selected contact name and number to SharedPreferences
            sharedPreferences.edit().apply {
                putString(PREFS_KEY_SELECTED_CONTACT_NAME, name)
                putString(PREFS_KEY_SELECTED_CONTACT_NUMBER, number)
                apply()
            }

            // Display the saved contact in the TextViews

        }

        private fun loadSelectedContact() {
        // Load the selected contact name and number from SharedPreferences
        val selectedContactName =
            sharedPreferences.getString(SettingsActivity.PREFS_KEY_SELECTED_CONTACT_NAME, "")
        ContactsDetails.number =
            sharedPreferences.getString(SettingsActivity.PREFS_KEY_SELECTED_CONTACT_NUMBER, "")
                .toString()
        ContactsDetails.message =
            sharedPreferences.getString(SettingsActivity.PREFS_KEY_SELECTED_MESSAGE, "").toString()
        // Display the selected contact name and number in the TextViews

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
                    // Retrieve the contact name column index
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                    if (nameIndex != -1) {
                        // Retrieve the contact name from the cursor
                        val contactName = it.getString(nameIndex)

                        // Log the retrieved name
                        Log.d("MainActivity", "Contact Name: $contactName")

                        // Check if the contact has a valid _ID
                        val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                        if (idIndex != -1) {
                            // Query phone numbers associated with the contact
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
                                    // Retrieve the contact phone number column index
                                    val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                                    if (numberIndex != -1) {
                                        // Retrieve the contact phone number from the phoneCursor
                                        val contactNumber = phoneCursor.getString(numberIndex)

                                        // Log the retrieved phone number
                                        Log.d("MainActivity", "Contact Number: $contactNumber")
                                        ContactsDetails.number = contactNumber
                                        // Save the selected contact to SharedPreferences
                                        saveSelectedContact(contactName, contactNumber)
                                    } else {
                                        // Handle the case where the phone number index is not found
                                        Log.d("MainActivity", "Phone number index not found.")
                                    }
                                } else {
                                    // Handle the case where no phone number is found
                                    Log.d("MainActivity", "No phone number found for the contact.")
                                }
                            }
                        } else {
                            // Handle the case where the _ID index is not found
                            Log.d("MainActivity", "_ID index not found.")
                        }
                    }
                }
            }
        }
    }
}

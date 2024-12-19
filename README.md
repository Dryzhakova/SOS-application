# SOS Mobile Application

## Description
The **SOS application** is a mobile app developed in **Kotlin** for the **Android platform**. The program monitors user movement and location and, in the event of a potentially dangerous situation such as sudden acceleration (e.g., a fall), it sends **SMS notifications** to a predefined contact.

---

## User Guide

### To use the application, follow these steps:

1. **Device Requirements:**
   - Ensure the device has sensors like an **accelerometer** and **GPS**.
   - Minimum Android version: **7.0 Nougat**.

2. **Setup and Permissions:**
   - Launch the application and enable monitoring using the **"Toggle" button**.
   - Ensure the required permissions are granted, such as access to **location** and **SMS**.

3. **Configuration:**
   - Customize preferences in the app settings, including:
     - **Message to send**
     - **Contact number**
     - **Accelerometer sensitivity**

4. **Emergency Response:**
   - In case of an emergency, the program automatically sends an **SMS** with the predefined message to the selected contact.

---

## Features

### Accelerometer Monitoring
- Monitors data from the **accelerometer** to detect sudden accelerations.
- Sensitivity can be adjusted in the settings.

### Location Tracking
- Tracks user **location** and reacts to changes.
- **GPS sensitivity** can be customized in the settings.

### SMS and Contact Integration
- Users can define a **message** and **contact number** in the settings.
- In emergencies, the program automatically sends the predefined **message** to the chosen contact.

### Timer Functionality
- Utilizes a **timer** for delayed actions, such as playing sounds or sending messages.

### Audio Notifications
- Plays **audible alerts** in emergency situations to notify the user of the detected event.

---

## Additional Features

### Voice Command Recognition
- Includes **voice command functionality** allowing users to issue commands like **"stop"** or **"help"**.
- Voice commands enable quick deactivation of monitoring or sending help messages.

### User Interaction
- Provides **status bar notifications** to inform users about the program's current state, such as when monitoring is active.

---

## Privacy and Security
- All user data, such as **messages** and **contact numbers**, is stored **locally** on the device.
- No data is shared with **external applications** or **servers**.
- **Location data** is used strictly for monitoring and emergency response purposes.

---

## How to Contribute
1. **Fork the repository.**
2. **Submit pull requests** for feature improvements or bug fixes.
3. **Report issues** or suggest features via the issue tracker.



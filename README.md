An Android safety application developed in Kotlin harnessing various Android
services such as geolocation, accelerometer, notifications, and messaging.
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/903f387c-5c2d-4fc1-af53-8ca876458d93.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/903f387c-5c2d-4fc1-af53-8ca876458d93.png" width="400" height="900" />\
<br />
<br />
On startup the application will request access to all required services:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/5faa3b37-6bd4-401a-a873-85e1866b370b.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/5faa3b37-6bd4-401a-a873-85e1866b370b.png" width="400" height="900" />\
<br />
<br />
In the settings user can adjust the sensetivity of both accelerometer and gps, and set how long should be the delay between the detection of impact and activation of the SOS mode:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/9611ec41-6fed-4795-812f-f50aed25b7f5.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/9611ec41-6fed-4795-812f-f50aed25b7f5.png" width="400" height="900" />\
<br />
<br />
They can also select a contact from their contact list to witch they want to send an sms with their status and location:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/95e1b323-3915-459d-854c-648d8a83c5f3.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/95e1b323-3915-459d-854c-648d8a83c5f3.png" width="400" height="900" />\
<br />
<br />
The message that is sent to the contact can also be edited:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/0bb6889d-988d-4dd5-ba89-abb61c3a3975.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/0bb6889d-988d-4dd5-ba89-abb61c3a3975.png" width="400" height="900" />\
<br />
<br />
After innitial setup the application monitoring mode can be toggled:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/4e3e7377-d585-4f3f-931a-273794d1a85b.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/4e3e7377-d585-4f3f-931a-273794d1a85b.png" width="400" height="900" />\
<br />
<br />
This starts a foreground service, that monitors sudden changes in acceleration:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/acb986ac-d4cc-4eca-80ed-a6a4673bdb30.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/acb986ac-d4cc-4eca-80ed-a6a4673bdb30.png" width="400" height="900" />\
<br />
<br />
If a serious change in acceleration is detected, the gps is turned on to record the current location and detect any further movement:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/cbbeed0e-e90b-4c03-89f6-948fecd71777.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/cbbeed0e-e90b-4c03-89f6-948fecd71777.png" width="400" height="100" />\
<br />
<br />
If no further movement is detected the application will notify the user that an impact was detected. The user can then stop the timer manually or via a voice command, by saying "Stop":
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/f8e60318-90a7-43a2-9e7a-f12f5d4e9ce6.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/f8e60318-90a7-43a2-9e7a-f12f5d4e9ce6.png" width="400" height="120" />\
<br />
<br />
If the user does not stop the countdown, the application will enter the SOS mode. It will send an sms to the selected contact with user's message and location and start signaling:
<br />
<br />
<img src="https://github.com/Hererra/Project_Android_2024/assets/99400189/9d15df13-8dd9-41fc-9ea4-66a4c17f0d76.png" data-canonical-src="https://github.com/Hererra/Project_Android_2024/assets/99400189/9d15df13-8dd9-41fc-9ea4-66a4c17f0d76.png" width="400" height="900" />

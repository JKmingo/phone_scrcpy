# phone_scrcpy

- This application is an Android port of the desktop application [**Scrcpy**](https://github.com/Genymobile/scrcpy).

- This application mirrors display and touch controls from a target Android device to the scrcpy-android device.

- scrcpy-android uses **ADB-Connect** interface to connect to the Android device to be mirrored.

## Download

[scrcpy-release-v1.2.apk](https://gitlab.com/las2mile/scrcpy-android/raw/master/release/scrcpy-release.apk)

## Features

- **Mirror over Wi-Fi**: Connect to the target device on the same network using ADB over the network.
- **Mirror over USB**: Connect to the target device via a USB cable, and use ADB for mirroring.

## Instructions to Use

### 1. Mirror Over Wi-Fi

- Ensure both devices are on the same local network.
- Enable **ADB-Connect/ADB-Wireless/ADB over network** on the target device.
- Open the scrcpy app and enter the IP address of the device to be mirrored.
- Select display parameters and bitrate from the drop-down menu (1280x720 and 2Mbps works best).
- Set the **Navbar** switch if the device to be mirrored has only hardware navigation buttons.
- Hit the **Start** button.
- Accept and trust (check "Always allow from this computer") the ADB connection prompt on the target device (some custom ROMs may not display this prompt).
- That's all! You should now see the screen of the target Android device.

### 2. Mirror Over USB

- Connect the target device to the scrcpy-android device using a USB cable.
- Ensure **USB Debugging** is enabled on the target device (under Developer Options).
- Open the scrcpy app and select **USB** as the connection type.
- Choose display parameters and bitrate from the drop-down menu (1280x720 and 2Mbps works best).
- Tap the **Start** button.
- Accept and trust (check "Always allow from this computer") the ADB connection prompt on the target device.
- Once connected, the target device's screen will be mirrored.

## Tips and Controls

- To wake up the device, **double-tap anywhere on the screen**.
- To put the device to sleep, **close the proximity sensor and double-tap anywhere on the screen**.
- To bring back the local Android system navbar while mirroring the remote device, **swipe up from the bottom edge of the screen**.

## Building with Gradle

Run the following command to build the project:

```bash
./gradlew assembleDebug

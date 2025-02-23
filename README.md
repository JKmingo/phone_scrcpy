[切换到中文](./README_zh.md)

# phone_scrcpy

- This application is an Android port of the desktop application [**Scrcpy**](https://github.com/Genymobile/scrcpy).
- The application mirrors the display and touch controls of a target Android device to the device running phone_scrcpy.
- phone_scrcpy uses **ADB-Connect** or **Wireless ADB** to connect to the target Android device for mirroring.

## Features

- **Wi-Fi Mirroring**: Connect to the target device over the same network using ADB.
- **USB Mirroring**: Connect to the target device via a USB cable and use ADB for mirroring.

## Usage Instructions

### 1. Wi-Fi Mirroring

1. Ensure both devices are on the same local network.
2. Enable **adb tcpip 5555** on the target device.
3. Open the phone_scrcpy app and enter the IP:5555 address of the device to be mirrored.
4. Select display parameters and bitrate from the dropdown menu (1280x720 resolution and 2Mbps bitrate are recommended).
5. Click the **Start** button.
6. Accept and trust the ADB connection prompt on the target device (check "Always allow from this computer"). Some custom ROMs may not display this prompt.
7. That's it! You should now see the screen of the target Android device.

### 2. USB Mirroring

1. Connect the target device to the phone_scrcpy device using a USB cable (OTG cable).
2. Ensure **USB Debugging** is enabled on the target device (under Developer Options).
3. Open the phone_scrcpy app and select **USB** as the connection type.
4. Choose display parameters and bitrate from the dropdown menu (1280x720 resolution and 2Mbps bitrate are recommended).
5. Tap the **Start** button.
6. Accept and trust the ADB connection prompt on the target device (check "Always allow from this computer").
7. Once connected, the target device's screen will be mirrored.

## Tips and Controls

- While mirroring the remote device, **swipe up from the bottom edge of the screen** to bring up the local Android system's navigation bar.

## Building the Project with Gradle

Run the following command to build the project:

```bash
./gradlew assembleDebug
```
## Support and Donations

If this project has been helpful to you, feel free to support me:

### alipay
![支付宝捐款](app/src/main/res/drawable/alipay.jpg)

### wechat
![微信支付](app/src/main/res/drawable/wechat.png)

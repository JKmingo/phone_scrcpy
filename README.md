# phone_scrcpy

- 本应用是桌面应用 [**Scrcpy**](https://github.com/Genymobile/scrcpy) 的 Android 移植版本。
- 该应用将目标 Android 设备的显示和触摸控制镜像到运行 scrcpy-android 的设备上。
- phone_scrcpy 使用 **ADB-Connect** 或 **无线adb** 连接到需要镜像的 Android 设备。

## 功能特性

- **Wi-Fi 镜像**：通过同一网络下的 ADB 连接目标设备。
- **USB 镜像**：通过 USB 线连接目标设备，并使用 ADB 进行镜像。

## 使用说明

### 1. Wi-Fi 镜像

1. 确保两台设备在同一局域网内。
2. 在目标设备上启用 **adb tcpip 5555**。
3. 打开 phone_scrcpy 应用，输入需要镜像设备的 IP:5555 地址。
4. 从下拉菜单中选择显示参数和比特率等（推荐 1280x720 分辨率和 2Mbps 比特率）。
6. 点击 **开始** 按钮。
7. 在目标设备上接受并信任 ADB 连接提示（勾选“始终允许此计算机连接”），某些自定义 ROM 可能不会显示此提示。
8. 完成！您现在应该可以看到目标 Android 设备的屏幕。

### 2. USB 镜像

1. 使用 USB 线（OTG线）将目标设备连接到运行 phone_scrcpy 的设备。
2. 确保目标设备已启用 **USB 调试**（在开发者选项中）。
3. 打开 scrcpy 应用，选择 **USB** 作为连接类型。
4. 从下拉菜单中选择显示参数和比特率等（推荐 1280x720 分辨率和 2Mbps 比特率）。
5. 点击 **开始** 按钮。
6. 在目标设备上接受并信任 ADB 连接提示（勾选“始终允许此计算机连接”）。
7. 连接成功后，目标设备的屏幕将被镜像。

## 操作提示

- 在镜像远程设备时，**从屏幕底部边缘向上滑动** 以唤出本地 Android 系统的导航栏。

## 使用 Gradle 构建项目

运行以下命令构建项目：

```bash
./gradlew assembleDebug
```

## 支持与捐款

如果这个项目对您有帮助，欢迎支持我：

### 支付宝
![支付宝捐款](app/src/main/res/drawable/res/alipay.jpg)

### 微信支付
![微信支付](app/src/main/res/drawable/res/wechat.png)

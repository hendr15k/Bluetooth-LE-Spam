# Bluetooth LE Spam [![Discord](https://img.shields.io/discord/1170266776731406386?label=Discord&link=https://discord.gg/x4e4Gma585)](https://discord.gg/x4e4Gma585) [![Build](https://github.com/hendr15k/Bluetooth-LE-Spam/actions/workflows/build.yml/badge.svg)](https://github.com/hendr15k/Bluetooth-LE-Spam/actions/workflows/build.yml)

[![Download](https://img.shields.io/github/v/release/hendr15k/Bluetooth-LE-Spam?label=Download&logo=github)](https://github.com/hendr15k/Bluetooth-LE-Spam/releases/latest)

This project uses the built-in Bluetooth Low Energy (BLE) functionality built into Android devices
to create Phantom Bluetooth Device Advertisements, similar to what is known, for instance, in the case of the Flipper Zero.
While other apps are available that provide similar functionality, the objective of this app is to enhance convenience and user-friendliness in the process.

#### [For Support, Join the Discord Server](https://discord.gg/x4e4Gma585)

> [!NOTE]
> Contributions from anyone are appreciated.

## Requirements

<details>
<summary>Details</summary>

- Android 8.0 (API level 26) or later

  > Tip: If you don't know your API level, visit the [SDK Platform release notes](https://developer.android.com/tools/releases/platforms).
  > You can also view your Android version in the Settings app under *About phone*.

- The app can not run on iOS or on a PC (including emulators/VMs).

</details>

## Functionality

This app sends BLE advertisements that mimic the usage of various services.
This can cause nearby devices that listen to such advertisements to show unwanted popup dialogs.

| Target | Advertisements |
| --- | --- |
| Apple | Continuity popups (new AirTag, new device, "not your device"), various action modals |
| Windows | [Microsoft Swift Pair](https://learn.microsoft.com/en-us/windows-hardware/design/component-guidelines/bluetooth-swift-pair) |
| Samsung | Easy Setup (Galaxy Watch and Galaxy Buds) |
| Android | [Google Fast Pair](https://developers.google.com/nearby/fast-pair/landing-page) — patched on modern devices |
| Lovespouse | Play / Stop (adult toys) |
| HID | Bluetooth keyboard advertisements |

You can send BLE advertisements for all services at once ("kitchen sink") or for selected targets only.

<details>
<summary>Apple iOS 17 Crash (patched)</summary>

With some modifications to the advertised BLE package, it is possible to trigger a reboot in various iOS 17 devices.
This causes the targeted iPhone to temporarily freeze for a few minutes before automatically restarting.

This is reported to be partially patched in iOS 17.2, and fully patched in iOS 18.

> [!NOTE]
> To achieve optimal results, it is recommended to set the advertising interval to a value between 20 and 100 milliseconds.
> Additionally, locking and unlocking the targeted iPhone can aid in the process.

</details>

<details>
<summary>Lovespouse (Adult Toys)</summary>

With these BLE advertisements, it is possible to enable and disable various adult toys that support the Lovespouse app.
Additionally, the "Denial of Pleasure" can be executed by selecting the Lovespouse Stops List and setting the repeat mode to Repeat List.
More information on this topic can be found [here](https://mandomat.github.io/2023-11-13-denial-of-pleasure/).

</details>

<details>
<summary>Spam Detector</summary>

The Spam Detector tool allows you to detect nearby spammers, even on the lock screen.
It can identify spam from Flipper Zeros, our app, and various other software and scripts.
Once your device has detected spam, it will send you a notification, indicating whether it was sent by a Flipper Zero or another source.

> [!NOTE]
> Location permission and background location access are required for this feature to work.
> These permissions are necessary because Android mandates them for Bluetooth scanning in the background.
> It's important to note that the app will NOT collect your location data.

</details>

## Range

<details>
<summary>Details</summary>

The range of Bluetooth Low Energy (BLE) can vary between devices.
Some may have a long range, while others may have a short range.

Android's Bluetooth Low Energy API allows apps to set the TX Power level and include it in the advertiser's payload.
However, it doesn't permit direct modification of the byte values actually transmitted in the payload.
This limitation affects the range of the Fast Pair functionality.
Receiving devices calculate the transmitter's proximity based on the actual received signal strength and the transmitted byte in the payload, which contains the TX Power level the transmitter used.

Devices like the Flipper Zero, however, have the capability to modify this byte, significantly extending their range.

</details>

## Installation

<details>
<summary>Details</summary>

You can get installable APK files from the [GitHub Releases](https://github.com/hendr15k/Bluetooth-LE-Spam/releases).
You can also clone the repository and build the app yourself.

- If you are an end-user looking for a fully functional app, download the **Release APK** for optimal performance.
- If you are a developer or tester, opt for the **Debug APK** for testing and debugging purposes.

</details>

## Building from source

<details>
<summary>Details</summary>

The project targets Android SDK 36, supports Android 8.0 (API 26) and later, and requires JDK 21.

1. Clone the repository:
   ```sh
   git clone https://github.com/hendr15k/Bluetooth-LE-Spam.git
   cd Bluetooth-LE-Spam
   ```
2. Build with Gradle:
   ```sh
   # Debug APK (also installed via Android Studio)
   ./gradlew assembleDebug

   # Release APK
   ./gradlew assembleRelease
   ```

   Alternatively, open the project in [Android Studio](https://developer.android.com/studio) and run it from there.

</details>

## Credits

<details>
<summary>Click to open</summary>

- [mh from mobile-hacker.com](https://www.mobile-hacker.com/author/boni11/) for the [Article / Guideline](https://www.mobile-hacker.com/2023/09/07/spoof-ios-devices-with-bluetooth-pairing-messages-using-android/) about using the nRF Connect App to Spoof iOS Devices
- [Willy-JL](https://github.com/Willy-JL), [ECTO-1A](https://github.com/ECTO-1A), [Spooks4576](https://github.com/Spooks4576) and [Mrproxy](https://github.com/Mr-Proxy-source) for their contribution in the BLE Spam App on the Flipper Zero
- [FuriousMAC](https://github.com/furiousMAC) and [Hexway](https://github.com/hexway) for their prior researches
- [mandomat](https://mandomat.github.io/aboutme/) for the research of [Denial of Pleasure](https://mandomat.github.io/2023-11-13-denial-of-pleasure/)
- [tutozz](https://github.com/tutozz) for the research of Easy Setup Buds in Android
- [K3YOMI](https://github.com/K3YOMI) for the spam detector idea
- [Glorious Gizmos](https://www.tiktok.com/discover/glorious-gizmos) for making content and tutorials on our app
- And special thanks to anyone else who has been involved in prior research and publications related to this topic.

</details>

## Screenshots

<details>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width=300>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width=300>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width=300>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width=300>
</details>

## Disclaimer

<details>
<summary>Disclaimer for Bluetooth Low Energy Protocol Investigation Repository</summary>

This repository contains code for the investigation and experimentation of the Bluetooth Low Energy (BLE) protocol.
Please be aware of the following disclaimers before using or contributing to this repository:

1. Purpose: The code and information provided in this repository are intended for educational and research purposes and is just a proof of concept.
   It is not intended for any malicious or harmful activities.

2. Legal Compliance: Users are responsible for ensuring that their use of the code and information in this repository complies with all applicable laws and regulations, including those governing wireless communication and intellectual property rights.

3. No Warranty: The code and information provided in this repository are provided "as is" without any warranties, expressed or implied.
   The authors and contributors are not responsible for any consequences resulting from the use of this code.

4. Risks: Experimenting with BLE protocols can have potential security and privacy implications.
   Users should exercise caution and use this code responsibly, respecting the privacy and security of devices and systems.

5. Contribution Guidelines: If you contribute to this repository, ensure that your contributions comply with the project's goals and the repository's license.
   By contributing, you agree to license your contributions under the same license as this repository.

6. Support: This repository is not maintained for production use.
   The authors and contributors may not provide support or updates regularly.

By using and contributing to this repository, you agree to these disclaimers and guidelines.
If you do not agree, please refrain from using or contributing to this repository.

For any questions or concerns, please contact the repository maintainers on Discord or GitHub.

</details>

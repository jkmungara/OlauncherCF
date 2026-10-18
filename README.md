<h1 align="center">OlauncherCF</h1>

<p align="center">A minimal, clutter-free Android launcher. Fast, smooth and light on your battery.</p>

<p align="center">
  <a href="https://github.com/jkmungara/OlauncherCF/releases/latest"><img src="https://img.shields.io/github/v/release/jkmungara/OlauncherCF?label=Download&logo=android&color=3DDC84" alt="Download latest release"></a>
  <a href="https://github.com/jkmungara/OlauncherCF/actions/workflows/build.yml"><img src="https://github.com/jkmungara/OlauncherCF/actions/workflows/build.yml/badge.svg" alt="Build status"></a>
  <img src="https://img.shields.io/badge/Android-6.0%2B-3DDC84" alt="Android 6.0 or newer">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3"></a>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/olauncher_0.png" width="250" alt="Home screen">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/olauncher_1.png" width="250" alt="App drawer">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/olauncher_2.png" width="250" alt="Settings">
</p>

<p align="center">
  <a href="https://github.com/jkmungara/OlauncherCF/releases/latest"><img src="art/get-it-on-github.png" alt="Get it on GitHub" height="80"></a>
</p>

## What's new in 1.7.0

A performance, smoothness and stability release:

- **Crash fixes** for double tap, long press and swipe gestures, and for quick repeated taps.
- **Much less background work.** The app list is cached and updated instantly, and bursts of app updates cause a single refresh.
- **Smoother animations** for opening the app drawer and settings, and for expanding settings sections.
- **Safer backups.** An invalid file can no longer wipe your settings, and hidden apps now survive a restore.
- **Smaller app** with fewer libraries.

See the full [changelog](CHANGELOG.md) for every change.

## Download

Get the APK from the [latest release](https://github.com/jkmungara/OlauncherCF/releases/latest). Requires Android 6.0 or newer.

### Switching from another OlauncherCF build

This build uses the same app ID as OlauncherCF on F-Droid but is signed with a different key, so Android will not install it over that version. To switch without losing your setup:

1. In your current OlauncherCF, long press an empty part of the home screen to open settings, then choose **Backup → Store Backup**.
2. Uninstall the current OlauncherCF.
3. Install the APK from the release page and set it as your default launcher.
4. Open settings and choose **Backup → Load Backup**.

The signing certificate fingerprint for each APK is listed in its release notes, so you can verify the download.

## Features

- Originally based on [Olauncher](https://github.com/tanujnotes/Olauncher)
- Removed clutter, like ads and links
- You can rename apps in the app-drawer _(Renaming apps on the home screen is already supported. Just long-click on an app on the home screen and start typing)_
- We have added a lot more options for gestures on the home screen:
    - Gestures are now:
        - Swiping up, down, left, right
        - Clicking on the clock
        - Clicking on the Date
    - Possible actions now include:
        - Open specified app
        - Locking the screen
        - Opening the notification drawer
        - Opening the quick settings
- You can also position the clock independently of the home apps
- Change alignment of apps in app-drawer
- Change font size
- Removed internet permission. You never know what an app developer wants to know about you.

### Optional permissions for gestures

- **Lock screen** and **show recent apps** use an accessibility service on Android 9 and newer. It is off until you enable it, and it can't read the content of your screen.
- On Android 8.1 and older, locking the screen uses device admin instead.

## Build from source

You need JDK 11 or newer and the Android SDK.

```
git clone https://github.com/jkmungara/OlauncherCF.git
cd OlauncherCF
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. Debug builds use the app ID `app.olaunchercf.debug`, so they can be installed next to a release build.

## Contribute

If you find a bug, please search the existing [issues](https://github.com/jkmungara/OlauncherCF/issues) first, and open a new one if it hasn't been reported yet.

## Translations

- A lot of people have translated the app to the following languages. Many thanks to you ❤️
  - Arabic
  - Chinese
  - Croatian
  - Dutch
  - English
  - Estonian
  - French
  - German
  - Greek
  - Indonesian
  - Italian
  - Japanese
  - Korean
  - Lithuanian
  - Persian
  - Portuguese (European)
  - Russian
  - Spanish
  - Swedish
  - Thai
  - Turkish

## Privacy

- OlauncherCF has no network access.
- OlauncherCF does not collect or transmit any data in any way whatsoever.

## Permissions

Olauncher CF uses the following permissions:

- `android.permission.EXPAND_STATUS_BAR`
	- Allows an application to expand or collapse the status bar.
- `android.permission.QUERY_ALL_PACKAGES`
	- Allows query of any normal app on the device, regardless of manifest declarations. Used to show the apps list.
- `android.alarm.permission.SET_ALARM`
	- Allows an application to broadcast an Intent to set an alarm for the user. Used to open the default alarm app if no other clock app is set in the settings.
- `android.permission.REQUEST_DELETE_PACKAGES`
	- Required for issuing the request to remove packages. This does not allow the app to remove apps directly; this only gives the permission to issue the request.

## Credits

- Based on [OlauncherCF](https://github.com/OlauncherCF/OlauncherCF) by its contributors. If you'd like to support its maintainer, you can [buy them a coffee](https://www.buymeacoffee.com/jooooscha).
- OlauncherCF is itself a fork of [Olauncher](https://github.com/tanujnotes/Olauncher) by tanujnotes, available on [Play Store](https://play.google.com/store/apps/details?id=app.olauncher), [F-Droid](https://f-droid.org/packages/app.olauncher/) and [GitHub](https://github.com/tanujnotes/Olauncher).
- Thanks to everyone who translated the app.

## License

OlauncherCF is free software under the [GNU General Public License v3](LICENSE): you can use, study, change and share it. Copyleft keeps it that way, and anyone can build it from the full source.

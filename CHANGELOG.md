# Changelog

All notable changes to this project will be documented in this file.

## [1.7.0] - 2026-10-18

A performance, smoothness and stability release. Every gesture, screen transition and list in the launcher was reviewed. This release fixes several crashes, does far less work in the background, animates more smoothly and makes the app smaller.

### 🐛 Crash Fixes

- Double tap and long press on the home screen no longer crash. Their actions ran on short lived background threads, so a double tap set to "Show app list" or "Open app" could take the launcher down.
- Swiping no longer crashes when Android reports a gesture without a start point.
- Tapping an app twice quickly in the drawer, or swiping twice quickly on the home screen, can no longer stack screens or leave a blank home screen.
- Settings with corrupt values, or values from an older backup, fall back to their defaults instead of crashing at startup.
- Uninstall, app info, accessibility settings and backup actions show a message instead of crashing when no app can handle them.

### 🛡️ Stability

- Loading an invalid backup file no longer wipes your settings. The file is checked first, and only a real OlauncherCF backup replaces them.
- Backups made by earlier versions still load.
- The home screen releases its views while another screen is shown, instead of keeping them in memory.
- One broken or paused work profile no longer stops the whole app list from loading.

### ⚡ Performance

- The app list is loaded once and cached. Hiding, unhiding and renaming apps now update it instantly instead of reloading every app from the system.
- App installs, updates and removals are tracked with the official launcher API. A burst of updates (common while the Play Store updates apps) now causes one reload instead of one per event.
- Gestures no longer start a new thread each time.
- Home screen apps and gesture apps no longer make a system call every time they are read.
- Searching the drawer prepares its search data in the background, only once you start typing, and opening the drawer needs none of it.
- Scrolling the drawer does less work per row.
- The app list is preloaded after the home screen has drawn, so it does not slow down startup.
- Pressing home returns to the launcher faster (`resumeWhilePausing`), and Android no longer needs to save and restore the launcher's state (`stateNotNeeded`).

### 🔋 Resource Usage

- Smaller app: Gson, ConstraintLayout (Views and Compose), Navigation UI, WorkManager and lifecycle-extensions were removed, and test libraries no longer ship inside the app.
- The optional accessibility service (used for lock screen and recents) now asks for the minimum: no access to window content and a single rare event type. Every enabled accessibility service adds work to the whole system, so this reduces overhead outside the launcher too.

### 🎨 Animations

- New screen transitions: the app drawer and settings rise gently into place and sink away when closed, using hardware layers so they stay smooth.
- Settings sections expand and collapse smoothly instead of jumping.
- The "list empty" message no longer flashes every time the drawer opens.
- Long pressing a home app or the home screen gives haptic feedback.

### 🔧 Fixes

- Lowering the number of apps on the home screen now actually removes the extra apps.
- Hidden apps are no longer lost when restoring a backup.
- Chinese, Greek and Swedish translations now load (they used incorrect language codes). Translations with regional variants also load on Android 6.
- New apps installed in a work profile now appear without restarting the launcher.
- Names given to apps in the drawer now also show on the home screen. Renaming an app from the home screen still overrides it.
- Choosing a home screen app now reliably includes hidden apps when it should.
- Double tap reacts immediately instead of waiting 300 ms for a possible triple tap.
- Swipe distances are based on screen density, so swipes feel the same on every phone.
- Tablet detection uses the standard Android definition.

### ⚙️ Miscellaneous Tasks

- *(CI)* Update GitHub Actions to current versions and make the release and emulator test workflows manual.
- *(CI)* Changelog fixes carried over from upstream.

## [1.6.12] - 2024-03-02

### 🐛 Bug Fixes

- *(CI)* Changelog

## [.1.6.11] - 2024-03-02

### 🐛 Bug Fixes

- *(CI)* Changelog generation

## [1.6.10] - 2024-03-02

### ⚙️ Miscellaneous Tasks

- Replace images
- Update english f-droid description
- Remove unused file
- *(CI)* Clean up old changelogs and auto generate changelogs

## [1.6.9] - 2024-03-02

### 🐛 Bug Fixes

- *(CI)* Release action

## [1.6.8] - 2024-03-02

### 🐛 Bug Fixes

- Name in relase action
- Release action

## [1.6.7] - 2024-03-02

### 🐛 Bug Fixes

- Release action

## [1.6.6] - 2024-03-01

### 🚀 Features

- Generate changelog with git cliff

### ⚙️ Miscellaneous Tasks

- Bump version number

## [1.6.5] - 2023-03-25

### 🚀 Features

- Add vietnamese

### ⚙️ Miscellaneous Tasks

- Version number

## [1.6.4] - 2023-01-30

### ⚙️ Miscellaneous Tasks

- Version number

## [1.6.3] - 2023-01-08

### ⚙️ Miscellaneous Tasks

- Version number

### ◀️ Revert

- Use Scrollview instead of jetpack compose in settings (to fix scroll behavior)

## [1.6.2] - 2023-01-02

### 🐛 Bug Fixes

- Double tap app name (#265)
- Backup
- Change log action

### ⚙️ Miscellaneous Tasks

- Version number

### Teset

- Fix serialize test

## [1.6.1] - 2022-12-31

### 🐛 Bug Fixes

- Tip not beeing removed; refactor: simpify firstOpen variable

### 🚜 Refactor

- Adapt translation files
- Use different method of opening quicksettings (thanks @The-Repo-Club)

### 🎨 Styling

- Remove dead code

### ⚙️ Miscellaneous Tasks

- Version number

## [1.6.0] - 2022-12-26

### 🎨 Styling

- README

### ⚙️ Miscellaneous Tasks

- Version number

## [1.5.1] - 2022-12-24

### 🚀 Features

- Add swipe up gesture
- Add toggle for "open apps automatically" (#236)

### 🐛 Bug Fixes

- Build singed apk (#239)
- Crash on double tap gesture
- Fix crash on keyboard enter (closes #244)
- #251

### 🚜 Refactor

- Replace deprecated function calls

### 🎨 Styling

- Code clean up

### ⚙️ Miscellaneous Tasks

- Issue templates
- Version number

### Build

- Setup apk signing
- Apk signing
- Fix
- Fix variable
- Trying out something. Maybe I understand the docs wrong

## [1.5.0] - 2022-12-16

### 🚀 Features

- Add two new actions: show recents and open quick settings

### 🐛 Bug Fixes

- This improves a lot of code concerend with locking the screen. This fixes #180

### 🎨 Styling

- Clarify strings

### ⚙️ Miscellaneous Tasks

- Version number

### Translations

- Add translations for Lithuanian (#230)

## [1.4.0] - 2022-12-13

### 🎨 Styling

- Reorder langs in readme

### ⚙️ Miscellaneous Tasks

- Version number

## [1.3.4] - 2022-12-07

### ⚙️ Miscellaneous Tasks

- Version number

## [1.3.3] - 2022-11-15

### 🐛 Bug Fixes

- Text size in settings
- #209

### ⚙️ Miscellaneous Tasks

- Version number

### ◀️ Revert

- Settings text changin size (i guess it was better as it was before)

## [1.3.2] - 2022-11-12

### 🚜 Refactor

- Improve text resize behaviour and set broader limits (#207)

### ⚙️ Miscellaneous Tasks

- Version number

## [1.3.1] - 2022-11-05

### 🐛 Bug Fixes

- Work icon size and work icon position

### ⚙️ Miscellaneous Tasks

- Version number

## [1.3.0] - 2022-10-31

### 🚀 Features

- Replace some texts with images, enable uninstall of apps throug… (#204)

### 🐛 Bug Fixes

- Set app name right when it is added to the home screen, not just at showing the fragment (#202)
- Version number

### ⚙️ Miscellaneous Tasks

- Version number

## [1.2.10] - 2022-09-21

### 🐛 Bug Fixes

- Set swipe right app

### ⚙️ Miscellaneous Tasks

- Version number

## [1.2.9] - 2022-09-21

### 🐛 Bug Fixes

- Move 'backToHomeScreen()' to onResume to not show home shortly before opening an app
- #172

### ⚙️ Miscellaneous Tasks

- Update dependencies
- Update dependencies
- Version number

## [1.2.8] - 2022-09-20

### 🐛 Bug Fixes

- German translation (#191)

### ⚙️ Miscellaneous Tasks

- Version number

## [1.2.7] - 2022-09-19

### 🐛 Bug Fixes

- *(homescreen)* This fixes the homescreen alignment bug (#185), while also being a general code improvement commit (#187)
- *(homescreen)* Place apps that are only in work profile on home screen (#188)

### ⚙️ Miscellaneous Tasks

- Version number

## [1.2.6] - 2022-09-14

### 🐛 Bug Fixes

- Remove non-free dependencies

## [1.2.5] - 2022-09-08

### 🚀 Features

- *(german)* Update german string (#175)

### 🐛 Bug Fixes

- Home alignment when click area is exetended (#176)

### ⚙️ Miscellaneous Tasks

- Version number

## [1.2.1] - 2022-08-24

### ⚙️ Miscellaneous Tasks

- Update version number

## [1.2.0] - 2022-08-24

### 🚀 Features

- Implement #102 (#160)

### ⚙️ Miscellaneous Tasks

- Bump version number

## [1.1.0] - 2022-08-06

### 🚀 Features

- Implements #82 (#136)

### 🐛 Bug Fixes

- Hidden apps string hardcoded (#126)
- Set app language to system language (#128)
- Swipe app names black if not set

### ⚙️ Miscellaneous Tasks

- Version number, preapre for next release

## [1.01] - 2022-07-17

### 🚀 Features

- Toggle date and time independently (#88)
- Add korean to settings

### 🐛 Bug Fixes

- Remove strings, use resources
- Use packageName for applias identifier instead of appLabel (#103)

### 🚜 Refactor

- Replace string with string resourece (#97)

### ⚙️ Miscellaneous Tasks

- Add pull request template for translations
- Rename translation template
- Rename pull_request_template
- Add version number to settings to improve debugging
- Change version code

## [1.0.0] - 2022-07-02

### 🚀 Features

- Changelogs for v9

### 🚜 Refactor

- Migrate to jetpack compose (#30)

### 🎨 Styling

- Deutsch -> german

### Add

- European Portuguese (#68)

## [test] - 2022-06-17

### 🐛 Bug Fixes

- *(settings)* Language list doubled

## [fork1.8] - 2022-05-25

### 🚀 Features

- Option to set language from system settings
- Option to set language from system settings

### 🐛 Bug Fixes

- Text size string
- Add layout to landscape-layout
- Add layout to landscape-layout

### ⚙️ Miscellaneous Tasks

- Change deprecated code
- Remove unused strings
- Remove more ads
- Removed more unused strings
- Removed even more unused strings
- Removed unused constants and xml layout
- Remove mode dead code
- Change version code

### Build

- Add build action
- Update build action
- Update kotlin version

## [fork1.7] - 2022-05-10

### 🚀 Features

- Change text size
- List hidden apps

### 🐛 Bug Fixes

- Fix double tap2lock

### ⚙️ Miscellaneous Tasks

- Clean up code

### Release

- Version number

## [fork1.6] - 2022-05-04

### 🚀 Features

- *(translation)* Manually select language
- *(permission)* Remove internet permisisons, and everything that needed it

### 🐛 Bug Fixes

- *(landscape)* Add language menu to landscape layout xml

### ⚙️ Miscellaneous Tasks

- *(version)* Update version code

## [fork1.5] - 2022-04-27

### 📚 Documentation

- *(translation)* Add italian translation

## [fork1.0] - 2022-03-28

### Fixed

- App drawer swipe down to dismiss not working sometimes

## [3.4.1] - 2021-09-01

### Fixed

- Daily wallpaper not updating correctly

## [3.4] - 2021-08-28

### Fixed

- Pressing enter not opening the first app in list

## [3.0] - 2021-03-12

### Fixed

- Some calendar apps not showing up
- Showing message dialog again and again

## [2.6] - 2021-01-08

### Fixed

- Wallpaper worker not cancelled when setting black wallpaper
- Some apps not being added on home screen
- Screen timeout resetting to 30 seconds if screen timeout lock is enabled

## [1.8] - 2020-11-19

### Fixed

- Some work apps not opening

## [1.7] - 2020-11-19

### Fixed

- Home apps not opening if userhandle is empty
- Crashing while opening dual/cloned apps

## [1.5] - 2020-11-16

### Fixed

- Crash when a hidden app list contains an uninstalled app

## [1.4] - 2020-10-18

### Fixed

- Hidden apps set getting empty

## [1.3] - 2020-10-11

### Fixed

- Extra space after apps when right aligned
- Non-transparent navigation bar in Android 10
- Gravity issue in home apps text
- App list not updating immediately after install/uninstall
- Not showing the triple lock message
- Nav bar not showing in certain conditions

### Updated

- List of fallback wallpaper urls

## [1.2] - 2020-09-10

### Fixed

- Home apps not changin color on pressed

## [1.0] - 2020-09-01

### Fixed

- App list refreshing unnecessarily
- Theme change glitch on cold app start

## [0.5] - 2020-08-07

### Fixed

- App drawer closing unexpectedly when swiping after partial search
- Default wallpaper not working

## [0.4] - 2020-07-27

### Fixed

- Not showing date if phone is set to 24 hour time format

## [0.3] - 2020-07-23

### Fixed

- Search view text color
- Wallpaper issue in Marshmallow (Android 6.0)
- App start crash in Android 8.0
- Apps blinking when home button pressed

## [0.1] - 2020-07-17

### Fixed

- Crash on initialising navController in MainActivity
- App click listener in app list

### Refactoring

- Moved some logic from view to viewmodel

<!-- generated by git-cliff -->

# Apple-Clean-TV

An Apple-style Android TV launcher: liquid glass UI, buttery 60fps animations,
icon packs, live wallpapers, Apple-like clock widgets, and a clean minimal home.

## Base project attribution

This project is built on top of **CleanYourTV** by Gathin
(https://github.com/Gathin23/cleanyourtv), MIT licensed.
All credit for the original launcher, debloat tooling, and base architecture
goes to the CleanYourTV project and its contributors.

## What's customized

- Apple liquid-glass theme across home, dock, settings, and dialogs
- Icon packs: OG originals, black liquid glass, Dark 16:9 tiles
  (switch per profile in settings, or double-press LEFT+RIGHT to quick-switch)
- 10 bundled offline wallpapers + blood-moon live wallpaper
- 7 minimal Apple-style clock widget designs with a picker
- Apple-style app open/close animations (zoom from icon, matching reverse)
- Fullscreen wallpaper view (UP) with swipe-up wallpaper changer
- Wi-Fi button hands off to the TV's native system Wi-Fi settings
- Zen mode, recent-apps glass cards, screensaver with offline fallback
- Custom avatar pack support

## Build

Open `launcher/` in Android Studio or run the Gradle wrapper:

```
cd launcher
./gradlew assembleRelease
```

Install on your TV over ADB:

```
adb install -r launcher/app/build/outputs/apk/release/app-release.apk
```

Note: binary assets (icons, wallpapers, fonts) ship with the release zip.
See Releases for the complete bundle.

## License

MIT, see LICENSE. The base CleanYourTV license and notices are preserved.

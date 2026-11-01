# Apple-Clean-TV

Turn any regular Android TV into a premium Apple-like UI with just a code.

## What is this?

Apple-Clean-TV is a **launcher for Android TV**. It fully replaces your TV's
stock home screen. Install it, set it as default Home, and your TV boots
straight into it. No ads, no clutter.

What it does:

- **Removes bloatware** — safe debloat disables the junk packages your TV
  ships with (stock launcher kept as instant fallback), with a full backup
  and one-line restore if you ever want stock back
- **Removes ads** — the stock Android TV home pushes sponsored rows and ads;
  this launcher has none. Your home screen is yours again
- **Makes your TV super fast** — no heavy stock UI, no background junk,
  60fps target, lightweight by design
- **Premium Apple-like UI** — liquid glass dock, cinematic top shelf, frosted
  dialogs, horizon glow, Apple-style clocks and animations throughout

## Base project attribution

This project is built on top of **CleanYourTV** by Gathin
(https://github.com/Gathin23/cleanyourtv), MIT licensed.
All credit for the original launcher, debloat tooling, and base architecture
goes to the CleanYourTV project and its contributors. See the base docs in
`docs/` and `PROMPT.md` for the original TV debloat procedure.

## Features

- **Liquid glass UI** — frosted dock, cinematic top shelf, glass dialogs,
  horizon glow, fully Apple-like throughout
- **Icon packs** — OG originals, black liquid glass, Dark 16:9 tiles.
  Switch per profile in Settings, or double-press LEFT+RIGHT on the remote
  to quick-switch. Your last pick stays default after restart
- **Live wallpapers** — blood-moon live wallpaper bundled, plus 10 offline
  wallpapers. No internet needed
- **Fullscreen wallpaper view** — press UP for an immersive wallpaper +
  clock screen; swipe up to reveal the wallpaper changer, LEFT/RIGHT to
  flip through wallpapers with smooth crossfade
- **7 Apple-style clock widgets** — minimal designs with a picker in Settings,
  shown on home and the fullscreen clock
- **App open/close animations** — apps zoom out from their icon on open and
  shrink back into it on close, same fluid animation both ways
- **Wi-Fi** — the Wi-Fi button opens the TV's native system Wi-Fi settings
- **Zen mode** — wallpaper + clock only, after 10 min idle on home,
  any key exits, never over other apps
- **Recent apps** — triple-press OK or UP+DOWN together; premium glass cards
- **Screensaver** — clock with online video streams and offline fallback
- **Safe debloat** — only safe packages disabled, full backup + one-line
  restore (`backup/restore.ps1` / `restore.sh`)

## Build it yourself with an AI agent

You don't need to be a developer. An AI agent (Cursor, Antigravity, Claude)
can build and customize this entire launcher for you.

1. **Connect your TV** — on your Android TV, enable Developer options, turn on
   ADB debugging, and connect over WiFi: `adb connect <TV-IP>:5555`
2. **Point your agent at this repo** — open the project in Cursor/Antigravity/
   Claude and tell it what you want
3. **Just wait** — the agent writes the code, builds the APK, installs it on
   your TV, and verifies it on the real screen
4. **Or customize anything** — new icon packs, new wallpapers, new animations,
   new features, change anything you want. It's all here, all editable

One repo, one WiFi connection, any old Android TV becomes premium and fast.

## Requirements

- Android TV / Google TV (tested on Realme Smart TV, Android 11)
- ADB access to the TV (same Wi-Fi network)
- JDK 21, Android SDK 34 for building

## Build

```
cd launcher
./gradlew assembleRelease
```

The APK lands at `launcher/app/build/outputs/apk/release/app-release.apk`.

## Install on your TV

1. Enable Developer options on the TV, turn on USB/ADB debugging
2. Connect: `adb connect <TV-IP>:5555`
3. Install: `adb install -r launcher/app/build/outputs/apk/release/app-release.apk`
4. Set as default Home when the TV prompts, or pick it in Settings > Apps

Build-and-install helpers: `launcher/build-and-install.sh` (Linux/macOS),
`launcher/build-and-install.ps1` (Windows).

## Icon packs

Drop 512x512 PNGs named by package name
(e.g. `com.google.android.youtube.tv.png`) into
`/sdcard/Android/data/com.gathin.tvhome/files/icons/` on the TV to replace
icons instantly, or add them under `launcher/app/src/main/res/drawable-nodpi/`
before building.

## Customization

All behaviour flags live in `launcher/app/src/main/res/values/config.xml`:
dock style, glow, effects, zen idle timeout, and more. See the base
`PROMPT.md` for the full debloat + theme workflow.

## Screenshots

Real device screenshots coming soon in `docs/screenshots/`.

## License

MIT, see LICENSE. The base CleanYourTV license and notices are preserved.

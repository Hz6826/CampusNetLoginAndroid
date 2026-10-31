# AGENTS.md

Guidance for AI coding agents working in this repository. Keep it up to date when
structure, build commands or conventions change.

## What this repository is

`XJTUNetLoginAndroid` is the Android client of the XJTU campus network login tool.
It is a Kotlin / Jetpack Compose rewrite of the desktop Qt client
[`XJTUNetLogin`](https://github.com/Hz6826/XJTUNetLogin) and uses
[Miuix](https://github.com/compose-miuix-ui/miuix) for the UI.

- Language: Kotlin
- UI: Jetpack Compose + Miuix 0.9.4
- License: **GPL-3.0** (`LICENSE`). The liquid-glass bottom bar is derived from
  XJTU-ToolBox (GPL-3.0) — keep existing source headers when touching it.

## Toolchain

- JDK 21 (language/target level 21)
- Android Gradle Plugin 9.x, Gradle wrapper 9.x
- compileSdk / targetSdk 37, minSdk 26
- Install **Android SDK Platform 37** and **Build-Tools 37.0.0**

## Build and verification commands

Run from the repository root.

Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

macOS / Linux:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Instrumented tests (`:app:connectedDebugAndroidTest`) require a connected device
or emulator. **Warn the user before running them**: the test rule clears the
saved account and the suite may log out a real campus session. Do not install the
APK to a user's device unless they explicitly allow it.

## Repository layout

```
app/src/main/java/com/hz6826/xjtunetlogin/
  MainActivity.kt          entry point, navigation shell, top bar, service wiring
  data/                    AccountStore (Keystore + AES-GCM), SettingsStore (prefs)
  model/                   Account, OnlineInfo, SessionState, LogEntry
  net/                     SrunClient, SrunCrypto, WifiTransport (protocol layer)
  session/                 SessionController state machine, SessionHolder singleton
  service/                 AutoLoginService (foreground keep-alive), BootReceiver
  update/                  GitHub Releases update checker, APK download + SHA-256
  ui/theme/                AppTheme, palettes, status colors
  ui/components/           shared composables (status card, settings rows, update dialog)
  ui/pages/                Login / Log / OnlineInfo / Settings / Connection /
                           KeepAlive / About / Contributors / References pages
  ui/nav/                  classic NavigationBar + LocalBottomBarInset
  ui/glass/                liquid-glass bottom bar (ported, GPL-3.0)
app/src/test/              JVM unit tests (protocol/crypto vectors)
app/src/androidTest/       instrumented UI smoke tests
```

## Conventions

- UI strings are Simplified Chinese; code comments and identifiers are English
  (with a few Chinese comments explaining UX decisions — keep that style).
- Use Miuix components (`Card`, `TextField`, `SwitchPreference`,
  `ArrowPreference`, `SmallTitle`, `Scaffold`, …) rather than Material widgets.
- The app intentionally supports Android 8+; guard API-31+ features
  (Monet colors, liquid glass) with `Build.VERSION.SDK_INT`.
- Do not add new permissions unless a feature requires them; document new
  permissions in the manifest and in `README.md`.
- Keep the account password out of logs, HTTP logs, status text and notifications.
- `SessionState`/`SessionController` is the single source of truth. Silent
  background checks must not set `busy`, change the status text or spam logs.
- Prefer `edit`/`write` over shell scripts for file changes.
- Keep diff scope minimal; this project values a readable, small codebase over
  speculative abstractions.

## Verification expectations

For code changes run `assembleDebug`, `testDebugUnitTest` and `lintDebug`; the
CI workflow does the same. If you touch protocol or crypto code, add/adjust unit
vectors in `app/src/test/.../SrunClientTest.kt`. If you touch navigation,
persistence or the session state machine, describe the manual test steps in the
PR / commit message.

## Pull requests

Use `.github/pull_request_template.md`. Keep commit subjects in the form
`area: short summary` (e.g. `ui: keep current tab when switching theme`).

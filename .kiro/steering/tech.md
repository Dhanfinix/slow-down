# Tech

## Stack
- Kotlin 2.2.21, AGP 8.13.0, Gradle 8.14.2 (wrapper), JDK 17+ (built with Temurin 18).
- compileSdk 36, targetSdk 36, minSdk 26.
- Jetpack Compose (BOM 2025.10.01) with Material 3 Expressive (`material3 1.5.0-alpha08`; 1.4.0 keeps the expressive APIs internal).
- Navigation Compose with type-safe `@Serializable` routes. ViewModels with StateFlow.
- DataStore for settings and stats. No Room.
- `VpnService` with a userspace packet relay (Java NIO) for the throttle. No root.
- `UsageStatsManager` for foreground detection. Foreground services use type `specialUse`.
- Tests: JUnit on the JVM. No instrumented tests yet.
- Version catalog: `gradle/libs.versions.toml`.

## Rules
- Run **one Gradle command at a time**, with `--no-daemon` on a tight machine. Never run two builds in parallel.
- **Never claim the throttle works without a measurement.** Cite a number and where it came from (`docs/REPORT-e2e.md`).
- The `:throttle` public API (`ThrottleController`, `ThrottleConfig`, `ThrottleState`, `ThrottleControllers`, `VpnPermission`) is a contract. Do not change it without changing `:app` in the same change.
- Only `ThrottleCoordinator` in `:app` may call `ThrottleController`.
- Keep detection logic pure Kotlin so it runs in JVM tests with a virtual clock.
- No secrets in the repo. `local.properties` is git-ignored.
- Never commit without the owner's go-ahead.

## Commands
```
./gradlew :throttle:testDebugUnitTest --no-daemon     # 22 engine tests
./gradlew testDebugUnitTest --no-daemon               # 42 tests
./gradlew lintDebug --no-daemon
./gradlew assembleDebug --no-daemon                   # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug --no-daemon
```

## Device checks
- VPN is up: `adb shell dumpsys connectivity | grep -i vpn`
- Logs: `adb logcat -s SlowDownEngine SlowDownVpn`
- Measure speed on a device with a timed HTTPS download. Do not trust the app's own counter alone.

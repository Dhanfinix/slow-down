# Report: signed release v1.0.0

Date: 2026-11-26.

**Back up `~/.slowdown-release/` now** (`slowdown-release.jks` and `keystore.properties`). If it is lost, no update can be installed over this APK. It is outside the repo and git-ignored by pattern (`*.jks`); the folder is not inside the work tree.

## Result
- Release: https://github.com/Dhanfinix/slow-down/releases/tag/v1.0.0 (target `submission`)
- Asset: `slow-down-v1.0.0.apk`, 2,582,686 bytes (2.5 MB)
- SHA-256: `01361b0ce399e6745b9338ba558b3178e288b8d641c31ddc9410b4310e8ead6d`
- Downloaded back with `gh release download v1.0.0`; SHA-256 identical. `gh release view` digest also identical.
- Commits on `submission`: "Release build: R8 on, signing from external keystore, version 1.0.0", then "README: download and quick-try section". `git rev-parse HEAD origin/submission` equal after push.

## Signing
- `keytool -genkeypair -keyalg RSA -keysize 2048 -validity 10000 -alias slowdown -dname "CN=Slow Down"` into `~/.slowdown-release/slowdown-release.jks`. Random 32-hex password in `keystore.properties`. Both `chmod 600`, folder `700`.
- `app/build.gradle.kts` reads the properties file from `-PslowdownKeystoreProps`, env `SLOWDOWN_KEYSTORE_PROPS`, or the default path. If absent, no signing config is created and the release APK is unsigned, so a clean clone still builds.
- `apksigner verify --verbose --print-certs`: Verifies. Scheme v2 true (v1, v3 false). One signer, `CN=Slow Down`, cert SHA-256 `27496564a11af94f83a27ce56148ed91dd49e219c64ed3bd271579d89e78e3ad`.

## Build
- `versionName 1.0.0`, `versionCode 1`, `isMinifyEnabled = true`, `isShrinkResources = true`, rules in `app/proguard-rules.pro` (serializers for `com.slowdown.app`, `SlowDownVpnService`, `BroadcastReceiver` subclasses).
- `./gradlew assembleRelease --no-daemon`: BUILD SUCCESSFUL (1m 19s).
- `aapt2 dump badging`: package `com.slowdown.app`, versionCode 1, versionName 1.0.0, minSdk 26, targetSdk 36. Permissions: PACKAGE_USAGE_STATS, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, RECEIVE_BOOT_COMPLETED, INTERNET, and the app's own DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION.

## R8 findings (emulator-5554, Android 15)
Fresh install (the debug build was removed first: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, `adb uninstall`). Result: no breakage with the rules above.
- Launch and the 4 intro pages, VPN consent dialog (OK), Home, Settings, Manage Watched Apps list, Stats: all open and render (fonts included).
- "Slow me down now" at 384 kbps, scope Entire device: VPN network present; 12 s HTTPS download measured **387.4 kbps** (+0.9%). Baseline before: 47,210 kbps. After Stop: 0 VPN networks, 35,413 kbps.
- `logcat -b crash`: the only FATAL entries came from the `Dl` measuring tool (shell process, IPv6 `ConnectException`), not from the app. `Dl` was rebuilt with `java.net.preferIPv4Stack=true` (the emulator has no working IPv6); the app's own IPv6 blocking is unchanged.

## What was not checked
- **The release APK was not installed on the phone `ZP2222TZPT`.** `adb devices` listed only `emulator-5554` for the whole run, so there was no phone to install on and no phone speed run. The phone numbers in the release notes come from the earlier debug build (`docs/REPORT-device.md`), and the notes say so. The phone was not touched, so it is as it was left (unthrottled).
- Automatic doom-scroll trigger, notification Stop, boot receiver, Wi-Fi and Moto battery behaviour on the release build.
- The Play Protect warning text on a real phone.
- A clean-clone `assembleRelease` without the keystore file (the code path is guarded, but this run did not build it).
- Settings left as found on the emulator: not applicable, the app was freshly installed; the emulator's scope is now Entire device.

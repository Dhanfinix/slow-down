# Report: Slow Down on a real phone

Date: 2026-11-26. No code was changed. Nothing was committed.

## Device
- Moto g67 power 5G (`ZP2222TZPT`), motorola, Android 16, build `W1VTS36H.22-20-3-2-6`, arm64-v8a.
- Network: **mobile data (LTE, interface `rmnet_data7`)**, not Wi-Fi. Source: `dumpsys connectivity`.
- Private DNS: strict hostname mode, `dns.adguard.com` (`settings get global private_dns_mode` = `hostname`).
- All adb commands used `adb -s ZP2222TZPT`.

## What was done
| Step | Command or action | Result |
|---|---|---|
| Build | `./gradlew assembleDebug --no-daemon` | OK, `app-debug.apk` |
| Install | `adb -s ZP2222TZPT install -r app/build/outputs/apk/debug/app-debug.apk` | `Success`. No prompt, no signature clash |
| Launch | `adb -s ZP2222TZPT shell am start -n com.slowdown.app/.MainActivity` | Onboarding shown, focus on `MainActivity` |
| Crashes | `adb -s ZP2222TZPT logcat -b crash -d` | Empty, before and after the whole run |
| Notifications | `pm grant com.slowdown.app android.permission.POST_NOTIFICATIONS` | Shown as Enabled |
| Usage access | `appops set com.slowdown.app GET_USAGE_STATS allow` | Allowed on this phone, shown as Allowed in the app |
| VPN consent | The user tapped OK on the system dialog | Shown as Allowed |

## Speed
Tool: `Dl` (from `docs/REPORT-e2e.md`), run as `CLASSPATH=/data/local/tmp/dl.dex app_process / Dl https://proof.ovh.net/files/100Mb.dat 12`. The same `classes.dex` ran on this arm64 Android 16 phone without change. Each run is 12 s of HTTPS download.

| State | Limit (kbps) | Measured (kbps) | Error |
|---|---|---|---|
| Before | none | 22483.3 | |
| Slowed (manual, 3G Patient, Entire device) | 384 | 387.0 and 383.6 | +0.8% / -0.1% |
| After Stop | none | 18107.5 | |

The speed after Stop is lower than the first run. Mobile data varies, so this shows speed came back to about 18 Mbps, not that it matches the first run.

Slowed state check: `dumpsys connectivity` listed a `VPN CONNECTED extra: VPN:com.slowdown.app` network on `tun0`. The home screen showed Slowed, 384 kbps. After pressing "Stop slowing" the same command found 0 VPN networks, and the download was fast again.

## Screenshots (`docs/screens/`)
- `device-01-launch.png` onboarding, `device-02-permissions.png` permission step, `device-03-vpn-consent.png` system VPN dialog, `device-04-after-consent.png` all three allowed.
- `device-05-home.png` Watching, `device-06-manual.png` manual slowdown card, `device-07-settings.png`, `device-08-settings-scope.png` settings.
- `device-09-slowed.png` Slowed at 384 kbps, `device-10-stopped.png` after Stop.
- `device-08-settings-scope.png` could not be opened by the image viewer, so its content was not checked. It was re-captured once after that.

## Moto-specific notes
- Private DNS is strict (`dns.adguard.com`) and the VPN has no DNS servers set. Name lookups still worked while slowed (`dns proof.ovh.net` 3 ms to 142 ms), so this setup is fine. Other Private DNS hostnames were not tried.
- `WatcherService` was running as a foreground service (id 1001) at the end of the run. It is not on the battery-optimization whitelist (`dumpsys deviceidle whitelist` has no slowdown entry). Whether Moto kills it after hours in the background was **not tested**.

## Not checked
- Auto doom-scroll trigger with real app use on this phone (threshold was 20 min).
- The notification Stop action, the boot receiver, and the cooldown release on this phone.
- Long-run behaviour of `WatcherService` under Moto battery management.
- Wi-Fi. Only mobile data was measured.
- The scope setting was set to "Entire device" by tapping. The measurement covered the shell uid, which only the entire-device scope covers, so the scope worked.

## End state
Stopped. 0 VPN networks, full speed (18107.5 kbps), no crashes.

# Slow Down: end-to-end report (emulator)

Verifier: independent run, 2026-11-26. Device: `emulator-5554`, Android 15 (API 35, arm64, Google APIs). Nothing was committed.

## Result
Everything in the brief works on the emulator after 3 fixes (below).

## What was checked

| # | Check | Result |
|---|---|---|
| 1 | Fresh install (`adb uninstall` then `install`), 4 onboarding pages, VPN consent dialog, notification dialog, usage access (real Settings page, toggled by tap, `appops` shows `allow`) | Pass |
| 2 | "Slow me down now": `dumpsys connectivity` shows `VPN CONNECTED ... VPN:com.slowdown.app`, `tun0` | Pass |
| 2 | Speed limit, 12 s HTTPS download, two speeds (table below) | Pass, within 1.5% |
| 2 | DNS and HTTPS: Java `HttpURLConnection` (DNS lookup, TLS) and Chrome loading `https://www.wikipedia.org` while throttled | Pass (after fix 1) |
| 3 | Stop from the Home button and from the notification action: VPN gone, speed back to 22 to 32 Mbps | Pass |
| 4 | Real doom scroll: threshold 5 min (minimum), release 1 min, scope Watched apps (YouTube). YouTube kept in front for about 5 min. | Pass |
| 4 | Auto throttle started on its own, notification "Time to look up. You have been scrolling 5 min. Slowing down." posted | Pass |
| 4 | Per-app: VPN `Uids: <{10167-10167, 20167-20167}>` = only `com.google.android.youtube`. A shell download during the throttle ran at 32 Mbps | Pass |
| 4 | Cooldown: left YouTube, VPN gone in about 1 to 2 min | Pass |
| 5 | Process death: `run-as ... kill -9` while throttling: VPN torn down, watcher restarted by the system, no crash, stale notification removed (after fix 3) | Pass |
| 5 | Real reboot (`adb reboot`) with Watching on: `BootReceiver` restarted `WatcherService` (foreground, id 1001), no crash in `logcat -b crash` | Pass |
| 6 | `./gradlew assembleDebug testDebugUnitTest lintDebug` | BUILD SUCCESSFUL, 42 tests, 0 failures (22 `:throttle`, 20 `:app`), lint has no errors |
| 6 | `./gradlew assembleRelease` | BUILD SUCCESSFUL, `app-release-unsigned.apk` (no signing config, R8 off) |

Usage events were real `UsageStatsManager` events. No injection was used.

## Measured speed
Tool: `/tmp/sd/t/Dl.java`, compiled with `javac --release 8` and `d8`, pushed to `/data/local/tmp/dl.dex`, run as `CLASSPATH=/data/local/tmp/dl.dex app_process / Dl <url> <seconds>` (HTTPS, reads for N seconds, prints kbps). URL: `https://proof.ovh.net/files/100Mb.dat`. Scope "Whole phone" so the shell uid is covered.

| Limit (kbps) | Measured (kbps) | Error |
|---|---|---|
| none (baseline) | 29833 | |
| 128 | 127.0 | -0.8% |
| 384 | 387.4 and 388.1 (two runs) | +0.9% / +1.1% |
| 1024 | 1012.7 | -1.1% |

Each run was 12 s. The app's own byte counter also moved (447 KB down at the 128 kbps run).

## Fixes made
1. `throttle/.../SlowDownVpnService.kt`: removed the IPv6 address and `::/0` route. Before, Chrome showed `ERR_CONNECTION_REFUSED` and the Java client hung for any host whose DNS answer had an IPv6 address first (wikipedia.org did). The ICMPv6 rejection did not make clients fall back. With no IPv6 route in a non-bypassable VPN, IPv6 connects fail at once and apps use IPv4. No traffic leaks around the limit.
2. `app/.../MainActivity.kt`: if Watching is on in settings but `WatcherService` is not running (app force-stopped or updated), opening the app now starts it. Before, the watcher stayed off, so auto detection never ran after an install over an old one.
3. `throttle/.../ThrottleControllers.kt` (`init`): after the process is killed during a throttle, Android leaves the dead VPN service's "Internet limited to N kbps" notification. `cancel` does nothing on a foreground notification, so a new process now sends `ACTION_STOP` to the service when that notification exists.

Public API of `:throttle` is unchanged. The engine unit tests were rerun and pass (22).

## Screenshots (`docs/screens/`)
- `e2e-01-permissions-granted.png` three permissions ticked
- `e2e-02-home.png` Home after onboarding
- `e2e-03-slowed-1024.png` Home while slowed
- `e2e-04-chrome-https-throttled.png` Chrome loading wikipedia over HTTPS while throttled
- `e2e-05-notification-stop.png` Slow Down notification with Stop
- `e2e-06-settings-min-threshold.png` threshold 5 min, release 1 min
- `e2e-07-auto-throttle-notification.png` auto throttle notifications
- `e2e-08-after-cooldown-home.png` Home after cooldown (Watching)

## Remaining limitations
- No DNS servers are set on the VPN. DNS works because the system uses the underlying network's resolver. Not tested with Private DNS set to a hostname.
- IPv6 is not throttled and not tunnelled. It is blocked for covered apps, so they use IPv4. A pure IPv6-only network would not work.
- Changing "What to slow" (scope) in Settings does not change a throttle that is already running. It applies at the next start.
- Chrome page load timing and the YouTube app's own speed under the limit were not measured. Per-app proof is the VPN uid list plus an unthrottled shell download, not a YouTube speed test.
- Other app speed numbers come from one TCP stream (no parallel streams, no UDP/QUIC test, no extra-latency test).
- Release build is unsigned and not minified, so R8 behaviour is untested.
- The "Time to look up" alert notification stays after the cooldown (it is informational).
- The emulator has a Proxyman CA installed. It did not affect results.
- Tested on one emulator only (API 35). The physical phone `ZP2222TZPT` was not used.

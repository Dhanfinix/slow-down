# Slow Down

**Bring dial-up back, on purpose.** Slow Down is an Android app that makes your internet slow when you doom scroll. At 2G or dial-up speed the feed stops being fun, so you look up.

Built for the [Kiroween 2026](https://kiroween.devpost.com/) hackathon, **Resurrection** category: *bring your favorite dead technology back to life*. The dead technology here is slow internet. Dial-up and 2G were a problem we fixed. Slow Down revives them as a digital wellbeing tool.

The idea came from the author's commute. A slow connection made them stop scrolling.

<p>
<img src="docs/readme/home-watching-light.png" width="170" alt="Home, watching">
<img src="docs/readme/home-slowed-light.png" width="170" alt="Home, slowed (light)">
<img src="docs/readme/home-slowed-dark.png" width="170" alt="Home, slowed (dark)">
</p>
<p>
<img src="docs/readme/stats-light.png" width="170" alt="Stats (light)">
<img src="docs/readme/stats-dark.png" width="170" alt="Stats (dark)">
<img src="docs/readme/settings-light.png" width="170" alt="Settings">
<img src="docs/readme/phone-slowed.png" width="170" alt="Slowed at 384 kbps on a real Moto g67 power 5G">
</p>

## Download / try it
**[Download slow-down-v1.0.0.apk](https://github.com/Dhanfinix/slow-down/releases/tag/v1.0.0)** (2.5 MB, Android 8.0 or newer)

SHA-256: `01361b0ce399e6745b9338ba558b3178e288b8d641c31ddc9410b4310e8ead6d`

1. Download the APK on your phone.
2. Open it and allow installs from this source if Android asks.
3. Open Slow Down and go through the intro.
4. Grant the permissions below.

| Permission | Why |
|---|---|
| VPN connection (required) | Slows traffic. Runs on the phone, nothing leaves it. |
| Notifications (recommended) | Shows "Slowing down" with a Stop button. |
| Usage access (optional) | Lets the app see which app is in front, for the automatic trigger. |

The APK is signed with a self-generated key. Play Protect may warn about it. That is normal for sideloaded apps.

**See it work in 2 minutes:** Settings, set the speed to 128 kbps. Home, tap **Slow me down now**. Open any website or video. It crawls. Tap **Stop slowing** and it is fast again.

## How it works
1. You pick watched apps, a time limit (5 to 120 min), a speed (32 to 1024 kbps) and a release time (1 to 30 min).
2. A foreground service reads `UsageStatsManager` events every 15 s and tracks how long you stay in a watched app.
3. At the limit, a local `VpnService` slows those apps and a notification says "You have been scrolling N min. Slowing down." with a Stop button.
4. When you have been away for the release time, speed returns.

You can also tap "Slow me down now" with a 15, 30 or 60 min timer.

No root. No account. No server. Settings and stats stay on the phone (DataStore).

## How the throttle works
The VPN sends the covered apps' IPv4 traffic into a TUN interface. Our engine (`:throttle`) reads the raw packets and plays the server for the app's TCP stack. It opens its own real socket (`protect()`ed, so it skips the VPN) to the real server, and moves bytes through a token bucket.

- **Download:** the engine reads from the real socket only when the bucket has tokens and the app's window has room. TCP flow control slows the server. Nothing is dropped.
- **Upload:** app data waits in a 64 KB buffer that drains at the upload rate, and the advertised window shrinks when it is full.
- **UDP:** extra packets are dropped, so QUIC backs off. DNS is not limited.
- **IPv6:** not routed. IPv6 connects fail at once and apps fall back to IPv4.

Details: [`docs/REPORT-engine.md`](docs/REPORT-engine.md).

## Architecture
```
app/        Compose UI (Material 3 Expressive), detection, DataStore
  logic/    DoomScrollDetector (pure Kotlin), WatchEngine, ThrottleCoordinator
  watch/    WatcherService, UsageEventsReader, Notifier, receivers
throttle/   Android library, no UI: RelayEngine, TcpSession, TokenBucket, SlowDownVpnService
.kiro/      specs, steering, hooks (written after the build)
docs/       measured reports, screenshots
```
`:app` depends on `:throttle`. Only `ThrottleCoordinator` calls the engine.

## Measured speed
Tested on an emulator (Android 15, API 35) and on a real Moto g67 power 5G (Android 16, mobile data LTE, Private DNS `dns.adguard.com`). Timed HTTPS download per run. Sources: [`docs/REPORT-e2e.md`](docs/REPORT-e2e.md) (emulator, 12 s runs), [`docs/REPORT-device.md`](docs/REPORT-device.md) (phone).

| Limit (kbps) | Measured (kbps) | Error |
|---|---|---|
| none (baseline) | 29833 | |
| 128 | 127.0 | -0.8% |
| 384 | 387.4 and 388.1 | +0.9% / +1.1% |
| 1024 | 1012.7 | -1.1% |

Real phone (Moto g67 power 5G, mobile data), whole device, manual slow-down. Source: [`docs/REPORT-device.md`](docs/REPORT-device.md).

| Limit (kbps) | Measured (kbps) | Error |
|---|---|---|
| none (baseline) | 22483 | |
| 384 | 387.0 and 383.6 | +0.8% / -0.1% |
| after Stop | 18107 | mobile data varies; VPN was gone |

No crash in `logcat -b crash` during the phone run.

A real doom-scroll run with YouTube (threshold 5 min, release 1 min): the throttle started by itself after about 5 min, covered only YouTube's uid, and ended 1 to 2 min after leaving the app. 42 JVM tests pass (22 engine, 20 app).

## Build and run
Needs JDK 17+ and the Android SDK (compileSdk 36). Set `sdk.dir` in `local.properties` (Android Studio does this).

```
./gradlew assembleDebug
./gradlew :app:installDebug          # device or emulator connected
./gradlew testDebugUnitTest          # 42 tests
```

On first run the onboarding asks for three permissions:
1. **VPN consent** (the system dialog).
2. **Notifications** (Android 13+).
3. **Usage access** (Settings page). Without it only manual slow-down works.

## Limitations
- Tested on one emulator (API 35) and one phone (Moto g67 power 5G, Android 16, mobile data). Not checked on the phone: the automatic doom-scroll trigger, the notification Stop button, the boot receiver, the release cooldown, battery kill by Moto, and Wi-Fi.
- Moto tip: set the app's battery setting to Unrestricted. Moto battery management was not tested and may stop the watcher.
- IPv6 is blocked for covered apps, so an IPv6-only network will not work.
- No DNS servers are set on the VPN. Private DNS with a hostname (`dns.adguard.com`) worked on the Moto.
- Changing "What to slow" in Settings does not affect a throttle already running.
- Only one TCP stream was measured. QUIC, parallel streams and extra latency were not tested.
- YouTube's own speed under the limit was not measured. Per-app proof is the VPN uid list.
- The release APK is signed with a self-generated key (not a Play Store upload key) and R8 is on. Play Store needs a "special use" foreground service declaration.

## Built with Kiro
Built with Kiro Crew in conductor mode: one goal, work items, a worker session per item, and independent verifier workers. The UI was designed in Google Stitch and imported by hand as a zip. The specs, steering and hooks in [`.kiro/`](.kiro) were written after the build. Details: [`docs/KIRO_WRITEUP.md`](docs/KIRO_WRITEUP.md).

## License
[MIT](LICENSE)
